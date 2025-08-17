package com.dealspulse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.dealspulse.app.config.AppConfig
import com.dealspulse.app.data.*
import com.dealspulse.app.location.LocationProvider
import com.dealspulse.app.presentation.feed.FeedScreen
import com.dealspulse.app.presentation.feed.FeedViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DealsPulseApp()
                }
            }
        }
    }
}

@Composable
fun DealsPulseApp() {
    // Initialize dependencies
    val locationProvider = LocationProvider(LocalContext.current)
    val dealApi = DealApi()
    val realtimeService = RealtimeService()
    
    // Create ViewModel
    val feedViewModel = FeedViewModel(
        dealApi = dealApi,
        realtimeService = realtimeService,
        locationProvider = locationProvider
    )
    
    // Show Feed Screen
    FeedScreen(
        viewModel = feedViewModel,
        onDealClick = { dealId ->
            // TODO: Navigate to deal detail
        },
        onVendorClick = { vendorId ->
            // TODO: Navigate to vendor profile
        }
    )
}