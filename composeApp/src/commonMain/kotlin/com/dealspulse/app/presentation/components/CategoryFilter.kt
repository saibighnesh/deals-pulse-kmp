package com.dealspulse.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dealspulse.app.model.DealCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilter(
    selectedCategory: DealCategory?,
    onCategorySelect: (DealCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        DealCategory.FOOD,
        DealCategory.SALON,
        DealCategory.FITNESS,
        DealCategory.RETAIL,
        DealCategory.ENTERTAINMENT,
        DealCategory.SERVICES,
        DealCategory.OTHER
    )
    
    LazyRow(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" option
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelect(null) },
                label = { Text("All") }
            )
        }
        
        // Category options
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelect(category) },
                label = { Text(category.name.lowercase().capitalize()) }
            )
        }
    }
}

private fun String.capitalize(): String {
    return if (isNotEmpty()) {
        this[0].uppercase() + substring(1)
    } else {
        this
    }
}