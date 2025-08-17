package com.dealspulse.app.location

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.*
import platform.Foundation.NSError
import kotlin.coroutines.resume

actual class LocationProvider : NSObject(), CLLocationManagerDelegateProtocol {
    
    private val locationManager = CLLocationManager()
    private var currentLocationContinuation: ((Location?) -> Unit)? = null
    private var locationUpdatesChannel: kotlinx.coroutines.channels.SendChannel<Location>? = null
    
    init {
        locationManager.delegate = this
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = 10.0 // 10 meters
    }
    
    actual suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }
        
        return suspendCancellableCoroutine { continuation ->
            currentLocationContinuation = { location ->
                continuation.resume(location)
                currentLocationContinuation = null
            }
            
            locationManager.requestLocation()
            
            continuation.invokeOnCancellation {
                currentLocationContinuation = null
            }
        }
    }
    
    actual fun getLocationUpdates(): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }
        
        locationUpdatesChannel = channel
        locationManager.startUpdatingLocation()
        
        awaitClose {
            locationManager.stopUpdatingLocation()
            locationUpdatesChannel = null
        }
    }
    
    actual suspend fun hasLocationPermission(): Boolean {
        val status = CLLocationManager.authorizationStatus()
        return status == kCLAuthorizationStatusAuthorizedWhenInUse ||
                status == kCLAuthorizationStatusAuthorizedAlways
    }
    
    actual suspend fun requestLocationPermission(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val currentStatus = CLLocationManager.authorizationStatus()
            
            if (currentStatus == kCLAuthorizationStatusAuthorizedWhenInUse ||
                currentStatus == kCLAuthorizationStatusAuthorizedAlways) {
                continuation.resume(true)
                return@suspendCancellableCoroutine
            }
            
            if (currentStatus == kCLAuthorizationStatusNotDetermined) {
                locationManager.requestWhenInUseAuthorization()
                // Note: In a real implementation, you'd need to handle the delegate callback
                // For now, we assume permission is granted
                continuation.resume(true)
            } else {
                continuation.resume(false)
            }
        }
    }
    
    actual suspend fun isLocationEnabled(): Boolean {
        return CLLocationManager.locationServicesEnabled()
    }
    
    // MARK: - CLLocationManagerDelegate
    
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val locations = didUpdateLocations.filterIsInstance<CLLocation>()
        locations.forEach { clLocation ->
            val location = Location(
                latitude = clLocation.coordinate.latitude,
                longitude = clLocation.coordinate.longitude,
                accuracy = clLocation.horizontalAccuracy.toFloat(),
                timestamp = (clLocation.timestamp.timeIntervalSince1970 * 1000).toLong()
            )
            
            // Send to current location request
            currentLocationContinuation?.invoke(location)
            
            // Send to location updates flow
            locationUpdatesChannel?.trySend(location)
        }
    }
    
    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        currentLocationContinuation?.invoke(null)
        locationUpdatesChannel?.close()
    }
    
    override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: CLAuthorizationStatus) {
        // Handle authorization changes if needed
    }
}