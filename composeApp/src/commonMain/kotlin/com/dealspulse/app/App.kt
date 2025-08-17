package com.dealspulse.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import com.dealspulse.app.location.LocationProvider
import com.dealspulse.app.presentation.feed.FeedScreen

@Composable
fun App(locationProvider: LocationProvider) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            Navigator(FeedScreen(locationProvider))
        }
    }
}