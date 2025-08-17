package com.dealspulse.app.data

import com.dealspulse.app.model.Deal
import com.dealspulse.app.util.haversineMiles
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.decodeList
import io.github.jan.supabase.postgrest.decodeSingle
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

object DealApi {

    lateinit var client: SupabaseClient
        private set

    fun init(supabaseClient: SupabaseClient) {
        client = supabaseClient
    }

    suspend fun fetchDeals(
        lat: Double,
        lng: Double,
        radiusMiles: Int,
        category: String? = null
    ): List<Deal> {
        val nowIso = Clock.System.now().toString()

        var query = client.from("deals")
            .select()
            .eq("status", "active")
            .gte("expires_at", nowIso)

        if (category != null) {
            query = query.eq("category", category)
        }

        val allDeals: List<Deal> = query.decodeList()

        return allDeals
            .filter { haversineMiles(lat, lng, it.lat, it.lng) <= radiusMiles }
            .sortedWith(
                compareBy<Deal> { it.expiresAt }
                    .thenBy { haversineMiles(lat, lng, it.lat, it.lng) }
            )
    }

    suspend fun createDeal(deal: Deal): Deal {
        return client.from("deals").insert(deal).decodeSingle()
    }

    private val _updates = MutableSharedFlow<DealRealtimeEvent>()
    val updates: Flow<DealRealtimeEvent> = _updates.asSharedFlow()

    fun subscribeToDeals(
        lat: Double,
        lng: Double,
        radiusMiles: Int,
        coroutineScope: CoroutineScope
    ) {
        val channel = client.channel("public:deals")
        coroutineScope.launch {
            channel.join()
            channel.postgresChangeFlow<PostgresAction>(PostgresAction.ALL, schema = "public") {
                table = "deals"
            }.collect { change ->
                val record = change.record
                if (record != null) {
                    val deal: Deal = record.decodeSingle()
                    when (change.action) {
                        PostgresAction.INSERT, PostgresAction.UPDATE -> {
                            if (haversineMiles(lat, lng, deal.lat, deal.lng) <= radiusMiles) {
                                _updates.emit(DealRealtimeEvent.InsertOrUpdate(deal))
                            } else {
                                _updates.emit(DealRealtimeEvent.Delete(deal.id))
                            }
                        }
                        PostgresAction.DELETE -> {
                            _updates.emit(DealRealtimeEvent.Delete(deal.id))
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

sealed class DealRealtimeEvent {
    data class InsertOrUpdate(val deal: Deal): DealRealtimeEvent()
    data class Delete(val dealId: String): DealRealtimeEvent()
}