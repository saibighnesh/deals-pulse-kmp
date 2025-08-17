package com.dealspulse.app.data

import com.dealspulse.app.model.*
import com.dealspulse.app.util.GeoUtils
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder
import io.github.jan.supabase.postgrest.query.Returning
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterRequestBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class DealApi {
    
    /**
     * Fetch deals within a radius of a location, sorted by expiration time and distance
     */
    suspend fun fetchNearbyDeals(
        lat: Double,
        lng: Double,
        radiusMiles: Double,
        category: DealCategory? = null,
        limit: Int = 50
    ): List<DealWithVendor> {
        val now = Clock.System.now()
        
        // First, get all active deals that haven't expired
        val deals = Supa.postgrest["deals"]
            .select(
                columns = Columns.list("*, profiles(*)")
            ) {
                eq("status", DealStatus.ACTIVE.name)
                gt("expires_at", now.toString())
                order("expires_at", Order.ASCENDING)
                limit(limit * 2) // Get more to filter by distance
            }
            .decodeList<DealWithVendor>()
        
        // Filter by distance and category
        return deals
            .filter { dealWithVendor ->
                val deal = dealWithVendor.deal
                val vendor = dealWithVendor.vendor
                
                // Check if vendor has location
                if (vendor.lat == null || vendor.lng == null) return@filter false
                
                // Check distance
                val distance = GeoUtils.calculateDistance(lat, lng, vendor.lat, vendor.lng)
                if (distance > radiusMiles) return@filter false
                
                // Check category if specified
                if (category != null && deal.category != category) return@filter false
                
                true
            }
            .sortedWith(
                compareBy<DealWithVendor> { dealWithVendor ->
                    val deal = dealWithVendor.deal
                    val vendor = dealWithVendor.vendor
                    if (vendor.lat != null && vendor.lng != null) {
                        GeoUtils.calculateDistance(lat, lng, vendor.lat, vendor.lng)
                    } else {
                        Double.MAX_VALUE
                    }
                }.thenBy { dealWithVendor ->
                    dealWithVendor.deal.expiresAt
                }
            )
            .take(limit)
    }
    
    /**
     * Fetch deals by vendor ID
     */
    suspend fun fetchVendorDeals(
        vendorId: String,
        status: DealStatus? = null,
        limit: Int = 50
    ): List<Deal> {
        val query = Supa.postgrest["deals"]
            .select {
                eq("vendor_id", vendorId)
                if (status != null) {
                    eq("status", status.name)
                }
                order("created_at", Order.DESCENDING)
                limit(limit)
            }
        
        return query.decodeList<Deal>()
    }
    
    /**
     * Fetch a single deal by ID with vendor information
     */
    suspend fun fetchDeal(id: String): DealWithVendor? {
        return try {
            Supa.postgrest["deals"]
                .select(
                    columns = Columns.list("*, profiles(*)")
                ) {
                    eq("id", id)
                }
                .decodeSingle<DealWithVendor>()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Create a new deal
     */
    suspend fun createDeal(
        vendorId: String,
        title: String,
        description: String,
        category: DealCategory,
        price: Double,
        originalPrice: Double?,
        imageUrl: String?,
        lat: Double,
        lng: Double,
        expiresAt: Instant
    ): Deal {
        val geohash = GeoUtils.generateGeohash(lat, lng)
        
        val dealData = mapOf(
            "vendor_id" to vendorId,
            "title" to title,
            "description" to description,
            "category" to category.name,
            "price" to price,
            "original_price" to originalPrice,
            "image_url" to imageUrl,
            "lat" to lat,
            "lng" to lng,
            "geohash" to geohash,
            "status" to DealStatus.PENDING.name,
            "expires_at" to expiresAt.toString()
        )
        
        return Supa.postgrest["deals"]
            .insert(dealData, returning = Returning.REPRESENTATION)
            .decodeSingle<Deal>()
    }
    
    /**
     * Update deal status
     */
    suspend fun updateDealStatus(dealId: String, status: DealStatus): Deal {
        val updateData = mapOf("status" to status.name)
        
        return Supa.postgrest["deals"]
            .update(updateData, returning = Returning.REPRESENTATION) {
                eq("id", dealId)
            }
            .decodeSingle<Deal>()
    }
    
    /**
     * Delete a deal
     */
    suspend fun deleteDeal(dealId: String) {
        Supa.postgrest["deals"]
            .delete {
                eq("id", dealId)
            }
    }
    
    /**
     * Search deals by text
     */
    suspend fun searchDeals(
        query: String,
        lat: Double? = null,
        lng: Double? = null,
        radiusMiles: Double? = null,
        limit: Int = 50
    ): List<DealWithVendor> {
        val now = Clock.System.now()
        
        val deals = Supa.postgrest["deals"]
            .select(
                columns = Columns.list("*, profiles(*)")
            ) {
                eq("status", DealStatus.ACTIVE.name)
                gt("expires_at", now.toString())
                or {
                    ilike("title", "%$query%")
                    ilike("description", "%$query%")
                }
                order("expires_at", Order.ASCENDING)
                limit(limit * 2)
            }
            .decodeList<DealWithVendor>()
        
        // Filter by distance if location is provided
        if (lat != null && lng != null && radiusMiles != null) {
            return deals.filter { dealWithVendor ->
                val vendor = dealWithVendor.vendor
                if (vendor.lat == null || vendor.lng == null) return@filter false
                
                val distance = GeoUtils.calculateDistance(lat, lng, vendor.lat, vendor.lng)
                distance <= radiusMiles
            }.take(limit)
        }
        
        return deals.take(limit)
    }
}