package com.dealspulse.app.presentation.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.dealspulse.app.data.DealApi
import com.dealspulse.app.data.RealtimeService
import com.dealspulse.app.location.LocationProvider
import com.dealspulse.app.model.*
import com.dealspulse.app.util.GeoUtils
import com.dealspulse.app.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class FeedViewModel(
    private val dealApi: DealApi,
    private val realtimeService: RealtimeService,
    private val locationProvider: LocationProvider
) {
    
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    var uiState by mutableStateOf(FeedUiState())
        private set
    
    var selectedCategory by mutableStateOf<DealCategory?>(null)
        private set
    
    var selectedRadius by mutableStateOf(10)
        private set
    
    private var currentLocation: Location? = null
    
    init {
        viewModelScope.launch {
            initializeLocation()
        }
        
        // Subscribe to real-time updates
        viewModelScope.launch {
            realtimeService.subscribeToDeals()
                .collect { change ->
                    handleRealtimeChange(change)
                }
        }
    }
    
    private suspend fun initializeLocation() {
        if (locationProvider.hasLocationPermission()) {
            currentLocation = locationProvider.getCurrentLocation()
            if (currentLocation != null) {
                loadDeals()
            }
        }
    }
    
    fun updateCategory(category: DealCategory?) {
        selectedCategory = category
        loadDeals()
    }
    
    fun updateRadius(radiusMiles: Int) {
        selectedRadius = radiusMiles
        loadDeals()
    }
    
    fun refreshDeals() {
        loadDeals()
    }
    
    private fun loadDeals() {
        val location = currentLocation ?: return
        
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            
            try {
                val deals = dealApi.fetchNearbyDeals(
                    lat = location.lat,
                    lng = location.lng,
                    radiusMiles = selectedRadius.toDouble(),
                    category = selectedCategory
                )
                
                uiState = uiState.copy(
                    deals = deals,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load deals"
                )
            }
        }
    }
    
    private fun handleRealtimeChange(change: DealChange) {
        val currentDeals = uiState.deals.toMutableList()
        val location = currentLocation ?: return
        
        when (change.type) {
            "INSERT" -> {
                val dealWithVendor = change.deal
                val vendor = dealWithVendor.vendor
                
                // Check if the new deal is within our current radius and category filter
                if (isDealRelevant(dealWithVendor, location, selectedRadius.toDouble(), selectedCategory)) {
                    // Add to the beginning of the list
                    currentDeals.add(0, dealWithVendor)
                    uiState = uiState.copy(deals = currentDeals)
                }
            }
            "UPDATE" -> {
                val dealWithVendor = change.deal
                val index = currentDeals.indexOfFirst { it.deal.id == dealWithVendor.deal.id }
                
                if (index != -1) {
                    if (isDealRelevant(dealWithVendor, location, selectedRadius.toDouble(), selectedCategory)) {
                        // Update existing deal
                        currentDeals[index] = dealWithVendor
                    } else {
                        // Deal no longer relevant, remove it
                        currentDeals.removeAt(index)
                    }
                    uiState = uiState.copy(deals = currentDeals)
                }
            }
            "DELETE" -> {
                val index = currentDeals.indexOfFirst { it.deal.id == change.deal.id }
                if (index != -1) {
                    currentDeals.removeAt(index)
                    uiState = uiState.copy(deals = currentDeals)
                }
            }
        }
    }
    
    private fun isDealRelevant(
        dealWithVendor: DealWithVendor,
        location: Location,
        radiusMiles: Double,
        category: DealCategory?
    ): Boolean {
        val deal = dealWithVendor.deal
        val vendor = dealWithVendor.vendor
        
        // Check if deal is active and not expired
        if (deal.status != DealStatus.ACTIVE || TimeUtils.isExpired(deal.expiresAt)) {
            return false
        }
        
        // Check category filter
        if (category != null && deal.category != category) {
            return false
        }
        
        // Check distance
        if (vendor.lat == null || vendor.lng == null) {
            return false
        }
        
        val distance = GeoUtils.calculateDistance(
            location.lat, location.lng,
            vendor.lat, vendor.lng
        )
        
        return distance <= radiusMiles
    }
    
    fun onDealClick(dealId: String) {
        // Navigate to deal detail
        // This will be handled by the navigation system
    }
    
    fun onVendorClick(vendorId: String) {
        // Navigate to vendor profile
        // This will be handled by the navigation system
    }
    
    fun clearError() {
        uiState = uiState.copy(error = null)
    }
}

data class FeedUiState(
    val deals: List<DealWithVendor> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)