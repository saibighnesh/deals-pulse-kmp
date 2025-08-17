package com.dealspulse.app.data

import com.dealspulse.app.model.Deal
import com.dealspulse.app.model.DealStatus
import com.dealspulse.app.util.GeoUtils
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class DealApi {
    
    /**
     * Fetch active deals within radius of given location
     */
    suspend fun fetchDealsNearby(
        lat: Double,
        lng: Double,
        radiusMiles: Double,
        category: String? = null,
        limit: Int = 50
    ): List<Deal> {
        val geoHashPrefixes = GeoUtils.getGeoHashPrefixes(lat, lng, radiusMiles)
        val now = Clock.System.now()
        
        var query = Supa.database.from("deals")
            .select(
                Columns.raw("""
                    *,
                    profiles!deals_vendor_id_fkey(
                        business_name,
                        phone,
                        address,
                        is_verified
                    )
                """.trimIndent())
            )
            .eq("status", DealStatus.ACTIVE.name.lowercase())
            .gt("expires_at", now.toString())
        
        // Filter by geohash prefixes for approximate location filtering
        if (geoHashPrefixes.isNotEmpty()) {
            // Use the first (most precise) geohash prefix
            query = query.like("geohash", "${geoHashPrefixes.first()}*")
        }
        
        // Filter by category if specified
        category?.let {
            query = query.eq("category", it.lowercase())
        }
        
        val response = query
            .order("expires_at", Order.ASCENDING)
            .limit(limit.toLong())
            .decodeList<DealWithProfile>()
        
        // Convert to Deal objects and filter by exact distance
        return response
            .map { it.toDeal() }
            .filter { deal ->
                val distance = GeoUtils.calculateDistance(lat, lng, deal.lat, deal.lng)
                distance <= radiusMiles
            }
            .sortedWith(compareBy<Deal> { it.expiresAt }.thenBy { 
                GeoUtils.calculateDistance(lat, lng, it.lat, it.lng) 
            })
    }
    
    /**
     * Fetch deals by vendor
     */
    suspend fun fetchDealsByVendor(vendorId: String): List<Deal> {
        val response = Supa.database.from("deals")
            .select(
                Columns.raw("""
                    *,
                    profiles!deals_vendor_id_fkey(
                        business_name,
                        phone,
                        address,
                        is_verified
                    )
                """.trimIndent())
            )
            .eq("vendor_id", vendorId)
            .order("created_at", Order.DESCENDING)
            .decodeList<DealWithProfile>()
        
        return response.map { it.toDeal() }
    }
    
    /**
     * Fetch single deal by ID
     */
    suspend fun fetchDealById(dealId: String): Deal? {
        val response = Supa.database.from("deals")
            .select(
                Columns.raw("""
                    *,
                    profiles!deals_vendor_id_fkey(
                        business_name,
                        phone,
                        address,
                        is_verified
                    )
                """.trimIndent())
            )
            .eq("id", dealId)
            .maybeSingle<DealWithProfile>()
        
        return response?.toDeal()
    }
    
    /**
     * Create a new deal
     */
    suspend fun createDeal(
        title: String,
        description: String,
        category: String,
        price: String,
        imageUrl: String?,
        lat: Double,
        lng: Double,
        expiresAt: Instant
    ): Deal {
        val vendorId = Supa.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("User must be authenticated")
        
        val geohash = GeoUtils.generateGeoHash(lat, lng)
        
        val newDeal = mapOf(
            "vendor_id" to vendorId,
            "title" to title,
            "description" to description,
            "category" to category.lowercase(),
            "price" to price,
            "image_url" to imageUrl,
            "lat" to lat,
            "lng" to lng,
            "geohash" to geohash,
            "status" to DealStatus.ACTIVE.name.lowercase(),
            "expires_at" to expiresAt.toString()
        )
        
        val response = Supa.database.from("deals")
            .insert(newDeal)
            .select(
                Columns.raw("""
                    *,
                    profiles!deals_vendor_id_fkey(
                        business_name,
                        phone,
                        address,
                        is_verified
                    )
                """.trimIndent())
            )
            .decodeSingle<DealWithProfile>()
        
        return response.toDeal()
    }
    
    /**
     * Update deal status
     */
    suspend fun updateDealStatus(dealId: String, status: DealStatus): Boolean {
        return try {
            Supa.database.from("deals")
                .update(mapOf("status" to status.name.lowercase()))
                .eq("id", dealId)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Delete deal
     */
    suspend fun deleteDeal(dealId: String): Boolean {
        return try {
            Supa.database.from("deals")
                .delete()
                .eq("id", dealId)
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Internal data class for handling joined queries
 */
@kotlinx.serialization.Serializable
private data class DealWithProfile(
    val id: String,
    val vendor_id: String,
    val title: String,
    val description: String,
    val category: String,
    val price: String,
    val image_url: String? = null,
    val lat: Double,
    val lng: Double,
    val geohash: String,
    val status: String,
    val created_at: String,
    val expires_at: String,
    val is_promoted: Boolean = false,
    val profiles: ProfileData? = null
) {
    fun toDeal(): Deal {
        return Deal(
            id = id,
            vendorId = vendor_id,
            title = title,
            description = description,
            category = category,
            price = price,
            imageUrl = image_url,
            lat = lat,
            lng = lng,
            geohash = geohash,
            status = when (status.lowercase()) {
                "active" -> DealStatus.ACTIVE
                "pending" -> DealStatus.PENDING
                "rejected" -> DealStatus.REJECTED
                "ended" -> DealStatus.ENDED
                else -> DealStatus.PENDING
            },
            createdAt = Instant.parse(created_at),
            expiresAt = Instant.parse(expires_at),
            isPromoted = is_promoted,
            vendorBusinessName = profiles?.business_name,
            vendorPhone = profiles?.phone,
            vendorAddress = profiles?.address,
            vendorIsVerified = profiles?.is_verified ?: false
        )
    }
}

@kotlinx.serialization.Serializable
private data class ProfileData(
    val business_name: String?,
    val phone: String?,
    val address: String?,
    val is_verified: Boolean
)