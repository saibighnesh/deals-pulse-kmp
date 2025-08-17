package com.dealspulse.app.util

import kotlin.math.*

object GeoUtil {
	fun haversineMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
		val R = 3958.7613 // Earth radius in miles
		val dLat = Math.toRadians(lat2 - lat1)
		val dLon = Math.toRadians(lon2 - lon1)
		val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
		val c = 2 * atan2(sqrt(a), sqrt(1 - a))
		return R * c
	}
}

object GeoHashUtil {
	private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

	fun encode(lat: Double, lon: Double, precision: Int = 7): String {
		var latMin = -90.0
		var latMax = 90.0
		var lonMin = -180.0
		var lonMax = 180.0
		var isLon = true
		var bit = 0
		var ch = 0
		val hash = StringBuilder()
		while (hash.length < precision) {
			if (isLon) {
				val mid = (lonMin + lonMax) / 2
				if (lon > mid) { ch = ch shl 1 or 1; lonMin = mid } else { ch = ch shl 1; lonMax = mid }
			} else {
				val mid = (latMin + latMax) / 2
				if (lat > mid) { ch = ch shl 1 or 1; latMin = mid } else { ch = ch shl 1; latMax = mid }
			}
			isLon = !isLon
			bit++
			if (bit == 5) {
				hash.append(BASE32[ch])
				bit = 0
				ch = 0
			}
		}
		return hash.toString()
	}
}