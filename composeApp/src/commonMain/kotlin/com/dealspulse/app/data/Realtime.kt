package com.dealspulse.app.data

import com.dealspulse.app.model.Deal
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.createChannel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class RealtimeApi {
    
    private val dealApi = DealApi()
    
    /**
     * Subscribe to real-time deal changes
     * Returns a flow of deal change events
     */
    fun subscribeToDeals(): Flow<DealChange> {
        val channel = Supa.realtime.createChannel("deals-channel")
        
        val changesFlow = channel.postgresChangeFlow<JsonObject>(
            schema = "public"
        ) {
            table = "deals"
        }
        
        channel.subscribe()
        
        return changesFlow.map { change ->
            when (change.action) {
                PostgresAction.INSERT -> {
                    val dealData = Json.decodeFromJsonElement<DealData>(change.record)
                    DealChange.Insert(dealData.toDeal())
                }
                PostgresAction.UPDATE -> {
                    val dealData = Json.decodeFromJsonElement<DealData>(change.record)
                    DealChange.Update(dealData.toDeal())
                }
                PostgresAction.DELETE -> {
                    val oldRecord = change.oldRecord
                    if (oldRecord != null) {
                        val dealData = Json.decodeFromJsonElement<DealData>(oldRecord)
                        DealChange.Delete(dealData.id)
                    } else {
                        DealChange.Delete("unknown")
                    }
                }
                else -> DealChange.Unknown
            }
        }
    }
    
    /**
     * Subscribe to deals with location filtering
     */
    fun subscribeToDealsNearby(
        lat: Double,
        lng: Double,
        radiusMiles: Double
    ): Flow<DealChange> {
        return subscribeToDeals()
            .map { change ->
                when (change) {
                    is DealChange.Insert -> {
                        val distance = com.dealspulse.app.util.GeoUtils.calculateDistance(
                            lat, lng, change.deal.lat, change.deal.lng
                        )
                        if (distance <= radiusMiles && change.deal.isActive) {
                            change
                        } else {
                            DealChange.Unknown
                        }
                    }
                    is DealChange.Update -> {
                        val distance = com.dealspulse.app.util.GeoUtils.calculateDistance(
                            lat, lng, change.deal.lat, change.deal.lng
                        )
                        if (distance <= radiusMiles) {
                            change
                        } else {
                            DealChange.Delete(change.deal.id)
                        }
                    }
                    else -> change
                }
            }
    }
}

/**
 * Sealed class representing different types of deal changes
 */
sealed class DealChange {
    data class Insert(val deal: Deal) : DealChange()
    data class Update(val deal: Deal) : DealChange()
    data class Delete(val dealId: String) : DealChange()
    object Unknown : DealChange()
}

/**
 * Simplified deal data for realtime updates
 */
@kotlinx.serialization.Serializable
private data class DealData(
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
    val is_promoted: Boolean = false
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
                "active" -> com.dealspulse.app.model.DealStatus.ACTIVE
                "pending" -> com.dealspulse.app.model.DealStatus.PENDING
                "rejected" -> com.dealspulse.app.model.DealStatus.REJECTED
                "ended" -> com.dealspulse.app.model.DealStatus.ENDED
                else -> com.dealspulse.app.model.DealStatus.PENDING
            },
            createdAt = kotlinx.datetime.Instant.parse(created_at),
            expiresAt = kotlinx.datetime.Instant.parse(expires_at),
            isPromoted = is_promoted
        )
    }
}