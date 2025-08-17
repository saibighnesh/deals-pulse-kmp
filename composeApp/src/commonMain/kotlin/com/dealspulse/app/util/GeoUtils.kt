package com.dealspulse.app.util

import kotlin.math.*

object GeoUtils {
    
    /**
     * Calculate distance between two points using Haversine formula
     * @return distance in miles
     */
    fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 3959.0 // Earth's radius in miles
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return r * c
    }
    
    /**
     * Generate a geohash for a given latitude and longitude
     * This is a simplified implementation - in production you might want to use a library
     */
    fun generateGeohash(lat: Double, lng: Double, precision: Int = 6): String {
        val base32 = "0123456789bcdefghjkmnpqrstuvwxyz"
        var latMin = -90.0
        var latMax = 90.0
        var lngMin = -180.0
        var lngMax = 180.0
        
        var geohash = ""
        var bit = 0
        var ch = 0
        
        while (geohash.length < precision) {
            if (bit % 2 == 0) {
                val lngMid = (lngMin + lngMax) / 2
                if (lng >= lngMid) {
                    ch = ch or (1 shl (4 - bit % 5))
                    lngMin = lngMid
                } else {
                    lngMax = lngMid
                }
            } else {
                val latMid = (latMin + latMax) / 2
                if (lat >= latMid) {
                    ch = ch or (1 shl (4 - bit % 5))
                    latMin = latMid
                } else {
                    latMax = latMid
                }
            }
            
            bit++
            if (bit % 5 == 0) {
                geohash += base32[ch]
                ch = 0
            }
        }
        
        return geohash
    }
    
    /**
     * Get bounding box for a geohash (simplified)
     */
    fun getGeohashBoundingBox(geohash: String): Pair<Pair<Double, Double>, Pair<Double, Double>> {
        // This is a simplified implementation
        // In production, you'd want to decode the geohash properly
        val lat = 0.0 // Decode from geohash
        val lng = 0.0 // Decode from geohash
        val precision = geohash.length
        
        val latDelta = 180.0 / (2.0.pow(precision / 2.0))
        val lngDelta = 360.0 / (2.0.pow(precision / 2.0))
        
        return Pair(
            Pair(lat - latDelta, lat + latDelta),
            Pair(lng - lngDelta, lng + lngDelta)
        )
    }
    
    /**
     * Check if a location is within a given radius of another location
     */
    fun isWithinRadius(
        centerLat: Double,
        centerLng: Double,
        targetLat: Double,
        targetLng: Double,
        radiusMiles: Double
    ): Boolean {
        return calculateDistance(centerLat, centerLng, targetLat, targetLng) <= radiusMiles
    }
}