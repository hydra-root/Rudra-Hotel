package com.teasupplier.app.feature.ledger

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Payment Ledger UI for recording customer cash/UPI payments, displaying live balance
 * statements, and managing payment history logs.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teasupplier.app.data.local.entity.CustomerEntity
import com.teasupplier.app.utils.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    onNavigateBack: () -> Unit,
    viewModel: LedgerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val customers by viewModel.customers.collectAsState()
    
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    com.teasupplier.app.core.ui.theme.GlassBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Customer Ledger") },
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
        },
        floatingActionButton = {
            if (uiState.selectedCustomer != null) {
                FloatingActionButton(onClick = { showPaymentDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Payment")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Customer Selector
            ExposedDropdownMenuBox(
                expanded = customerDropdownExpanded,
                onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = uiState.selectedCustomer?.shopName ?: "Select Customer",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Customer") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = customerDropdownExpanded,
                    onDismissRequest = { customerDropdownExpanded = false }
                ) {
                    customers.forEach { customer ->
                        DropdownMenuItem(
                            text = { Text("${customer.shopName} - ${customer.balance.formatCurrency()}") },
                            onClick = {
                                viewModel.selectCustomer(customer)
                                customerDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Balance Summary
            uiState.selectedCustomer?.let { customer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Current Balance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = customer.balance.formatCurrency(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (customer.balance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val totalSales = uiState.ledgerEntries.filterIsInstance<LedgerEntry.Sale>().sumOf { it.amount }
                            val totalPayments = uiState.ledgerEntries.filterIsInstance<LedgerEntry.Payment>().sumOf { it.amount }
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Sales", style = MaterialTheme.typography.bodySmall)
                                Text(totalSales.formatCurrency(), fontWeight = FontWeight.Bold, color = Color(0xFFE57373))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Paid", style = MaterialTheme.typography.bodySmall)
                                Text(totalPayments.formatCurrency(), fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Ledger List
            if (uiState.selectedCustomer != null) {
                if (uiState.ledgerEntries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No transactions found.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.ledgerEntries) { entry ->
                            LedgerItemRow(entry)
                        }
                    }
                }
            }
        }
    }
    }



    if (showPaymentDialog) {
        AddPaymentDialog(
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, desc ->
                viewModel.addPayment(amount, desc)
                showPaymentDialog = false
            }
        )
    }
}

@Composable
fun LedgerItemRow(entry: LedgerEntry) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(entry.timestamp))
    
    val (title, typeColor, sign) = when (entry) {
        is LedgerEntry.Sale -> Triple("${entry.sale.productName} Supply (${if (entry.sale.quantity % 1.0 == 0.0) entry.sale.quantity.toInt() else entry.sale.quantity} qty)", Color(0xFFE57373), "+")
        is LedgerEntry.Payment -> Triple("Payment: ${entry.payment.mode}", Color(0xFF81C784), "-")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(text = dateStr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$sign ${entry.amount.formatCurrency().removePrefix("₹ ").removePrefix("-₹ ")}",
                    color = typeColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Bal: ${entry.runningBalance.formatCurrency()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AddPaymentDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("Cash") }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        title = { Text("Add Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Payment Mode / Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Quick Select Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf("Cash", "UPI", "Bank")
                    modes.forEach { mode ->
                        FilterChip(
                            selected = description == mode,
                            onClick = { description = mode },
                            label = { Text(mode) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    onConfirm(amt, description)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
