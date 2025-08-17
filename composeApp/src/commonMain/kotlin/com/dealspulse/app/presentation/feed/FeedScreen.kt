package com.dealspulse.app.presentation.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.dealspulse.app.location.LocationProvider
import com.dealspulse.app.model.Deal
import com.dealspulse.app.model.DealCategory
import com.dealspulse.app.util.GeoUtils
import com.dealspulse.app.util.TimeUtils
import com.dealspulse.app.util.UrgencyLevel

class FeedScreen(
    private val locationProvider: LocationProvider
) : Screen {
    
    override val key: ScreenKey = uniqueScreenKey
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = remember { FeedScreenModel(locationProvider = locationProvider) }
        val uiState by screenModel.uiState.collectAsState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // Header
            FeedHeader(
                radiusMiles = uiState.radiusMiles,
                onRadiusChange = screenModel::updateRadiusMiles,
                onRefresh = screenModel::refreshDeals
            )
            
            // Category Filter
            CategoryFilter(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = screenModel::updateSelectedCategory
            )
            
            // Error handling
            uiState.error?.let { error ->
                ErrorBanner(
                    message = error,
                    onDismiss = screenModel::clearError
                )
            }
            
            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading && uiState.deals.isEmpty() -> {
                        LoadingIndicator()
                    }
                    uiState.deals.isEmpty() && !uiState.isLoading -> {
                        EmptyState(onRetry = screenModel::getCurrentLocation)
                    }
                    else -> {
                        DealsList(
                            deals = uiState.deals,
                            onDealClick = { deal ->
                                // Navigate to deal detail
                                // navigator.push(DealDetailScreen(deal.id))
                            }
                        )
                    }
                }
                
                // Loading indicator for refresh
                if (uiState.isLoading && uiState.deals.isNotEmpty()) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedHeader(
    radiusMiles: Int,
    onRadiusChange: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Deals Near You",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Radius selector
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Within $radiusMiles miles",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Radius chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf(5, 10, 20, 50)) { radius ->
                    RadiusChip(
                        radius = radius,
                        isSelected = radius == radiusMiles,
                        onClick = { onRadiusChange(radius) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RadiusChip(
    radius: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .clip(CircleShape),
        color = if (isSelected) MaterialTheme.colors.primary else Color.Transparent,
        contentColor = if (isSelected) Color.White else MaterialTheme.colors.onSurface
    ) {
        Text(
            text = "${radius}mi",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.caption,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun CategoryFilter(
    selectedCategory: DealCategory?,
    onCategorySelected: (DealCategory?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All categories chip
        item {
            CategoryChip(
                text = "All",
                isSelected = selectedCategory == null,
                onClick = { onCategorySelected(null) }
            )
        }
        
        items(DealCategory.entries.filter { it != DealCategory.OTHER }) { category ->
            CategoryChip(
                text = category.displayName,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(16.dp)),
        color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
        contentColor = if (isSelected) Color.White else MaterialTheme.colors.onSurface,
        elevation = if (isSelected) 4.dp else 1.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.body2,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        backgroundColor = MaterialTheme.colors.error,
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.body2
            )
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("DISMISS")
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Finding deals near you...",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun EmptyState(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No deals found nearby",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Try expanding your search radius or check back later",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun DealsList(
    deals: List<Pair<Deal, Double>>,
    onDealClick: (Deal) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(deals) { (deal, distance) ->
            DealCard(
                deal = deal,
                distance = distance,
                onClick = { onDealClick(deal) }
            )
        }
    }
}

@Composable
private fun DealCard(
    deal: Deal,
    distance: Double,
    onClick: () -> Unit
) {
    val urgency = TimeUtils.getUrgencyLevel(deal.expiresAt)
    val urgencyColor = when (urgency) {
        UrgencyLevel.CRITICAL -> Color.Red
        UrgencyLevel.HIGH -> Color(0xFFFF9800)
        UrgencyLevel.MEDIUM -> Color(0xFFFFC107)
        else -> Color.Gray
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deal.title,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = deal.vendorBusinessName ?: "Unknown Vendor",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                        if (deal.vendorIsVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                color = Color.Green,
                                shape = CircleShape,
                                modifier = Modifier.size(8.dp)
                            ) {}
                        }
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Surface(
                        color = urgencyColor,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = TimeUtils.formatTimeRemaining(deal.expiresAt),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = GeoUtils.formatDistance(distance),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            Text(
                text = deal.description,
                style = MaterialTheme.typography.body2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Price and category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = deal.price,
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Surface(
                    color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colors.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = DealCategory.fromString(deal.category).displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }
    }
}