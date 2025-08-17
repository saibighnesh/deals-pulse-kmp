package com.dealspulse.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
	@SerialName("user_id") val userId: String,
	@SerialName("account_type") val accountType: AccountType = AccountType.User,
	@SerialName("business_name") val businessName: String? = null,
	@SerialName("logo_url") val logoUrl: String? = null,
	val address: String? = null,
	val lat: Double? = null,
	val lng: Double? = null,
	val phone: String? = null,
	@SerialName("is_verified") val isVerified: Boolean = false,
	@SerialName("radius_miles") val radiusMiles: Int? = null
)

@Serializable
enum class AccountType { User, Vendor }

@Serializable
data class Deal(
	val id: String,
	@SerialName("vendor_id") val vendorId: String,
	val title: String,
	val description: String,
	val category: String? = null,
	val price: Double? = null,
	@SerialName("image_url") val imageUrl: String? = null,
	val lat: Double,
	val lng: Double,
	val geohash: String? = null,
	val status: DealStatus = DealStatus.Active,
	@SerialName("created_at") val createdAt: String? = null,
	@SerialName("expires_at") val expiresAt: String,
	@SerialName("is_promoted") val isPromoted: Boolean = false,
	// Joined vendor profile info (optional in queries)
	@SerialName("vendor_profile") val vendorProfile: Profile? = null
)

@Serializable
enum class DealStatus { Pending, Active, Rejected, Ended }

@Serializable
data class Report(
	val id: String,
	@SerialName("post_id") val postId: String,
	@SerialName("reporter_id") val reporterId: String,
	val reason: String,
	@SerialName("created_at") val createdAt: String? = null
)