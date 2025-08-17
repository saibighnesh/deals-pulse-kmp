package com.dealspulse.app.location

import kotlinx.coroutines.flow.Flow

data class Location(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

expect class LocationProvider {
    
    /**
     * Get current location once
     */
    suspend fun getCurrentLocation(): Location?
    
    /**
     * Start location updates flow
     */
    fun getLocationUpdates(): Flow<Location>
    
    /**
     * Check if location permission is granted
     */
    suspend fun hasLocationPermission(): Boolean
    
    /**
     * Request location permission
     */
    suspend fun requestLocationPermission(): Boolean
    
    /**
     * Check if location services are enabled
     */
    suspend fun isLocationEnabled(): Boolean
}