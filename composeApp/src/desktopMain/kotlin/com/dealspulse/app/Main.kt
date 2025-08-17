package com.dealspulse.app

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.dealspulse.app.presentation.feed.FeedScreen
import com.dealspulse.app.presentation.feed.FeedViewModel

fun main() = application {
	Window(onCloseRequest = ::exitApplication, title = "DealsPulse Debug") {
		MaterialTheme {
			val vm = FeedViewModel()
			// Sample location: downtown San Francisco
			vm.setLocation(37.7749, -122.4194)
			FeedScreen(viewModel = vm, onDealClick = {})
		}
	}
}