package com.dealspulse.app.presentation.post

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.dealspulse.app.data.DealApi
import com.dealspulse.app.data.ProfileApi
import com.dealspulse.app.data.StorageApi
import com.dealspulse.app.location.Location
import com.dealspulse.app.location.LocationProvider
import com.dealspulse.app.model.AccountType
import com.dealspulse.app.model.Deal
import com.dealspulse.app.model.DealCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class CreateDealScreenModel(
    private val dealApi: DealApi = DealApi(),
    private val profileApi: ProfileApi = ProfileApi(),
    private val storageApi: StorageApi = StorageApi(),
    private val locationProvider: LocationProvider
) : ScreenModel {
    
    private val _uiState = MutableStateFlow(CreateDealUiState())
    val uiState: StateFlow<CreateDealUiState> = _uiState.asStateFlow()
    
    private var currentLocation: Location? = null
    
    init {
        checkVendorStatus()
        getCurrentLocation()
    }
    
    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }
    
    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }
    
    fun updateCategory(category: DealCategory) {
        _uiState.value = _uiState.value.copy(category = category)
    }
    
    fun updatePrice(price: String) {
        _uiState.value = _uiState.value.copy(price = price)
    }
    
    fun updateExpirationHours(hours: Int) {
        val expiresAt = Clock.System.now() + hours.hours
        _uiState.value = _uiState.value.copy(
            expirationHours = hours,
            expiresAt = expiresAt
        )
    }
    
    fun updateExpirationMinutes(minutes: Int) {
        val currentHours = _uiState.value.expirationHours
        val totalMinutes = currentHours * 60 + minutes
        val expiresAt = Clock.System.now() + totalMinutes.minutes
        _uiState.value = _uiState.value.copy(
            expirationMinutes = minutes,
            expiresAt = expiresAt
        )
    }
    
    fun selectImage(imageData: ByteArray, fileName: String) {
        _uiState.value = _uiState.value.copy(
            selectedImageData = imageData,
            selectedImageName = fileName
        )
    }
    
    fun removeImage() {
        _uiState.value = _uiState.value.copy(
            selectedImageData = null,
            selectedImageName = null,
            uploadedImageUrl = null
        )
    }
    
    fun createDeal() {
        val state = _uiState.value
        val location = currentLocation
        
        if (!validateForm(state) || location == null) {
            return
        }
        
        _uiState.value = state.copy(isLoading = true, error = null)
        
        screenModelScope.launch {
            try {
                // Upload image if selected
                var imageUrl: String? = null
                if (state.selectedImageData != null && state.selectedImageName != null) {
                    imageUrl = storageApi.uploadDealImage(
                        fileName = state.selectedImageName,
                        imageData = state.selectedImageData
                    )
                    
                    if (imageUrl == null) {
                        _uiState.value = state.copy(
                            isLoading = false,
                            error = "Failed to upload image"
                        )
                        return@launch
                    }
                }
                
                // Create deal
                val deal = dealApi.createDeal(
                    title = state.title,
                    description = state.description,
                    category = state.category.name,
                    price = state.price,
                    imageUrl = imageUrl,
                    lat = location.latitude,
                    lng = location.longitude,
                    expiresAt = state.expiresAt
                )
                
                _uiState.value = state.copy(
                    isLoading = false,
                    isSuccess = true,
                    createdDeal = deal
                )
                
            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isLoading = false,
                    error = "Failed to create deal: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun resetForm() {
        _uiState.value = CreateDealUiState(
            isVendor = _uiState.value.isVendor,
            hasLocationPermission = _uiState.value.hasLocationPermission
        )
        getCurrentLocation()
    }
    
    private fun checkVendorStatus() {
        screenModelScope.launch {
            try {
                val profile = profileApi.getCurrentProfile()
                val isVendor = profile?.accountType == AccountType.VENDOR
                
                _uiState.value = _uiState.value.copy(
                    isVendor = isVendor,
                    isAuthenticated = profile != null
                )
                
                if (!isVendor && profile != null) {
                    _uiState.value = _uiState.value.copy(
                        error = "Only vendors can create deals. Please upgrade your account."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = false,
                    error = "Please sign in to create deals"
                )
            }
        }
    }
    
    private fun getCurrentLocation() {
        screenModelScope.launch {
            try {
                val hasPermission = locationProvider.hasLocationPermission()
                if (!hasPermission) {
                    val granted = locationProvider.requestLocationPermission()
                    _uiState.value = _uiState.value.copy(hasLocationPermission = granted)
                    if (!granted) return@launch
                }
                
                val location = locationProvider.getCurrentLocation()
                currentLocation = location
                _uiState.value = _uiState.value.copy(
                    hasLocationPermission = true,
                    currentLocation = location
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    hasLocationPermission = false,
                    error = "Location access required to create deals"
                )
            }
        }
    }
    
    private fun validateForm(state: CreateDealUiState): Boolean {
        val errors = mutableListOf<String>()
        
        if (state.title.isBlank()) {
            errors.add("Title is required")
        }
        if (state.description.isBlank()) {
            errors.add("Description is required")
        }
        if (state.price.isBlank()) {
            errors.add("Price is required")
        }
        if (state.expirationHours == 0 && state.expirationMinutes == 0) {
            errors.add("Expiration time must be set")
        }
        if (!state.isVendor) {
            errors.add("Only vendors can create deals")
        }
        if (!state.hasLocationPermission) {
            errors.add("Location permission required")
        }
        
        if (errors.isNotEmpty()) {
            _uiState.value = state.copy(error = errors.joinToString(", "))
            return false
        }
        
        return true
    }
}

data class CreateDealUiState(
    val title: String = "",
    val description: String = "",
    val category: DealCategory = DealCategory.FOOD,
    val price: String = "",
    val expirationHours: Int = 1,
    val expirationMinutes: Int = 0,
    val expiresAt: Instant = Clock.System.now() + 1.hours,
    val selectedImageData: ByteArray? = null,
    val selectedImageName: String? = null,
    val uploadedImageUrl: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val isVendor: Boolean = false,
    val isAuthenticated: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val currentLocation: Location? = null,
    val createdDeal: Deal? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CreateDealUiState

        if (title != other.title) return false
        if (description != other.description) return false
        if (category != other.category) return false
        if (price != other.price) return false
        if (expirationHours != other.expirationHours) return false
        if (expirationMinutes != other.expirationMinutes) return false
        if (expiresAt != other.expiresAt) return false
        if (selectedImageData != null) {
            if (other.selectedImageData == null) return false
            if (!selectedImageData.contentEquals(other.selectedImageData)) return false
        } else if (other.selectedImageData != null) return false
        if (selectedImageName != other.selectedImageName) return false
        if (uploadedImageUrl != other.uploadedImageUrl) return false
        if (isLoading != other.isLoading) return false
        if (isSuccess != other.isSuccess) return false
        if (error != other.error) return false
        if (isVendor != other.isVendor) return false
        if (isAuthenticated != other.isAuthenticated) return false
        if (hasLocationPermission != other.hasLocationPermission) return false
        if (currentLocation != other.currentLocation) return false
        if (createdDeal != other.createdDeal) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + price.hashCode()
        result = 31 * result + expirationHours
        result = 31 * result + expirationMinutes
        result = 31 * result + expiresAt.hashCode()
        result = 31 * result + (selectedImageData?.contentHashCode() ?: 0)
        result = 31 * result + (selectedImageName?.hashCode() ?: 0)
        result = 31 * result + (uploadedImageUrl?.hashCode() ?: 0)
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + isSuccess.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + isVendor.hashCode()
        result = 31 * result + isAuthenticated.hashCode()
        result = 31 * result + hasLocationPermission.hashCode()
        result = 31 * result + (currentLocation?.hashCode() ?: 0)
        result = 31 * result + (createdDeal?.hashCode() ?: 0)
        return result
    }
}