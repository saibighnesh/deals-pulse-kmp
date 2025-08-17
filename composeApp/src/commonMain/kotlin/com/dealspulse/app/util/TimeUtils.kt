package com.dealspulse.app.util

import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object TimeUtils {
    
    /**
     * Get time remaining until expiration
     */
    fun getTimeRemaining(expiresAt: Instant): TimeRemaining {
        val now = Clock.System.now()
        val duration = expiresAt - now
        
        if (duration.isNegative()) {
            return TimeRemaining(0, 0, 0, 0, true)
        }
        
        val totalSeconds = duration.inWholeSeconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val days = hours / 24
        
        return TimeRemaining(
            days = days.toInt(),
            hours = (hours % 24).toInt(),
            minutes = minutes.toInt(),
            seconds = seconds.toInt(),
            isExpired = false
        )
    }
    
    /**
     * Format time remaining as a human-readable string
     */
    fun formatTimeRemaining(timeRemaining: TimeRemaining): String {
        return when {
            timeRemaining.isExpired -> "Expired"
            timeRemaining.days > 0 -> "${timeRemaining.days}d ${timeRemaining.hours}h"
            timeRemaining.hours > 0 -> "${timeRemaining.hours}h ${timeRemaining.minutes}m"
            timeRemaining.minutes > 0 -> "${timeRemaining.minutes}m ${timeRemaining.seconds}s"
            else -> "${timeRemaining.seconds}s"
        }
    }
    
    /**
     * Format expiration time as a readable string
     */
    fun formatExpirationTime(expiresAt: Instant): String {
        val localDateTime = expiresAt.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${localDateTime.monthNumber}/${localDateTime.dayOfMonth} at ${localDateTime.hour}:${localDateTime.minute.toString().padStart(2, '0')}"
    }
    
    /**
     * Check if a deal is expired
     */
    fun isExpired(expiresAt: Instant): Boolean {
        return Clock.System.now() >= expiresAt
    }
    
    /**
     * Get urgency level based on time remaining
     */
    fun getUrgencyLevel(expiresAt: Instant): UrgencyLevel {
        val timeRemaining = getTimeRemaining(expiresAt)
        if (timeRemaining.isExpired) return UrgencyLevel.EXPIRED
        
        return when {
            timeRemaining.hours < 1 -> UrgencyLevel.CRITICAL
            timeRemaining.hours < 3 -> UrgencyLevel.HIGH
            timeRemaining.hours < 12 -> UrgencyLevel.MEDIUM
            else -> UrgencyLevel.LOW
        }
    }
}

data class TimeRemaining(
    val days: Int,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val isExpired: Boolean
)

enum class UrgencyLevel {
    LOW, MEDIUM, HIGH, CRITICAL, EXPIRED
}