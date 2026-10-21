package com.teasupplier.app.feature.reports.customer

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.teasupplier.app.domain.usecase.TransactionType
import com.teasupplier.app.utils.PdfGenerator
import com.teasupplier.app.utils.printPdf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerReportScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPdfViewer: (String) -> Unit,
    viewModel: CustomerReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val context = LocalContext.current
    
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    val totalQty = uiState.items.filter { it.type == TransactionType.SALE }.sumOf { it.quantity ?: 0.0 }
    val totalAmount = uiState.items.filter { it.type == TransactionType.SALE }.sumOf { it.amount }
    val totalPaid = uiState.items.filter { it.type == TransactionType.PAYMENT }.sumOf { it.amount }
    val outstanding = totalAmount - totalPaid

    LaunchedEffect(uiState.generatedPdf) {
        uiState.generatedPdf?.let { file ->
            val encodedPath = android.net.Uri.encode(file.absolutePath)
            onNavigateToPdfViewer(encodedPath)
            viewModel.onPdfGenerated(null)
        }
    }

    com.teasupplier.app.core.ui.theme.GlassBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Customer Report") },
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
            bottomBar = {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Qty: $totalQty", fontWeight = FontWeight.Bold)
                            Text("Total Billed: ₹$totalAmount", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Paid: ₹$totalPaid", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                            Text("Outstanding: ₹$outstanding", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = {
                                    val file = PdfGenerator.generateCustomerStatementPdf(context, uiState.selectedCustomer!!, uiState.items, totalQty, totalAmount, totalPaid, outstanding)
                                    if (file != null) viewModel.onPdfGenerated(file)
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                enabled = uiState.selectedCustomer != null && uiState.items.isNotEmpty()
                            ) {
                                Text("Generate PDF")
                            }
                            if (uiState.generatedPdf != null) {
                                OutlinedButton(
                                    onClick = { printPdf(context, uiState.generatedPdf!!, "Statement_${uiState.selectedCustomer?.shopName}") },
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text("Print")
                                }
                                FilledTonalButton(
                                    onClick = { com.teasupplier.app.utils.sharePdf(context, uiState.generatedPdf!!) },
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text("Share")
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = customerDropdownExpanded,
                    onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.selectedCustomer?.shopName ?: "Select Customer",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = customerDropdownExpanded, onDismissRequest = { customerDropdownExpanded = false }) {
                        customers.forEach { customer ->
                            DropdownMenuItem(text = { Text(customer.shopName) }, onClick = { viewModel.selectCustomer(customer); customerDropdownExpanded = false })
                        }
                    }
                }
                
                if (uiState.selectedCustomer != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(uiState.selectedCustomer!!.name, fontWeight = FontWeight.Bold)
                            Text("Mobile: ${uiState.selectedCustomer!!.mobile}")
                            Text("Address: ${uiState.selectedCustomer!!.address}")
                        }
                    }
                }

                if (uiState.selectedCustomer == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Please select a customer.")
                    }
                } else if (uiState.items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No transactions found.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(uiState.items, key = { it.id }) { row ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(dateFormat.format(Date(row.date)), fontWeight = FontWeight.Bold)
                                        Text(if (row.type == TransactionType.SALE) "Tea Supply" else "Payment Received", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (row.type == TransactionType.SALE) {
                                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${row.quantity}", fontWeight = FontWeight.Bold)
                                            Text("Qty", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                            Text("₹${row.amount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text("Bill Amt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    } else {
                                        Column(modifier = Modifier.weight(2f), horizontalAlignment = Alignment.End) {
                                            Text("+ ₹${row.amount}", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                                            Text("Paid", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
