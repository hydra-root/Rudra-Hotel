package com.teasupplier.app.feature.billing

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Customer Billing screen providing date-range bill generation, master billing,
 * individual customer statements, UPI payment QR code embedding, and direct printing/sharing.
 */

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teasupplier.app.utils.PdfGenerator
import com.teasupplier.app.utils.printPdf

import com.teasupplier.app.utils.formatCurrency
import androidx.compose.ui.text.font.FontWeight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPdfViewer: (String) -> Unit,
    viewModel: BillingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val businessProfile by viewModel.businessProfile.collectAsState()
    val context = LocalContext.current
    
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showBillDatePicker by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.startDate)
    val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.endDate)
    val billDatePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.billDate)

    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val startStr = dateFormatter.format(Date(uiState.startDate))
    val endStr = dateFormatter.format(Date(uiState.endDate))
    val billDateStr = dateFormatter.format(Date(uiState.billDate))
    val billingPeriodStr = "$startStr to $endStr"

    LaunchedEffect(uiState.generatedPdf) {
        uiState.generatedPdf?.let { file ->
            val encodedPath = android.net.Uri.encode(file.absolutePath)
            onNavigateToPdfViewer(encodedPath)
            // Reset state to avoid triggering again on back press
            viewModel.onPdfGenerated(null)
        }
    }

    com.teasupplier.app.core.ui.theme.GlassBackground {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Generate Bill") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Customer Dropdown
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
                            text = { Text(customer.shopName) },
                            onClick = {
                                viewModel.selectCustomer(customer)
                                customerDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Date Range Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Billing Period (कालावधी)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = "Start Date", modifier = Modifier.padding(end = 8.dp))
                        Text(startStr)
                    }
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = "End Date", modifier = Modifier.padding(end = 8.dp))
                        Text(endStr)
                    }
                }
            }

            // Bill Date Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Bill Date on Invoice (बिल दिनांक)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { showBillDatePicker = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Bill Date", modifier = Modifier.padding(end = 8.dp))
                    Text("Bill Date: $billDateStr", fontWeight = FontWeight.SemiBold)
                }
            }

            // Summary
            uiState.selectedCustomer?.let { customer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sales in period: ${uiState.salesForMonth.size}", style = MaterialTheme.typography.bodyLarge)
                            Text("Total: ${uiState.salesForMonth.sumOf { it.amount }.formatCurrency()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Current Outstanding:", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = customer.balance.formatCurrency(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (customer.balance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Summary Bill (existing)
                Button(
                    onClick = {
                        val file = PdfGenerator.generateBill(
                            context = context,
                            customer = customer,
                            sales = uiState.salesForMonth,
                            billingPeriodStr = billingPeriodStr,
                            shopNameMarathi = businessProfile.hotelNameMarathi,
                            shopNameEnglish = businessProfile.hotelNameEnglish,
                            shopAddress = businessProfile.address,
                            shopPhone = businessProfile.phone,
                            billDate = uiState.billDate,
                            upiId = businessProfile.upiId
                        )
                        if (file != null) {
                            viewModel.onPdfGenerated(file)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = uiState.salesForMonth.isNotEmpty()
                ) {
                    Text("Generate Summary Bill")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Detailed Bill (Morning / Evening breakdown)
                OutlinedButton(
                    onClick = {
                        val file = PdfGenerator.generateDetailedBill(
                            context = context,
                            customer = customer,
                            sales = uiState.salesForMonth,
                            billingPeriodStr = billingPeriodStr,
                            shopNameMarathi = businessProfile.hotelNameMarathi,
                            shopNameEnglish = businessProfile.hotelNameEnglish,
                            shopAddress = businessProfile.address,
                            shopPhone = businessProfile.phone,
                            billDate = uiState.billDate,
                            upiId = businessProfile.upiId
                        )
                        if (file != null) {
                            viewModel.onPdfGenerated(file)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = uiState.salesForMonth.isNotEmpty()
                ) {
                    Text("Generate Detailed Bill (Morning / Evening)")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    viewModel.generateMasterBill(context, billingPeriodStr)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Generate Master Bill (All Customers)")
            }
            
            // Actions after Generation
            uiState.generatedPdf?.let { pdf ->
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { printPdf(context, pdf, "Bill_${uiState.selectedCustomer?.shopName}") },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("Print")
                    }
                    Button(
                        onClick = { com.teasupplier.app.utils.sharePdf(context, pdf) },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("Share")
                    }
                }
            }
        }
    }
    }

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            confirmButton = {
                TextButton(onClick = {
                    showStartDatePicker = false
                    startDatePickerState.selectedDateMillis?.let {
                        viewModel.selectDateRange(it, uiState.endDate)
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            confirmButton = {
                TextButton(onClick = {
                    showEndDatePicker = false
                    endDatePickerState.selectedDateMillis?.let {
                        viewModel.selectDateRange(uiState.startDate, it)
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

    if (showBillDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showBillDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            confirmButton = {
                TextButton(onClick = {
                    showBillDatePicker = false
                    billDatePickerState.selectedDateMillis?.let {
                        viewModel.selectBillDate(it)
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBillDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = billDatePickerState)
        }
    }
}
