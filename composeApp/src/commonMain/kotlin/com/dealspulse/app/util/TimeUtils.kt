package com.dealspulse.app.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

object TimeUtils {
    
    /**
     * Format time remaining until expiration
     */
    fun formatTimeRemaining(expiresAt: Instant): String {
        val now = Clock.System.now()
        val remaining = expiresAt - now
        
        if (remaining.isNegative()) {
            return "Expired"
        }
        
        val totalSeconds = remaining.inWholeSeconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
    
    /**
     * Get urgency level based on time remaining
     */
    fun getUrgencyLevel(expiresAt: Instant): UrgencyLevel {
        val now = Clock.System.now()
        val remaining = expiresAt - now
        
        if (remaining.isNegative()) {
            return UrgencyLevel.EXPIRED
        }
        
        val remainingMinutes = remaining.inWholeMinutes
        
        return when {
            remainingMinutes <= 5 -> UrgencyLevel.CRITICAL
            remainingMinutes <= 30 -> UrgencyLevel.HIGH
            remainingMinutes <= 120 -> UrgencyLevel.MEDIUM
            else -> UrgencyLevel.LOW
        }
    }
    
    /**
     * Format relative time (e.g., "2 hours ago", "just now")
     */
    fun formatRelativeTime(instant: Instant): String {
        val now = Clock.System.now()
        val duration = now - instant
        
        val totalMinutes = duration.inWholeMinutes
        val totalHours = duration.inWholeHours
        val totalDays = duration.inWholeDays
        
        return when {
            totalMinutes < 1 -> "Just now"
            totalMinutes < 60 -> "${totalMinutes}m ago"
            totalHours < 24 -> "${totalHours}h ago"
            totalDays < 7 -> "${totalDays}d ago"
            else -> "${totalDays / 7}w ago"
        }
    }
    
    /**
     * Check if a deal is expiring soon (within next 30 minutes)
     */
    fun isExpiringSoon(expiresAt: Instant): Boolean {
        val now = Clock.System.now()
        val remaining = expiresAt - now
        return remaining.inWholeMinutes <= 30 && !remaining.isNegative()
    }
}

enum class UrgencyLevel {
    LOW,        // > 2 hours
    MEDIUM,     // 30min - 2 hours
    HIGH,       // 5-30 minutes
    CRITICAL,   // < 5 minutes
    EXPIRED
}