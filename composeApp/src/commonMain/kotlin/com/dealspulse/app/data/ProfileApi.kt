package com.dealspulse.app.data

import com.dealspulse.app.model.Profile
import com.dealspulse.app.model.AccountType
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
                .decodeSingle()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Create or update profile
     */
    suspend fun upsertProfile(profile: Profile): Profile {
        return Supa.postgrest["profiles"]
            .upsert(profile, returning = Returning.REPRESENTATION) {
                eq("user_id", profile.userId)
            }
            .decodeSingle()
    }
    
    /**
     * Update profile fields
     */
    suspend fun updateProfile(userId: String, updates: Map<String, Any>): Profile {
        return Supa.postgrest["profiles"]
            .update(updates, returning = Returning.REPRESENTATION) {
                eq("user_id", userId)
            }
            .decodeSingle()
    }
    
    /**
     * Get vendor profiles by business name (search)
     */
    suspend fun searchVendors(query: String, limit: Int = 20): List<Profile> {
        return Supa.postgrest["profiles"]
            .select {
                eq("account_type", AccountType.VENDOR.name)
                ilike("business_name", "%$query%")
                order("business_name", Order.ASCENDING)
                limit(limit)
            }
            .decodeList()
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
            .decodeList()
    }
    
    /**
     * Update user's preferred radius
     */
    suspend fun updateUserRadius(userId: String, radiusMiles: Int): Profile {
        return updateProfile(userId, mapOf("radius_miles" to radiusMiles))
    }
    
    /**
     * Update vendor verification status
     */
    suspend fun updateVendorVerification(userId: String, isVerified: Boolean): Profile {
        return updateProfile(userId, mapOf("is_verified" to isVerified))
    }
}