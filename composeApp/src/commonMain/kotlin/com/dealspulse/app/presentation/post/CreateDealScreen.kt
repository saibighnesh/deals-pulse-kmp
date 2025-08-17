package com.dealspulse.app.presentation.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.dealspulse.app.location.LocationProvider
import com.dealspulse.app.model.DealCategory
import com.dealspulse.app.util.TimeUtils

class CreateDealScreen(
    private val locationProvider: LocationProvider
) : Screen {
    
    override val key: ScreenKey = uniqueScreenKey
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = remember { CreateDealScreenModel(locationProvider = locationProvider) }
        val uiState by screenModel.uiState.collectAsState()
        
        // Handle success navigation
        LaunchedEffect(uiState.isSuccess) {
            if (uiState.isSuccess) {
                navigator.pop()
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // Top Bar
            TopAppBar(
                title = { Text("Create Deal") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        TextButton(
                            onClick = { screenModel.createDeal() },
                            enabled = !uiState.isLoading
                        ) {
                            Text(
                                "POST",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
            
            // Error handling
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    backgroundColor = MaterialTheme.colors.error,
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.body2
                        )
                        IconButton(onClick = screenModel::clearError) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White)
                        }
                    }
                }
            }
            
            // Form content
            if (!uiState.isAuthenticated) {
                NotAuthenticatedState()
            } else if (!uiState.isVendor) {
                NotVendorState()
            } else {
                CreateDealForm(
                    uiState = uiState,
                    onTitleChange = screenModel::updateTitle,
                    onDescriptionChange = screenModel::updateDescription,
                    onCategoryChange = screenModel::updateCategory,
                    onPriceChange = screenModel::updatePrice,
                    onExpirationHoursChange = screenModel::updateExpirationHours,
                    onExpirationMinutesChange = screenModel::updateExpirationMinutes,
                    onImageSelected = screenModel::selectImage,
                    onImageRemoved = screenModel::removeImage
                )
            }
        }
    }
}

@Composable
private fun NotAuthenticatedState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sign In Required",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please sign in to create deals",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* Navigate to sign in */ }) {
                Text("Sign In")
            }
        }
    }
}

@Composable
private fun NotVendorState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Vendor Account Required",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Only vendor accounts can create deals",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* Navigate to upgrade account */ }) {
                Text("Upgrade Account")
            }
        }
    }
}

@Composable
private fun CreateDealForm(
    uiState: CreateDealUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (DealCategory) -> Unit,
    onPriceChange: (String) -> Unit,
    onExpirationHoursChange: (Int) -> Unit,
    onExpirationMinutesChange: (Int) -> Unit,
    onImageSelected: (ByteArray, String) -> Unit,
    onImageRemoved: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        OutlinedTextField(
            value = uiState.title,
            onValueChange = onTitleChange,
            label = { Text("Deal Title") },
            placeholder = { Text("e.g., 50% off all drinks") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
        )
        
        // Description
        OutlinedTextField(
            value = uiState.description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            placeholder = { Text("Describe your deal...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 4
        )
        
        // Category Selection
        CategorySelection(
            selectedCategory = uiState.category,
            onCategorySelected = onCategoryChange
        )
        
        // Price
        OutlinedTextField(
            value = uiState.price,
            onValueChange = onPriceChange,
            label = { Text("Price") },
            placeholder = { Text("e.g., $9.99 or 50% off") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
        
        // Expiration Time
        ExpirationTimeSelector(
            hours = uiState.expirationHours,
            minutes = uiState.expirationMinutes,
            onHoursChange = onExpirationHoursChange,
            onMinutesChange = onExpirationMinutesChange
        )
        
        // Time preview
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Deal will expire in:",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = TimeUtils.formatTimeRemaining(uiState.expiresAt),
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Image Upload (placeholder)
        ImageUploadSection(
            selectedImageName = uiState.selectedImageName,
            onImageSelected = onImageSelected,
            onImageRemoved = onImageRemoved
        )
        
        // Location info
        uiState.currentLocation?.let { location ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color.Green.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Green
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Location detected",
                        style = MaterialTheme.typography.body2,
                        color = Color.Green
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun CategorySelection(
    selectedCategory: DealCategory,
    onCategorySelected: (DealCategory) -> Unit
) {
    Column {
        Text(
            text = "Category",
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DealCategory.entries.filter { it != DealCategory.OTHER }) { category ->
                Surface(
                    modifier = Modifier
                        .clickable { onCategorySelected(category) }
                        .clip(RoundedCornerShape(16.dp)),
                    color = if (category == selectedCategory) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
                    contentColor = if (category == selectedCategory) Color.White else MaterialTheme.colors.onSurface,
                    elevation = if (category == selectedCategory) 4.dp else 1.dp
                ) {
                    Text(
                        text = category.displayName,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.body2,
                        fontWeight = if (category == selectedCategory) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpirationTimeSelector(
    hours: Int,
    minutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit
) {
    Column {
        Text(
            text = "Expires In",
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hours
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hours",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf(0, 1, 2, 4, 6, 12, 24)) { hour ->
                        TimeChip(
                            value = hour,
                            isSelected = hour == hours,
                            onClick = { onHoursChange(hour) },
                            suffix = "h"
                        )
                    }
                }
            }
            
            // Minutes
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Minutes",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf(0, 15, 30, 45)) { minute ->
                        TimeChip(
                            value = minute,
                            isSelected = minute == minutes,
                            onClick = { onMinutesChange(minute) },
                            suffix = "m"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeChip(
    value: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    suffix: String
) {
    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(8.dp)),
        color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
        contentColor = if (isSelected) Color.White else MaterialTheme.colors.onSurface,
        elevation = if (isSelected) 2.dp else 1.dp
    ) {
        Text(
            text = "$value$suffix",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.caption,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ImageUploadSection(
    selectedImageName: String?,
    onImageSelected: (ByteArray, String) -> Unit,
    onImageRemoved: () -> Unit
) {
    Column {
        Text(
            text = "Photo (Optional)",
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (selectedImageName != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color.Green.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Green
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedImageName,
                            style = MaterialTheme.typography.body2,
                            color = Color.Green
                        )
                    }
                    
                    IconButton(onClick = onImageRemoved) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove image",
                            tint = Color.Red
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* Open image picker */ },
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to add photo",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}