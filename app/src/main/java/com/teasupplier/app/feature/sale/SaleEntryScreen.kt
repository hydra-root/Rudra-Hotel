package com.teasupplier.app.feature.sale

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teasupplier.app.data.local.entity.CustomerEntity
import com.teasupplier.app.utils.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleEntryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBulkOrder: () -> Unit = {},
    viewModel: SaleEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allowEditing by viewModel.allowEditing.collectAsState()
    val editLock = !allowEditing
    val messageState by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog state for adding a new entry
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }

    // Dialog state for editing an existing entry
    var showEditDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<SaleEntryRow?>(null) }

    // Delete confirm
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deletingEntry by remember { mutableStateOf<SaleEntryRow?>(null) }

    LaunchedEffect(messageState) {
        messageState?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    com.teasupplier.app.core.ui.theme.GlassBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Daily Sale Entry") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        FilledTonalButton(
                            onClick = onNavigateToBulkOrder,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Bulk", modifier = Modifier.size(16.dp).padding(end = 4.dp))
                            Text("All Orders (Bulk)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            var showDatePicker by remember { mutableStateOf(false) }
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.selectedDate)

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { viewModel.onDateSelected(it) }
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) { DatePicker(state = datePickerState) }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // ─── Date Selector ───
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = {
                            viewModel.onDateSelected(uiState.selectedDate - 86400000)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Day")
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(
                                onClick = { showDatePicker = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                                    .format(java.util.Date(uiState.selectedDate))
                                Icon(Icons.Default.DateRange, contentDescription = "Date", modifier = Modifier.padding(end = 8.dp))
                                Text(dateStr, fontWeight = FontWeight.Bold)
                            }
                            
                            if (uiState.entries.isEmpty() && !editLock) {
                                TextButton(
                                    onClick = { viewModel.repeatYesterday() },
                                    modifier = Modifier.padding(top = 4.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    Text("↻ Repeat Yesterday", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        
                        IconButton(onClick = {
                            viewModel.onDateSelected(uiState.selectedDate + 86400000)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Day")
                        }
                    }
                }

                // ─── Quick Banner to All Customer Orders ───
                item {
                    Card(
                        onClick = onNavigateToBulkOrder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Fill All Customer Orders At Once", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("सर्व ग्राहकांची ऑर्डर एकाच पानावर भरा", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // ─── Customer List (tap to add entry) ───
                item {
                    Text(
                        text = "Tap customer to add entry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(uiState.customers, key = { "customer_${it.id}" }) { customer ->
                    CustomerTapCard(
                        customer = customer,
                        onClick = {
                            selectedCustomer = customer
                            showAddDialog = true
                        }
                    )
                }

                // ─── Today's Entries (shown below customers) ───
                if (uiState.entries.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    
                    item {
                        val totalQty = uiState.entries.sumOf { it.sale.quantity }
                        val totalAmount = uiState.entries.sumOf { it.sale.amount }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today's Entries (${uiState.entries.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${totalQty.toInt()} qty • ${totalAmount.formatCurrency()}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (editLock) {
                            Text(
                                text = "🔒 Edit lock ON",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    items(uiState.entries, key = { "sale_${it.sale.id}" }) { entry ->
                        SavedEntryCard(
                            entry = entry,
                            editLocked = editLock,
                            onEditClick = {
                                editingEntry = entry
                                showEditDialog = true
                            },
                            onDeleteClick = {
                                deletingEntry = entry
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            }
        }
    }

    // ─── Add Entry Dialog ───
    if (showAddDialog) {
        selectedCustomer?.let { customer ->
            AddEntryDialog(
                title = "Add Entry",
                customer = customer,
                products = uiState.products,
                customerRates = uiState.customerRates,
                onConfirm = { product, qty ->
                    viewModel.addEntry(customer.id, product, qty)
                    showAddDialog = false
                    selectedCustomer = null
                },
                onDismiss = {
                    showAddDialog = false
                    selectedCustomer = null
                }
            )
        }
    }

    // ─── Edit Entry Dialog ───
    if (showEditDialog) {
        editingEntry?.let { entry ->
            val sale = entry.sale
            QuantityDialog(
                title = "Edit Entry #${entry.displayIndex}",
                customerName = entry.customer?.shopName ?: "Unknown",
                rate = sale.rate,
                initialQuantity = if (sale.quantity % 1.0 == 0.0) sale.quantity.toInt().toString() else sale.quantity.toString(),
                onConfirm = { qty ->
                    viewModel.editEntry(sale.id, qty)
                    showEditDialog = false
                    editingEntry = null
                },
                onDismiss = {
                    showEditDialog = false
                    editingEntry = null
                }
            )
        }
    }

    // ─── Delete Confirmation ───
    if (showDeleteConfirm) {
        deletingEntry?.let { entry ->
            AlertDialog(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Entry") },
                text = { Text("Delete entry #${entry.displayIndex} for ${entry.customer?.shopName ?: "Unknown"}?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteEntry(entry.sale.id)
                        showDeleteConfirm = false
                        deletingEntry = null
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        deletingEntry = null
                    }) { Text("Cancel") }
                }
            )
        }
    }
}

// ─── Add Entry Dialog ───
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryDialog(
    title: String,
    customer: CustomerEntity,
    products: List<com.teasupplier.app.data.local.entity.ProductEntity>,
    customerRates: List<com.teasupplier.app.data.local.entity.CustomerProductRateEntity>,
    onConfirm: (com.teasupplier.app.data.local.entity.ProductEntity, Double) -> Unit,
    onDismiss: () -> Unit
) {
    if (products.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("No Products") },
            text = { Text("Please add at least one product in Settings -> Manage Products.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
        )
        return
    }

    var selectedProduct by remember { mutableStateOf(products.first()) }
    var qtyText by remember { mutableStateOf("") }
    var productDropdownExpanded by remember { mutableStateOf(false) }

    val rate = customerRates.find { it.customerId == customer.id && it.productId == selectedProduct.id }?.rate ?: selectedProduct.defaultRate
    val qty = qtyText.toDoubleOrNull() ?: 0.0
    val amount = qty * rate
    val rateStr = if (rate % 1.0 == 0.0) rate.toInt().toString() else rate.toString()

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = customer.shopName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                ExposedDropdownMenuBox(
                    expanded = productDropdownExpanded,
                    onExpandedChange = { productDropdownExpanded = !productDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedProduct.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Product") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = productDropdownExpanded,
                        onDismissRequest = { productDropdownExpanded = false }
                    ) {
                        products.forEach { product ->
                            DropdownMenuItem(
                                text = { Text(product.name) },
                                onClick = {
                                    selectedProduct = product
                                    productDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Rate: ₹$rateStr per unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                )
                if (qty > 0) {
                    Text(
                        text = "Amount: ${amount.formatCurrency()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedProduct, qty) },
                enabled = qty > 0
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ─── Quantity Input Dialog (For Edits) ───
@Composable
fun QuantityDialog(
    title: String,
    customerName: String,
    rate: Double,
    initialQuantity: String,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var qtyText by remember { mutableStateOf(initialQuantity) }
    val qty = qtyText.toDoubleOrNull() ?: 0.0
    val amount = qty * rate
    val rateStr = if (rate % 1.0 == 0.0) rate.toInt().toString() else rate.toString()

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = customerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Rate: ₹$rateStr per unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
                if (qty > 0) {
                    Text(
                        text = "Amount: ${amount.formatCurrency()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(qty) },
                enabled = qty > 0
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ─── Customer Tap Card (simple list item) ───
@Composable
fun CustomerTapCard(
    customer: CustomerEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.shopName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (customer.name.isNotBlank()) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Saved Entry Card ───
@Composable
fun SavedEntryCard(
    entry: SaleEntryRow,
    editLocked: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val sale = entry.sale
    val customerName = entry.customer?.shopName ?: "Unknown"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sequence badge
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#${entry.displayIndex}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                val qtyStr = if (sale.quantity % 1.0 == 0.0) sale.quantity.toInt().toString() else sale.quantity.toString()
                val rateStr = if (sale.rate % 1.0 == 0.0) sale.rate.toInt().toString() else sale.rate.toString()
                Text(
                    text = "${sale.productName} — $qtyStr × ₹$rateStr = ${sale.amount.formatCurrency()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!editLocked) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }
    }
}
