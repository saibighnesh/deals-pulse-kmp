package com.dealspulse.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealspulse.app.model.DealWithVendor
import com.dealspulse.app.util.GeoUtils
import com.dealspulse.app.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealCard(
    dealWithVendor: DealWithVendor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deal = dealWithVendor.deal
    val vendor = dealWithVendor.vendor
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with image and urgency badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Deal image
                DealImage(
                    imageUrl = deal.imageUrl,
                    modifier = Modifier
                        .size(80.dp)
                        .weight(0.3f)
                )
                
                // Deal info
                Column(
                    modifier = Modifier.weight(0.7f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = deal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = vendor.businessName ?: "Unknown Vendor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Price and discount
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$${deal.price}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        deal.originalPrice?.let { originalPrice ->
                            if (originalPrice > deal.price) {
                                Text(
                                    text = "$${originalPrice}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.strikeThrough()
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Description
            Text(
                text = deal.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Footer with distance, time remaining, and category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distance and category
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Distance
                    vendor.lat?.let { lat ->
                        vendor.lng?.let { lng ->
                            // This would need to be calculated based on user's current location
                            // For now, we'll show a placeholder
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Location",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Nearby",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Category
                    CategoryChip(category = deal.category)
                }
                
                // Time remaining
                TimeRemainingBadge(expiresAt = deal.expiresAt)
            }
        }
    }
}

@Composable
private fun DealImage(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    if (imageUrl != null) {
        // In a real app, you'd use Coil or another image loading library
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📷",
                fontSize = 24.sp
            )
        }
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "No image",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: com.dealspulse.app.model.DealCategory,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (category) {
        com.dealspulse.app.model.DealCategory.FOOD -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        com.dealspulse.app.model.DealCategory.SALON -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        com.dealspulse.app.model.DealCategory.FITNESS -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        com.dealspulse.app.model.DealCategory.RETAIL -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        com.dealspulse.app.model.DealCategory.ENTERTAINMENT -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        com.dealspulse.app.model.DealCategory.SERVICES -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        com.dealspulse.app.model.DealCategory.OTHER -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = category.name.lowercase().capitalize(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TimeRemainingBadge(
    expiresAt: kotlinx.datetime.Instant,
    modifier: Modifier = Modifier
) {
    val timeRemaining = TimeUtils.getTimeRemaining(expiresAt)
    val urgencyLevel = TimeUtils.getUrgencyLevel(expiresAt)
    
    val backgroundColor = when (urgencyLevel) {
        TimeUtils.UrgencyLevel.CRITICAL -> Color(0xFFD32F2F) // Red
        TimeUtils.UrgencyLevel.HIGH -> Color(0xFFFF9800) // Orange
        TimeUtils.UrgencyLevel.MEDIUM -> Color(0xFFFFEB3B) // Yellow
        TimeUtils.UrgencyLevel.LOW -> Color(0xFF4CAF50) // Green
        TimeUtils.UrgencyLevel.EXPIRED -> Color(0xFF9E9E9E) // Gray
    }
    
    val textColor = if (urgencyLevel == TimeUtils.UrgencyLevel.MEDIUM) {
        Color.Black
    } else {
        Color.White
    }
    
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = TimeUtils.formatTimeRemaining(timeRemaining),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun String.capitalize(): String {
    return if (isNotEmpty()) {
        this[0].uppercase() + substring(1)
    } else {
        this
    }
}