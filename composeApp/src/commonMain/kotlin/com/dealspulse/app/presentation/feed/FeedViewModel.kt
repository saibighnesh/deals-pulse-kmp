package com.dealspulse.app.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dealspulse.app.data.DealApi
import com.dealspulse.app.data.DealRealtimeEvent
import com.dealspulse.app.model.Deal
import com.dealspulse.app.util.haversineMiles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FeedViewModel(
    private val lat: Double,
    private val lng: Double,
    private val radiusMiles: Int
) : ViewModel() {

    private val _deals = MutableStateFlow<List<Deal>>(emptyList())
    val deals: StateFlow<List<Deal>> = _deals

    init {
        viewModelScope.launch {
            reload()
        }
        DealApi.subscribeToDeals(lat, lng, radiusMiles, viewModelScope)
        viewModelScope.launch {
            DealApi.updates.collect { event ->
                when (event) {
                    is DealRealtimeEvent.InsertOrUpdate -> {
                        val existing = _deals.value.filter { it.id != event.deal.id }
                        val updated = (existing + event.deal).sortedWith(
                            compareBy<Deal> { it.expiresAt }.thenBy {
                                haversineMiles(lat, lng, it.lat, it.lng)
                            }
                        )
                        _deals.value = updated
                    }
                    is DealRealtimeEvent.Delete -> {
                        _deals.value = _deals.value.filterNot { it.id == event.dealId }
                    }
                }
            }
        }
    }

    private suspend fun reload() {
        _deals.value = DealApi.fetchDeals(lat, lng, radiusMiles)
    }

    fun pullToRefresh() {
        viewModelScope.launch { reload() }
    }
}