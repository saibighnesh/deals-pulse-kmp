package com.dealspulse.app.presentation.feed

import com.dealspulse.app.data.DealApi
import com.dealspulse.app.model.Deal
import com.dealspulse.app.model.DealStatus
import com.dealspulse.app.util.GeoUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedViewModel(
	private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
	private val _state = MutableStateFlow(FeedState())
	val state: StateFlow<FeedState> = _state.asStateFlow()

	private var realtimeChannel: DealApi.NoopRealtimeChannel? = null
	private var realtimeJob: Job? = null

	fun setLocation(lat: Double, lng: Double) {
		_state.update { it.copy(currentLat = lat, currentLng = lng) }
		refresh()
	}

	fun setRadius(miles: Int) {
		_state.update { it.copy(radiusMiles = miles) }
		filterAndSort()
	}

	fun setCategory(category: String?) {
		_state.update { it.copy(selectedCategory = category) }
		filterAndSort()
	}

	fun refresh() {
		scope.launch {
			_state.update { it.copy(isLoading = true, errorMessage = null) }
			try {
				val deals = DealApi.fetchActiveDeals(limit = 200)
				_state.update { it.copy(allDeals = deals, isLoading = false) }
				filterAndSort()
				ensureRealtime()
			} catch (t: Throwable) {
				_state.update { it.copy(isLoading = false, errorMessage = t.message ?: "Failed to load deals") }
			}
		}
	}

	private fun ensureRealtime() {
		if (realtimeChannel != null) return
		realtimeChannel = DealApi.subscribeDeals()
		// Realtime flows are no-op for now
	}

	private fun onUpsert(deal: Deal) {
		if (deal.status != DealStatus.Active) return
		_state.update { it.copy(allDeals = mergeReplace(it.allDeals, deal)) }
		filterAndSort()
	}

	private fun onUpdate(old: Deal?, new: Deal?) {
		val updated = new ?: return
		_state.update { it.copy(allDeals = mergeReplace(it.allDeals, updated)) }
		filterAndSort()
	}

	private fun onDelete(old: Deal?) {
		val deleted = old ?: return
		_state.update { it.copy(allDeals = it.allDeals.filterNot { d -> d.id == deleted.id }) }
		filterAndSort()
	}

	private fun mergeReplace(list: List<Deal>, item: Deal): List<Deal> {
		val idx = list.indexOfFirst { it.id == item.id }
		return if (idx >= 0) list.toMutableList().apply { set(idx, item) } else list + item
	}

	private fun filterAndSort() {
		val s = _state.value
		val lat = s.currentLat ?: run { _state.update { it.copy(visibleDeals = emptyList()) }; return }
		val lng = s.currentLng ?: run { _state.update { it.copy(visibleDeals = emptyList()) }; return }
		val radius = s.radiusMiles
		val filtered = s.allDeals.asSequence()
			.filter { it.status == DealStatus.Active }
			.filter { d -> s.selectedCategory?.let { cat -> d.category?.equals(cat, ignoreCase = true) == true } ?: true }
			.filter { d -> GeoUtil.haversineMiles(lat, lng, d.lat, d.lng) <= radius }
			.sortedWith(compareBy<Deal> { it.expiresAt }.thenBy { GeoUtil.haversineMiles(lat, lng, it.lat, it.lng) })
			.toList()
		_state.update { it.copy(visibleDeals = filtered) }
	}

	fun clear() {
		realtimeJob?.cancel()
		realtimeJob = null
		realtimeChannel = null
	}
}

data class FeedState(
	val isLoading: Boolean = false,
	val errorMessage: String? = null,
	val currentLat: Double? = null,
	val currentLng: Double? = null,
	val radiusMiles: Int = 10,
	val selectedCategory: String? = null,
	val allDeals: List<Deal> = emptyList(),
	val visibleDeals: List<Deal> = emptyList()
)