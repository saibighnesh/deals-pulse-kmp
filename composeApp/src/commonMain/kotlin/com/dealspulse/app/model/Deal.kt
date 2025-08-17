package com.dealspulse.app.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Deal(
    val id: String,
    
    @SerialName("vendor_id")
    val vendorId: String,
    
    val title: String,
    
    val description: String,
    
    val category: String,
    
    val price: String,
    
    @SerialName("image_url")
    val imageUrl: String? = null,
    
    val lat: Double,
    
    val lng: Double,
    
    val geohash: String,
    
    val status: DealStatus,
    
    @SerialName("created_at")
    val createdAt: Instant,
    
    @SerialName("expires_at")
    val expiresAt: Instant,
    
    @SerialName("is_promoted")
    val isPromoted: Boolean = false,
    
    // Joined fields from vendor profile
    @SerialName("vendor_business_name")
    val vendorBusinessName: String? = null,
    
    @SerialName("vendor_phone")
    val vendorPhone: String? = null,
    
    @SerialName("vendor_address")
    val vendorAddress: String? = null,
    
    @SerialName("vendor_is_verified")
    val vendorIsVerified: Boolean = false
) {
    val isExpired: Boolean
        get() = kotlinx.datetime.Clock.System.now() > expiresAt
        
    val isActive: Boolean
        get() = status == DealStatus.ACTIVE && !isExpired
}

@Serializable
enum class DealStatus {
    @SerialName("pending")
    PENDING,
    
    @SerialName("active")
    ACTIVE,
    
    @SerialName("rejected")
    REJECTED,
    
    @SerialName("ended")
    ENDED
}

@Serializable
enum class DealCategory(val displayName: String) {
    @SerialName("food")
    FOOD("Food & Drinks"),
    
    @SerialName("salon")
    SALON("Beauty & Salon"),
    
    @SerialName("fitness")
    FITNESS("Fitness & Wellness"),
    
    @SerialName("retail")
    RETAIL("Retail & Shopping"),
    
    @SerialName("services")
    SERVICES("Services"),
    
    @SerialName("entertainment")
    ENTERTAINMENT("Entertainment"),
    
    @SerialName("other")
    OTHER("Other");
    
    companion object {
        fun fromString(value: String): DealCategory {
            return entries.find { it.name.lowercase() == value.lowercase() } ?: OTHER
        }
    }
}