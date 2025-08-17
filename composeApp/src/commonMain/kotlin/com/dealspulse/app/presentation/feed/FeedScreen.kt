package com.dealspulse.app.presentation.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dealspulse.app.model.Deal

@Composable
fun FeedScreen(viewModel: FeedViewModel, onDealClick: (Deal) -> Unit) {
	val state by viewModel.state.collectAsState()

	Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
		Header(state, onRadiusChange = { viewModel.setRadius(it) })
		Spacer(Modifier.height(8.dp))
		when {
			state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
			state.errorMessage != null -> Text("Error: ${state.errorMessage}")
			state.visibleDeals.isEmpty() -> Text("No deals nearby—widen your radius.")
			else -> LazyColumn(Modifier.fillMaxSize()) {
				items(state.visibleDeals) { deal -> DealRow(deal) { onDealClick(deal) } }
			}
		}
	}
}

@Composable
private fun Header(state: FeedState, onRadiusChange: (Int) -> Unit) {
	Row(verticalAlignment = Alignment.CenterVertically) {
		Text("Radius:")
		Spacer(Modifier.width(8.dp))
		var expanded by remember { mutableStateOf(false) }
		Box {
			Button(onClick = { expanded = true }) { Text("${state.radiusMiles} mi") }
			DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
				listOf(5, 10, 20, 50).forEach { miles ->
					DropdownMenuItem(onClick = { expanded = false; onRadiusChange(miles) }) { Text("$miles mi") }
				}
			}
		}
	}
}

@Composable
private fun DealRow(deal: Deal, onClick: () -> Unit) {
	Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), elevation = 2.dp) {
		Column(Modifier.padding(12.dp)) {
			Text(deal.title, style = MaterialTheme.typography.h6)
			deal.vendorProfile?.businessName?.let { Text(it, style = MaterialTheme.typography.body2) }
			Text("Expires: ${deal.expiresAt}", style = MaterialTheme.typography.caption)
			deal.price?.let { Text("$" + String.format("%.2f", it)) }
			Button(onClick = onClick, modifier = Modifier.padding(top = 8.dp)) { Text("View") }
		}
	}
}