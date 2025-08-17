package com.dealspulse.app.presentation.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dealspulse.app.model.*
import com.dealspulse.app.util.GeoUtils
import com.dealspulse.app.util.TimeUtils
import com.dealspulse.app.util.UrgencyLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onDealClick: (String) -> Unit,
    onVendorClick: (String) -> Unit
) {
    val uiState by viewModel.uiState
    val selectedCategory by viewModel.selectedCategory
    val selectedRadius by viewModel.selectedRadius
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with location and radius picker
        FeedHeader(
            selectedRadius = selectedRadius,
            onRadiusChange = { viewModel.updateRadius(it) }
        )
        
        // Category filters
        CategoryFilters(
            selectedCategory = selectedCategory,
            onCategorySelect = { viewModel.updateCategory(it) }
        )
        
        // Deals list
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                ErrorState(
                    error = uiState.error!!,
                    onRetry = { viewModel.refreshDeals() },
                    onDismiss = { viewModel.clearError() }
                )
            }
            uiState.deals.isEmpty() -> {
                EmptyState(
                    selectedRadius = selectedRadius,
                    selectedCategory = selectedCategory
                )
            }
            else -> {
                DealsList(
                    deals = uiState.deals,
                    onDealClick = onDealClick,
                    onVendorClick = onVendorClick,
                    onRefresh = { viewModel.refreshDeals() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedHeader(
    selectedRadius: Int,
    onRadiusChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Deals Near You",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "San Francisco, CA", // TODO: Get actual city
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Search Radius",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val radiusOptions = listOf(5, 10, 20, 50)
                items(radiusOptions) { radius ->
                    FilterChip(
                        selected = selectedRadius == radius,
                        onClick = { onRadiusChange(radius) },
                        label = { Text("${radius} mi") }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilters(
    selectedCategory: DealCategory?,
    onCategorySelect: (DealCategory?) -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All categories option
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelect(null) },
                label = { Text("All") }
            )
        }
        
        // Individual categories
        items(DealCategory.values()) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelect(category) },
                label = { Text(category.name.lowercase().capitalize()) }
            )
        }
    }
}

@Composable
private fun DealsList(
    deals: List<DealWithVendor>,
    onDealClick: (String) -> Unit,
    onVendorClick: (String) -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(deals) { dealWithVendor ->
            DealCard(
                dealWithVendor = dealWithVendor,
                onDealClick = onDealClick,
                onVendorClick = onVendorClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DealCard(
    dealWithVendor: DealWithVendor,
    onDealClick: (String) -> Unit,
    onVendorClick: (String) -> Unit
) {
    val deal = dealWithVendor.deal
    val vendor = dealWithVendor.vendor
    val timeRemaining = TimeUtils.getTimeRemaining(deal.expiresAt)
    val urgencyLevel = TimeUtils.getUrgencyLevel(deal.expiresAt)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDealClick(deal.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Deal image
            deal.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = deal.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
            
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header with title and urgency badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = deal.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    UrgencyBadge(urgencyLevel)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description
                Text(
                    text = deal.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Price and vendor info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$${deal.price}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        deal.originalPrice?.let { originalPrice ->
                            Text(
                                text = "$${originalPrice}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.strikeThrough()
                            )
                        }
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = vendor.businessName ?: "Unknown Vendor",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onVendorClick(vendor.userId) }
                        )
                        
                        // Distance (if we had current location)
                        Text(
                            text = "0.5 mi away", // TODO: Calculate actual distance
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Time remaining
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Time remaining",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = TimeUtils.formatTimeRemaining(timeRemaining),
                        style = MaterialTheme.typography.bodySmall,
                        color = when (urgencyLevel) {
                            UrgencyLevel.CRITICAL -> Color.Red
                            UrgencyLevel.HIGH -> Color(0xFFFF6B35)
                            UrgencyLevel.MEDIUM -> Color(0xFFFFB74D)
                            UrgencyLevel.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                            UrgencyLevel.EXPIRED -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun UrgencyBadge(urgencyLevel: UrgencyLevel) {
    val (backgroundColor, textColor) = when (urgencyLevel) {
        UrgencyLevel.CRITICAL -> MaterialTheme.colorScheme.error to Color.White
        UrgencyLevel.HIGH -> Color(0xFFFF6B35) to Color.White
        UrgencyLevel.MEDIUM -> Color(0xFFFFB74D) to Color.Black
        UrgencyLevel.LOW -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        UrgencyLevel.EXPIRED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = when (urgencyLevel) {
                UrgencyLevel.CRITICAL -> "URGENT"
                UrgencyLevel.HIGH -> "SOON"
                UrgencyLevel.MEDIUM -> "ENDING"
                UrgencyLevel.LOW -> "ACTIVE"
                UrgencyLevel.EXPIRED -> "EXPIRED"
            },
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyState(
    selectedRadius: Int,
    selectedCategory: DealCategory?
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = "No deals found",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No deals nearby",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Try widening your radius or changing categories",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Retry")
            }
        }
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}