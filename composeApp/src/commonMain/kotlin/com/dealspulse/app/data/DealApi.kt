package com.dealspulse.app.data

import com.dealspulse.app.config.AppConfig
import com.dealspulse.app.model.Deal
import com.dealspulse.app.model.DealStatus
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

object DealApi {
	private val client get() = Http.client
	private val baseUrl get() = AppConfig.supabaseUrl
	private val anonKey get() = AppConfig.supabaseAnonKey

	suspend fun fetchActiveDeals(limit: Int = 100): List<Deal> {
		val url = URLBuilder(baseUrl).apply { appendPathSegments("rest", "v1", "deals") }.build()
		val response = client.get(url) {
			header("apikey", anonKey)
			header("Authorization", "Bearer $anonKey")
			parameter("select", "*,vendor_profile:profiles!deals_vendor_id_fkey(*)")
			parameter("status", "eq.${DealStatus.Active.name}")
			parameter("order", "expires_at.asc")
			parameter("limit", limit)
		}
		return response.body()
	}

	// Stubs for create/update/end to match API surface
	suspend fun createDeal(deal: Deal): Deal { throw NotImplementedError("Use PostgREST insert in platform code") }
	suspend fun updateDeal(id: String, patch: Map<String, Any?>): Deal { throw NotImplementedError("Use PostgREST update in platform code") }
	suspend fun endDeal(id: String): Deal { return updateDeal(id, mapOf("status" to DealStatus.Ended.name)) }

	// Realtime no-op stubs to keep the presentation layer compiling
	class NoopRealtimeChannel { suspend fun subscribe() {} suspend fun unsubscribe() {} }
	fun subscribeDeals(): NoopRealtimeChannel = NoopRealtimeChannel()
	fun insertedFlow(channel: NoopRealtimeChannel): Flow<Deal> = emptyFlow()
	fun updatedFlow(channel: NoopRealtimeChannel): Flow<Pair<Deal?, Deal?>> = emptyFlow()
	fun deletedFlow(channel: NoopRealtimeChannel): Flow<Deal?> = emptyFlow()
}