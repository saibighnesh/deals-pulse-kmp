package com.dealspulse.app.presentation.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pull.PullRefreshIndicator
import androidx.compose.foundation.pull.pullRefresh
import androidx.compose.foundation.pull.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealspulse.app.model.*
import com.dealspulse.app.presentation.components.DealCard
import com.dealspulse.app.presentation.components.CategoryFilter
import com.dealspulse.app.presentation.components.RadiusSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onDealClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val deals by viewModel.deals.collectAsState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isLoading,
        onRefresh = { viewModel.refresh() }
    )
    
    LaunchedEffect(Unit) {
        if (!uiState.hasLocationPermission) {
            viewModel.requestLocationPermission()
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header with radius selector
        FeedHeader(
            selectedRadius = uiState.selectedRadius,
            onRadiusChange = { viewModel.setRadius(it) },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Category filter
        CategoryFilter(
            selectedCategory = uiState.selectedCategory,
            onCategorySelect = { viewModel.setCategory(it) },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            when {
                uiState.isLoading && deals.isEmpty() -> {
                    LoadingState()
                }
                uiState.error != null && deals.isEmpty() -> {
                    ErrorState(
                        error = uiState.error!!,
                        onRetry = { viewModel.refresh() }
                    )
                }
                uiState.isEmpty -> {
                    EmptyState(
                        radius = uiState.selectedRadius,
                        onRetry = { viewModel.refresh() }
                    )
                }
                else -> {
                    DealsList(
                        deals = deals,
                        onDealClick = onDealClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            PullRefreshIndicator(
                refreshing = uiState.isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedHeader(
    selectedRadius: Int,
    onRadiusChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Deals Near You",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            RadiusSelector(
                selectedRadius = selectedRadius,
                onRadiusChange = onRadiusChange
            )
        }
    }
}

@Composable
private fun DealsList(
    deals: List<DealWithVendor>,
    onDealClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = deals,
            key = { it.deal.id }
        ) { dealWithVendor ->
            DealCard(
                dealWithVendor = dealWithVendor,
                onClick = { onDealClick(dealWithVendor.deal.id) }
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun EmptyState(
    radius: Int,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "No deals nearby",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Try widening your radius or check back later for new offers",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(onClick = onRetry) {
                Text("Refresh")
            }
        }
    }
}