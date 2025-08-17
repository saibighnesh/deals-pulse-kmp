package com.dealspulse.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    @SerialName("user_id")
    val userId: String,
    
    @SerialName("account_type")
    val accountType: AccountType,
    
    @SerialName("business_name")
    val businessName: String? = null,
    
    @SerialName("logo_url")
    val logoUrl: String? = null,
    
    val address: String? = null,
    
    val lat: Double? = null,
    
    val lng: Double? = null,
    
    val phone: String? = null,
    
    @SerialName("is_verified")
    val isVerified: Boolean = false,
    
    @SerialName("radius_miles")
    val radiusMiles: Int = 10
)

@Serializable
enum class AccountType {
    @SerialName("user")
    USER,
    
    @SerialName("vendor")
    VENDOR
}