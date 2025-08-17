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
     * Fetch deals by vendor
     */
    suspend fun fetchVendorDeals(vendorId: String): List<Deal> {
        return Supa.postgrest["deals"]
            .select {
                eq("vendor_id", vendorId)
                order("created_at", Order.DESCENDING)
            }
            .decodeList()
    }
    
    /**
     * Fetch a single deal by ID
     */
    suspend fun fetchDeal(dealId: String): DealWithVendor? {
        return try {
            Supa.postgrest["deals"]
                .select(
                    columns = Columns.list("*, profiles(*)")
                ) {
                    eq("id", dealId)
                }
                .decodeSingle<DealWithVendor>()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Create a new deal
     */
    suspend fun createDeal(deal: Deal): Deal {
        return Supa.postgrest["deals"]
            .insert(deal, returning = Returning.REPRESENTATION)
            .decodeSingle()
    }
    
    /**
     * Update a deal
     */
    suspend fun updateDeal(dealId: String, updates: Map<String, Any>): Deal {
        return Supa.postgrest["deals"]
            .update(updates, returning = Returning.REPRESENTATION) {
                eq("id", dealId)
            }
            .decodeSingle()
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
     * Mark a deal as ended
     */
    suspend fun endDeal(dealId: String): Deal {
        return updateDeal(dealId, mapOf("status" to DealStatus.ENDED.name))
    }
    
    /**
     * Search deals by text
     */
    suspend fun searchDeals(
        query: String,
        lat: Double,
        lng: Double,
        radiusMiles: Double,
        limit: Int = 20
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
        
        // Filter by distance
        return deals
            .filter { dealWithVendor ->
                val vendor = dealWithVendor.vendor
                if (vendor.lat == null || vendor.lng == null) return@filter false
                
                val distance = GeoUtils.calculateDistance(lat, lng, vendor.lat, vendor.lng)
                distance <= radiusMiles
            }
            .sortedWith(
                compareBy<DealWithVendor> { dealWithVendor ->
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
     * Get deals that are expiring soon (within next hour)
     */
    suspend fun fetchExpiringSoonDeals(
        lat: Double,
        lng: Double,
        radiusMiles: Double,
        withinHours: Int = 1
    ): List<DealWithVendor> {
        val now = Clock.System.now()
        val cutoff = Instant.fromEpochSeconds(now.epochSeconds + (withinHours * 3600))
        
        val deals = Supa.postgrest["deals"]
            .select(
                columns = Columns.list("*, profiles(*)")
            ) {
                eq("status", DealStatus.ACTIVE.name)
                gt("expires_at", now.toString())
                lt("expires_at", cutoff.toString())
                order("expires_at", Order.ASCENDING)
            }
            .decodeList<DealWithVendor>()
        
        // Filter by distance
        return deals.filter { dealWithVendor ->
            val vendor = dealWithVendor.vendor
            if (vendor.lat == null || vendor.lng == null) return@filter false
            
            val distance = GeoUtils.calculateDistance(lat, lng, vendor.lat, vendor.lng)
            distance <= radiusMiles
        }
    }
}