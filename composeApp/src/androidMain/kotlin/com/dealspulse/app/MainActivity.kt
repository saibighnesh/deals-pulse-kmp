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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dealspulse.app.location.LocationProvider
import com.dealspulse.app.presentation.feed.FeedScreen
import com.dealspulse.app.presentation.feed.FeedViewModel
import com.dealspulse.app.ui.theme.DealsPulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DealsPulseTheme {
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
    val locationProvider = LocationProvider(LocalContext.current)
    val feedViewModel: FeedViewModel = viewModel {
        FeedViewModel(locationProvider = locationProvider)
    }
    
    FeedScreen(
        viewModel = feedViewModel,
        onDealClick = { dealId ->
            // Navigate to deal detail screen
            // This would be implemented with navigation
        }
    )
}