package com.teasupplier.app.feature.customer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCustomerScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditCustomerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    com.teasupplier.app.core.ui.theme.GlassBackground {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Customer" else "Add Customer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.shopName,
                onValueChange = { viewModel.onEvent(AddEditCustomerEvent.ShopNameChanged(it)) },
                label = { Text("Shop Name *") },
                isError = uiState.shopNameError != null,
                supportingText = uiState.shopNameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.onEvent(AddEditCustomerEvent.NameChanged(it)) },
                label = { Text("Owner Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.mobile,
                onValueChange = { viewModel.onEvent(AddEditCustomerEvent.MobileChanged(it)) },
                label = { Text("Mobile Number") },
                isError = uiState.mobileError != null,
                supportingText = uiState.mobileError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.address,
                onValueChange = { viewModel.onEvent(AddEditCustomerEvent.AddressChanged(it)) },
                label = { Text("Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.area,
                onValueChange = { viewModel.onEvent(AddEditCustomerEvent.AreaChanged(it)) },
                label = { Text("Area") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            // Custom Rates for Products
            if (uiState.productRates.isNotEmpty()) {
                Text(
                    text = "Product Rates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                uiState.productRates.forEach { rateInput ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = rateInput.product.name, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Global Default: ₹${rateInput.product.defaultRate}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            OutlinedTextField(
                                value = rateInput.rateText,
                                onValueChange = { 
                                    viewModel.onEvent(AddEditCustomerEvent.ProductRateChanged(rateInput.product.id, it))
                                },
                                label = { Text("Rate (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = uiState.openingBalance,
                onValueChange = { viewModel.onEvent(AddEditCustomerEvent.OpeningBalanceChanged(it)) },
                label = { Text("Opening Balance (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Status toggle (only in edit mode)
            if (uiState.isEditMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.isActive) "Active Customer" else "Inactive Customer",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (uiState.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Switch(
                        checked = uiState.isActive,
                        onCheckedChange = { viewModel.onEvent(AddEditCustomerEvent.ToggleStatus) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.onEvent(AddEditCustomerEvent.SaveCustomer) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(if (uiState.isEditMode) "Update Customer" else "Save Customer")
            }
        }
    }
    }
}
