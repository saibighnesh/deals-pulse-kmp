package com.dealspulse.app.util

import ch.hsr.geohash.GeoHash
import kotlin.math.*

object GeoUtils {
    
    /**
     * Generate geohash for given coordinates with specified precision
     */
    fun generateGeoHash(lat: Double, lng: Double, precision: Int = 8): String {
        return GeoHash.withBitPrecision(lat, lng, precision * 5).toBase32()
    }
    
    /**
     * Calculate distance between two points using Haversine formula
     * Returns distance in miles
     */
    fun calculateDistance(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val earthRadiusMiles = 3959.0
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        
        val a = sin(dLat / 2).pow(2) + 
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * 
                sin(dLng / 2).pow(2)
        
        val c = 2 * asin(sqrt(a))
        
        return earthRadiusMiles * c
    }
    
    /**
     * Get geohash prefixes for radius-based queries
     * Returns list of geohash prefixes that cover the area within the given radius
     */
    fun getGeoHashPrefixes(lat: Double, lng: Double, radiusMiles: Double): List<String> {
        // Simplified approach: use different precision levels based on radius
        val precision = when {
            radiusMiles <= 1 -> 8
            radiusMiles <= 5 -> 6
            radiusMiles <= 20 -> 5
            radiusMiles <= 50 -> 4
            else -> 3
        }
        
        val centerHash = generateGeoHash(lat, lng, precision)
        val prefixes = mutableSetOf<String>()
        
        // Add center hash
        prefixes.add(centerHash)
        
        // Add neighboring hashes for better coverage
        try {
            val geoHash = GeoHash.fromGeohashString(centerHash)
            val neighbors = geoHash.adjacent
            neighbors.forEach { neighbor ->
                prefixes.add(neighbor.toBase32())
            }
        } catch (e: Exception) {
            // Fallback to just center hash if neighbor calculation fails
        }
        
        return prefixes.toList()
    }
    
    /**
     * Format distance for display
     */
    fun formatDistance(distanceMiles: Double): String {
        return when {
            distanceMiles < 0.1 -> "< 0.1 mi"
            distanceMiles < 1.0 -> String.format("%.1f mi", distanceMiles)
            else -> String.format("%.0f mi", distanceMiles)
        }
    }
}