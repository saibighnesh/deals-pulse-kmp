package com.dealspulse.app.data

import com.dealspulse.app.model.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.Returning

class ProfileApi {
    
    /**
     * Get profile by user ID
     */
    suspend fun getProfile(userId: String): Profile? {
        return try {
            Supa.postgrest["profiles"]
                .select {
                    eq("user_id", userId)
                }
                .decodeSingle<Profile>()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Create or update profile
     */
    suspend fun upsertProfile(profile: Profile): Profile {
        val profileData = mapOf(
            "user_id" to profile.userId,
            "account_type" to profile.accountType.name,
            "business_name" to profile.businessName,
            "logo_url" to profile.logoUrl,
            "address" to profile.address,
            "lat" to profile.lat,
            "lng" to profile.lng,
            "phone" to profile.phone,
            "is_verified" to profile.isVerified,
            "radius_miles" to profile.radiusMiles
        )
        
        return Supa.postgrest["profiles"]
            .upsert(profileData, returning = Returning.REPRESENTATION) {
                onConflict("user_id")
            }
            .decodeSingle<Profile>()
    }
    
    /**
     * Update profile verification status
     */
    suspend fun updateVerificationStatus(userId: String, isVerified: Boolean): Profile {
        val updateData = mapOf("is_verified" to isVerified)
        
        return Supa.postgrest["profiles"]
            .update(updateData, returning = Returning.REPRESENTATION) {
                eq("user_id", userId)
            }
            .decodeSingle<Profile>()
    }
    
    /**
     * Update user's preferred radius
     */
    suspend fun updateRadius(userId: String, radiusMiles: Int): Profile {
        val updateData = mapOf("radius_miles" to radiusMiles)
        
        return Supa.postgrest["profiles"]
            .update(updateData, returning = Returning.REPRESENTATION) {
                eq("user_id", userId)
            }
            .decodeSingle<Profile>()
    }
    
    /**
     * Search vendors by business name
     */
    suspend fun searchVendors(query: String, limit: Int = 20): List<Profile> {
        return Supa.postgrest["profiles"]
            .select {
                eq("account_type", AccountType.VENDOR.name)
                ilike("business_name", "%$query%")
                order("business_name", Order.ASCENDING)
                limit(limit)
            }
            .decodeList<Profile>()
    }
    
    /**
     * Get verified vendors
     */
    suspend fun getVerifiedVendors(limit: Int = 50): List<Profile> {
        return Supa.postgrest["profiles"]
            .select {
                eq("account_type", AccountType.VENDOR.name)
                eq("is_verified", true)
                order("business_name", Order.ASCENDING)
                limit(limit)
            }
            .decodeList<Profile>()
    }
}