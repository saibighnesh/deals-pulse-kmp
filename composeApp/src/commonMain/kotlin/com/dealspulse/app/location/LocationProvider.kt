package com.dealspulse.app.location

import com.dealspulse.app.model.Location
import kotlinx.coroutines.flow.Flow

expect class LocationProvider {
    /**
     * Get current location
     */
    suspend fun getCurrentLocation(): Location?
    
    /**
     * Get location updates as a flow
     */
    fun getLocationUpdates(): Flow<Location>
    
    /**
     * Check if location permissions are granted
     */
    suspend fun hasLocationPermission(): Boolean
    
    /**
     * Request location permissions
     */
    suspend fun requestLocationPermission(): Boolean
    
    /**
     * Check if location services are enabled
     */
    suspend fun isLocationEnabled(): Boolean
}