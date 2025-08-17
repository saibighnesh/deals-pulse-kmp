package com.dealspulse.app.data

import com.dealspulse.app.model.Deal
import com.dealspulse.app.model.DealStatus
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.postgresListFlow
import io.github.jan.supabase.realtime.postgresListFlowWithAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class RealtimeService {
    
    /**
     * Subscribe to all deal changes (inserts, updates, deletes)
     */
    fun subscribeToDeals(): Flow<RealtimeEvent<Deal>> {
        return Supa.realtime
            .postgresListFlowWithAuth<Deal>(
                schema = "public",
                table = "deals"
            )
            .map { change ->
                when (change.eventType) {
                    "INSERT" -> RealtimeEvent.Insert(change.record)
                    "UPDATE" -> RealtimeEvent.Update(change.oldRecord, change.record)
                    "DELETE" -> RealtimeEvent.Delete(change.oldRecord)
                    else -> RealtimeEvent.Unknown
                }
            }
    }
    
    /**
     * Subscribe to deals within a specific geohash range
     */
    fun subscribeToDealsInArea(geohash: String, precision: Int = 4): Flow<RealtimeEvent<Deal>> {
        // This would require a more sophisticated geohash implementation
        // For now, we'll subscribe to all deals and filter in the app
        return subscribeToDeals()
    }
    
    /**
     * Subscribe to deals by vendor
     */
    fun subscribeToVendorDeals(vendorId: String): Flow<RealtimeEvent<Deal>> {
        return Supa.realtime
            .postgresListFlowWithAuth<Deal>(
                schema = "public",
                table = "deals",
                filter = "vendor_id=eq.$vendorId"
            )
            .map { change ->
                when (change.eventType) {
                    "INSERT" -> RealtimeEvent.Insert(change.record)
                    "UPDATE" -> RealtimeEvent.Update(change.oldRecord, change.record)
                    "DELETE" -> RealtimeEvent.Delete(change.oldRecord)
                    else -> RealtimeEvent.Unknown
                }
            }
    }
    
    /**
     * Subscribe to active deals only
     */
    fun subscribeToActiveDeals(): Flow<RealtimeEvent<Deal>> {
        return Supa.realtime
            .postgresListFlowWithAuth<Deal>(
                schema = "public",
                table = "deals",
                filter = "status=eq.${DealStatus.ACTIVE.name}"
            )
            .map { change ->
                when (change.eventType) {
                    "INSERT" -> RealtimeEvent.Insert(change.record)
                    "UPDATE" -> RealtimeEvent.Update(change.oldRecord, change.record)
                    "DELETE" -> RealtimeEvent.Delete(change.oldRecord)
                    else -> RealtimeEvent.Unknown
                }
            }
    }
}

sealed class RealtimeEvent<T> {
    data class Insert<T>(val record: T) : RealtimeEvent<T>()
    data class Update<T>(val oldRecord: T?, val newRecord: T) : RealtimeEvent<T>()
    data class Delete<T>(val record: T?) : RealtimeEvent<T>()
    object Unknown : RealtimeEvent<Nothing>()
}