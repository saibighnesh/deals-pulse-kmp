package com.dealspulse.app.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class Profile(
    val userId: String,
    val accountType: AccountType,
    val businessName: String? = null,
    val logoUrl: String? = null,
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val phone: String? = null,
    val isVerified: Boolean = false,
    val radiusMiles: Int = 10
)

@Serializable
enum class AccountType {
    USER,
    VENDOR
}

@Serializable
data class Deal(
    val id: String,
    val vendorId: String,
    val title: String,
    val description: String,
    val category: DealCategory,
    val price: Double,
    val originalPrice: Double? = null,
    val imageUrl: String? = null,
    val lat: Double,
    val lng: Double,
    val geohash: String,
    val status: DealStatus,
    val createdAt: Instant,
    val expiresAt: Instant,
    val isPromoted: Boolean = false
)

@Serializable
enum class DealCategory {
    FOOD,
    SALON,
    FITNESS,
    RETAIL,
    ENTERTAINMENT,
    SERVICES,
    OTHER
}

@Serializable
enum class DealStatus {
    PENDING,
    ACTIVE,
    REJECTED,
    ENDED
}

@Serializable
data class Report(
    val id: String,
    val postId: String,
    val reporterId: String,
    val reason: String,
    val createdAt: Instant
)

@Serializable
data class DealWithVendor(
    val deal: Deal,
    val vendor: Profile
)

@Serializable
data class Location(
    val lat: Double,
    val lng: Double
)