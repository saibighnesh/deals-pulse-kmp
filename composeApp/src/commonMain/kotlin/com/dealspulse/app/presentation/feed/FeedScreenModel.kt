package com.dealspulse.app.presentation.feed

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.dealspulse.app.data.DealApi
import com.dealspulse.app.data.DealChange
import com.dealspulse.app.data.RealtimeApi
import com.dealspulse.app.location.Location
import com.dealspulse.app.location.LocationProvider
import com.dealspulse.app.model.Deal
import com.dealspulse.app.model.DealCategory
import com.dealspulse.app.util.GeoUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FeedScreenModel(
    private val dealApi: DealApi = DealApi(),
    private val realtimeApi: RealtimeApi = RealtimeApi(),
    private val locationProvider: LocationProvider
) : ScreenModel {
    
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
    
    private var currentLocation: Location? = null
    
    init {
        // Start location updates
        screenModelScope.launch {
            locationProvider.getLocationUpdates()
                .collect { location ->
                    currentLocation = location
                    if (_uiState.value.deals.isEmpty()) {
                        loadDeals()
                    }
                }
        }
        
        // Initialize location
        getCurrentLocation()
    }
    
    fun loadDeals() {
        val location = currentLocation ?: return
        val state = _uiState.value
        
        _uiState.value = state.copy(isLoading = true, error = null)
        
        screenModelScope.launch {
            try {
                val deals = dealApi.fetchDealsNearby(
                    lat = location.latitude,
                    lng = location.longitude,
                    radiusMiles = state.radiusMiles.toDouble(),
                    category = state.selectedCategory?.name?.lowercase(),
                    limit = 50
                )
                
                val dealsWithDistance = deals.map { deal ->
                    val distance = GeoUtils.calculateDistance(
                        location.latitude, location.longitude,
                        deal.lat, deal.lng
                    )
                    deal to distance
                }.sortedWith(
                    compareBy<Pair<Deal, Double>> { it.first.expiresAt }
                        .thenBy { it.second }
                )
                
                _uiState.value = state.copy(
                    deals = dealsWithDistance,
                    isLoading = false,
                    lastUpdated = System.currentTimeMillis()
                )
                
                // Start realtime updates
                startRealtimeUpdates(location)
                
            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isLoading = false,
                    error = "Failed to load deals: ${e.message}"
                )
            }
        }
    }
    
    fun refreshDeals() {
        loadDeals()
    }
    
    fun updateRadiusMiles(radius: Int) {
        _uiState.value = _uiState.value.copy(radiusMiles = radius)
        loadDeals()
    }
    
    fun updateSelectedCategory(category: DealCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        loadDeals()
    }
    
    fun getCurrentLocation() {
        screenModelScope.launch {
            try {
                val hasPermission = locationProvider.hasLocationPermission()
                if (!hasPermission) {
                    val granted = locationProvider.requestLocationPermission()
                    if (!granted) {
                        _uiState.value = _uiState.value.copy(
                            error = "Location permission is required to find nearby deals"
                        )
                        return@launch
                    }
                }
                
                val location = locationProvider.getCurrentLocation()
                if (location != null) {
                    currentLocation = location
                    loadDeals()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Unable to get current location"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Location error: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private fun startRealtimeUpdates(location: Location) {
        screenModelScope.launch {
            realtimeApi.subscribeToDealsNearby(
                lat = location.latitude,
                lng = location.longitude,
                radiusMiles = _uiState.value.radiusMiles.toDouble()
            ).collect { change ->
                when (change) {
                    is DealChange.Insert -> {
                        addDealToList(change.deal, location)
                    }
                    is DealChange.Update -> {
                        updateDealInList(change.deal, location)
                    }
                    is DealChange.Delete -> {
                        removeDealFromList(change.dealId)
                    }
                    is DealChange.Unknown -> {
                        // Ignore unknown changes
                    }
                }
            }
        }
    }
    
    private fun addDealToList(deal: Deal, userLocation: Location) {
        if (!deal.isActive) return
        
        val currentState = _uiState.value
        val currentDeals = currentState.deals.toMutableList()
        
        // Check if deal already exists
        if (currentDeals.any { it.first.id == deal.id }) return
        
        val distance = GeoUtils.calculateDistance(
            userLocation.latitude, userLocation.longitude,
            deal.lat, deal.lng
        )
        
        // Add deal and re-sort
        currentDeals.add(deal to distance)
        val sortedDeals = currentDeals.sortedWith(
            compareBy<Pair<Deal, Double>> { it.first.expiresAt }
                .thenBy { it.second }
        )
        
        _uiState.value = currentState.copy(
            deals = sortedDeals,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    private fun updateDealInList(deal: Deal, userLocation: Location) {
        val currentState = _uiState.value
        val currentDeals = currentState.deals.toMutableList()
        
        val index = currentDeals.indexOfFirst { it.first.id == deal.id }
        if (index >= 0) {
            if (deal.isActive) {
                val distance = GeoUtils.calculateDistance(
                    userLocation.latitude, userLocation.longitude,
                    deal.lat, deal.lng
                )
                currentDeals[index] = deal to distance
                
                // Re-sort after update
                val sortedDeals = currentDeals.sortedWith(
                    compareBy<Pair<Deal, Double>> { it.first.expiresAt }
                        .thenBy { it.second }
                )
                
                _uiState.value = currentState.copy(
                    deals = sortedDeals,
                    lastUpdated = System.currentTimeMillis()
                )
            } else {
                // Remove inactive deal
                removeDealFromList(deal.id)
            }
        }
    }
    
    private fun removeDealFromList(dealId: String) {
        val currentState = _uiState.value
        val updatedDeals = currentState.deals.filter { it.first.id != dealId }
        
        _uiState.value = currentState.copy(
            deals = updatedDeals,
            lastUpdated = System.currentTimeMillis()
        )
    }
}

data class FeedUiState(
    val deals: List<Pair<Deal, Double>> = emptyList(), // Deal with distance
    val isLoading: Boolean = false,
    val error: String? = null,
    val radiusMiles: Int = 10,
    val selectedCategory: DealCategory? = null,
    val lastUpdated: Long = 0L
)