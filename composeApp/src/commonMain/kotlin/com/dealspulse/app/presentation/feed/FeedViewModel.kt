package com.dealspulse.app.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dealspulse.app.data.*
import com.dealspulse.app.location.LocationProvider
import com.dealspulse.app.model.*
import com.dealspulse.app.util.GeoUtils
import com.dealspulse.app.util.TimeUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class FeedViewModel(
    private val dealApi: DealApi = DealApi(),
    private val realtimeService: RealtimeService = RealtimeService(),
    private val locationProvider: LocationProvider
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
    
    private val _deals = MutableStateFlow<List<DealWithVendor>>(emptyList())
    val deals: StateFlow<List<DealWithVendor>> = _deals.asStateFlow()
    
    private var currentLocation: Location? = null
    private var currentRadius: Int = 10
    private var selectedCategory: DealCategory? = null
    
    init {
        viewModelScope.launch {
            // Get initial location
            if (locationProvider.hasLocationPermission()) {
                currentLocation = locationProvider.getCurrentLocation()
                loadDeals()
            }
            
            // Subscribe to real-time updates
            realtimeService.subscribeToActiveDeals()
                .collect { event ->
                    handleRealtimeEvent(event)
                }
        }
    }
    
    fun setRadius(radiusMiles: Int) {
        currentRadius = radiusMiles
        _uiState.update { it.copy(selectedRadius = radiusMiles) }
        loadDeals()
    }
    
    fun setCategory(category: DealCategory?) {
        selectedCategory = category
        _uiState.update { it.copy(selectedCategory = category) }
        loadDeals()
    }
    
    fun refresh() {
        loadDeals()
    }
    
    fun requestLocationPermission() {
        viewModelScope.launch {
            val granted = locationProvider.requestLocationPermission()
            if (granted) {
                currentLocation = locationProvider.getCurrentLocation()
                loadDeals()
            }
            _uiState.update { it.copy(hasLocationPermission = granted) }
        }
    }
    
    private suspend fun loadDeals() {
        if (currentLocation == null) {
            _uiState.update { it.copy(isLoading = false, error = "Location not available") }
            return
        }
        
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        try {
            val deals = dealApi.fetchNearbyDeals(
                lat = currentLocation!!.lat,
                lng = currentLocation!!.lng,
                radiusMiles = currentRadius.toDouble(),
                category = selectedCategory
            )
            
            _deals.value = deals
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    error = null,
                    isEmpty = deals.isEmpty()
                )
            }
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    error = "Failed to load deals: ${e.message}"
                )
            }
        }
    }
    
    private fun handleRealtimeEvent(event: RealtimeEvent<Deal>) {
        when (event) {
            is RealtimeEvent.Insert -> {
                val newDeal = event.record
                // Add new deal if it meets our criteria
                if (shouldShowDeal(newDeal)) {
                    addDealToList(newDeal)
                }
            }
            is RealtimeEvent.Update -> {
                val oldDeal = event.oldRecord
                val newDeal = event.newRecord
                
                if (oldDeal != null) {
                    // Remove old deal if it no longer meets criteria
                    if (!shouldShowDeal(newDeal)) {
                        removeDealFromList(oldDeal.id)
                    } else {
                        // Update existing deal
                        updateDealInList(newDeal)
                    }
                } else if (shouldShowDeal(newDeal)) {
                    // This is a new deal that was just approved
                    addDealToList(newDeal)
                }
            }
            is RealtimeEvent.Delete -> {
                val deletedDeal = event.record
                if (deletedDeal != null) {
                    removeDealFromList(deletedDeal.id)
                }
            }
            is RealtimeEvent.Unknown -> {
                // Ignore unknown events
            }
        }
    }
    
    private fun shouldShowDeal(deal: Deal): Boolean {
        if (deal.status != DealStatus.ACTIVE) return false
        if (TimeUtils.isExpired(deal.expiresAt)) return false
        if (currentLocation == null) return false
        
        // Check if vendor has location
        val vendor = _deals.value.find { it.deal.id == deal.id }?.vendor
        if (vendor?.lat == null || vendor.lng == null) return false
        
        // Check distance
        val distance = GeoUtils.calculateDistance(
            currentLocation!!.lat,
            currentLocation!!.lng,
            vendor.lat,
            vendor.lng
        )
        if (distance > currentRadius) return false
        
        // Check category
        if (selectedCategory != null && deal.category != selectedCategory) return false
        
        return true
    }
    
    private fun addDealToList(deal: Deal) {
        viewModelScope.launch {
            // Fetch vendor profile for the new deal
            val vendor = getVendorProfile(deal.vendorId)
            if (vendor != null) {
                val dealWithVendor = DealWithVendor(deal, vendor)
                val currentDeals = _deals.value.toMutableList()
                currentDeals.add(0, dealWithVendor) // Add to top
                _deals.value = currentDeals
                
                _uiState.update { it.copy(isEmpty = false) }
            }
        }
    }
    
    private fun updateDealInList(deal: Deal) {
        val currentDeals = _deals.value.toMutableList()
        val index = currentDeals.indexOfFirst { it.deal.id == deal.id }
        if (index != -1) {
            val vendor = currentDeals[index].vendor
            currentDeals[index] = DealWithVendor(deal, vendor)
            _deals.value = currentDeals
        }
    }
    
    private fun removeDealFromList(dealId: String) {
        val currentDeals = _deals.value.toMutableList()
        currentDeals.removeAll { it.deal.id == dealId }
        _deals.value = currentDeals
        
        _uiState.update { it.copy(isEmpty = currentDeals.isEmpty()) }
    }
    
    private suspend fun getVendorProfile(vendorId: String): Profile? {
        return try {
            val profileApi = ProfileApi()
            profileApi.getProfile(vendorId)
        } catch (e: Exception) {
            null
        }
    }
}

data class FeedUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEmpty: Boolean = false,
    val selectedRadius: Int = 10,
    val selectedCategory: DealCategory? = null,
    val hasLocationPermission: Boolean = false
)