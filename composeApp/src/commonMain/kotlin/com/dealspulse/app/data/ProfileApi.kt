package com.dealspulse.app.data

import com.dealspulse.app.model.AccountType
import com.dealspulse.app.model.Profile
import com.dealspulse.app.util.GeoUtils
import io.github.jan.supabase.gotrue.providers.builtin.Email

class ProfileApi {
    
    /**
     * Sign up new user
     */
    suspend fun signUp(email: String, password: String): Boolean {
        return try {
            Supa.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Sign in user
     */
    suspend fun signIn(email: String, password: String): Boolean {
        return try {
            Supa.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Sign out current user
     */
    suspend fun signOut(): Boolean {
        return try {
            Supa.auth.signOut()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get current user profile
     */
    suspend fun getCurrentProfile(): Profile? {
        val user = Supa.auth.currentUserOrNull() ?: return null
        
        return try {
            val response = Supa.database.from("profiles")
                .select()
                .eq("user_id", user.id)
                .maybeSingle<ProfileData>()
            
            response?.toProfile()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Create or update profile
     */
    suspend fun upsertProfile(
        accountType: AccountType,
        businessName: String? = null,
        address: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        phone: String? = null,
        radiusMiles: Int = 10
    ): Profile? {
        val user = Supa.auth.currentUserOrNull() ?: return null
        
        val profileData = mapOf(
            "user_id" to user.id,
            "account_type" to accountType.name.lowercase(),
            "business_name" to businessName,
            "address" to address,
            "lat" to lat,
            "lng" to lng,
            "phone" to phone,
            "radius_miles" to radiusMiles
        )
        
        return try {
            val response = Supa.database.from("profiles")
                .upsert(profileData)
                .select()
                .decodeSingle<ProfileData>()
            
            response.toProfile()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get profile by user ID
     */
    suspend fun getProfileById(userId: String): Profile? {
        return try {
            val response = Supa.database.from("profiles")
                .select()
                .eq("user_id", userId)
                .maybeSingle<ProfileData>()
            
            response?.toProfile()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Update profile verification status (admin only)
     */
    suspend fun updateVerificationStatus(userId: String, isVerified: Boolean): Boolean {
        return try {
            Supa.database.from("profiles")
                .update(mapOf("is_verified" to isVerified))
                .eq("user_id", userId)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if current user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return Supa.auth.currentUserOrNull() != null
    }
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return Supa.auth.currentUserOrNull()?.id
    }
}

/**
 * Internal data class for profile serialization
 */
@kotlinx.serialization.Serializable
private data class ProfileData(
    val user_id: String,
    val account_type: String,
    val business_name: String? = null,
    val logo_url: String? = null,
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val phone: String? = null,
    val is_verified: Boolean = false,
    val radius_miles: Int = 10
) {
    fun toProfile(): Profile {
        return Profile(
            userId = user_id,
            accountType = when (account_type.lowercase()) {
                "vendor" -> AccountType.VENDOR
                else -> AccountType.USER
            },
            businessName = business_name,
            logoUrl = logo_url,
            address = address,
            lat = lat,
            lng = lng,
            phone = phone,
            isVerified = is_verified,
            radiusMiles = radius_miles
        )
    }
}