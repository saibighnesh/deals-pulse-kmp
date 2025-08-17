package com.dealspulse.app.data

import com.dealspulse.app.model.Deal
import com.dealspulse.app.model.DealStatus
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.postgresListFlow
import io.github.jan.supabase.realtime.postgresListFlowUntil
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class RealtimeService {
    
    /**
     * Subscribe to all deal changes
     */
    fun subscribeToDeals(): Flow<DealChange> {
        return Supa.realtime
            .createChannel("public:deals")
            .postgresListFlow<Deal>("deals")
            .map { change ->
                DealChange(
                    type = change.type,
                    deal = change.record,
                    timestamp = Clock.System.now()
                )
            }
    }
    
    /**
     * Subscribe to deals with filtering
     */
    fun subscribeToDealsWithFilter(
        status: DealStatus? = null,
        vendorId: String? = null
    ): Flow<DealChange> {
        return Supa.realtime
            .createChannel("public:deals")
            .postgresListFlow<Deal>("deals") {
                status?.let { eq("status", it.name) }
                vendorId?.let { eq("vendor_id", it) }
            }
            .map { change ->
                DealChange(
                    type = change.type,
                    deal = change.record,
                    timestamp = Clock.System.now()
                )
            }
    }
    
    /**
     * Subscribe to deals in a specific area (using geohash prefix)
     */
    fun subscribeToDealsInArea(geohashPrefix: String): Flow<DealChange> {
        return Supa.realtime
            .createChannel("public:deals")
            .postgresListFlow<Deal>("deals") {
                like("geohash", "$geohashPrefix%")
                eq("status", DealStatus.ACTIVE.name)
            }
            .map { change ->
                DealChange(
                    type = change.type,
                    deal = change.record,
                    timestamp = Clock.System.now()
                )
            }
    }
    
    /**
     * Subscribe to expiring deals
     */
    fun subscribeToExpiringDeals(withinHours: Int = 1): Flow<DealChange> {
        val now = Clock.System.now()
        val cutoff = now.plus(kotlinx.datetime.Duration.parse("PT${withinHours}H"))
        
        return Supa.realtime
            .createChannel("public:deals")
            .postgresListFlow<Deal>("deals") {
                eq("status", DealStatus.ACTIVE.name)
                lt("expires_at", cutoff.toString())
            }
            .map { change ->
                DealChange(
                    type = change.type,
                    deal = change.record,
                    timestamp = Clock.System.now()
                )
            }
    }
}

data class DealChange(
    val type: String,
    val deal: Deal,
    val timestamp: kotlinx.datetime.Instant
)

enum class ChangeType {
    INSERT,
    UPDATE,
    DELETE
}