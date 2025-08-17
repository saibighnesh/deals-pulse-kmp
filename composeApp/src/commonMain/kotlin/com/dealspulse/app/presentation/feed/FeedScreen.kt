package com.dealspulse.app.presentation.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dealspulse.app.model.Deal
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.roundToInt

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onDealClick: (Deal) -> Unit = {}
) {
    val deals by viewModel.deals.collectAsState()

    Scaffold(
        topBar = {
            SmallTopAppBar(title = { Text("Deals near you") })
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (deals.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No deals nearby—widen your radius.")
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(deals) { deal ->
                        DealListItem(deal, onDealClick)
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun DealListItem(deal: Deal, onDealClick: (Deal) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onDealClick(deal) }
            .padding(16.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(deal.title, style = MaterialTheme.typography.titleMedium)
            Text(deal.category, style = MaterialTheme.typography.bodySmall)
            Text("Expires in ${timeLeft(deal.expiresAt)}", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(8.dp))
        Text("$${deal.price.roundToInt()}", style = MaterialTheme.typography.titleMedium)
    }
}

private fun timeLeft(expiry: Instant): String {
    val now = Clock.System.now()
    val seconds = (expiry - now).inWholeSeconds
    return when {
        seconds <= 0 -> "Expired"
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m"
        else -> "${seconds / 3600}h"
    }
}