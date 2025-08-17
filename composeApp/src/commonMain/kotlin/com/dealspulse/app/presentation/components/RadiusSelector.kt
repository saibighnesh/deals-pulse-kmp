package com.dealspulse.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadiusSelector(
    selectedRadius: Int,
    onRadiusChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val radiusOptions = listOf(5, 10, 20, 50)
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Radius:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        radiusOptions.forEach { radius ->
            FilterChip(
                selected = selectedRadius == radius,
                onClick = { onRadiusChange(radius) },
                label = { Text("${radius} mi") }
            )
        }
    }
}