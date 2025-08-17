package com.dealspulse.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class Deal(
    val id: String,
    @SerialName("vendor_id") val vendorId: String,
    val title: String,
    val description: String,
    val category: String,
    val price: Double,
    @SerialName("image_url") val imageUrl: String? = null,
    val lat: Double,
    val lng: Double,
    val geohash: String? = null,
    val status: DealStatus = DealStatus.ACTIVE,
    @SerialName("created_at") val createdAt: Instant? = null,
    @SerialName("expires_at") val expiresAt: Instant,
    @SerialName("is_promoted") val isPromoted: Boolean = false
)

@Serializable
enum class DealStatus {
    @SerialName("pending") PENDING,
    @SerialName("active") ACTIVE,
    @SerialName("rejected") REJECTED,
    @SerialName("ended") ENDED
}