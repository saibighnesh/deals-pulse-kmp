package com.dealspulse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dealspulse.app.location.LocationProvider

class MainActivity : ComponentActivity() {
    
    private lateinit var locationProvider: LocationProvider
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        locationProvider = LocationProvider(this)
        
        setContent {
            App(locationProvider)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    // Preview without location provider for design purposes
}