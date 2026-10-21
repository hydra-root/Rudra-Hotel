package com.teasupplier.app.feature.reports.daily

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.teasupplier.app.utils.PdfGenerator
import com.teasupplier.app.utils.printPdf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReportScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPdfViewer: (String) -> Unit,
    viewModel: DailyReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.date)
    var showSortMenu by remember { mutableStateOf(false) }

    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val dateStr = dateFormatter.format(Date(uiState.date))

    val totalQty = uiState.filteredItems.sumOf { it.sale.quantity }
    val totalAmount = uiState.filteredItems.sumOf { it.sale.amount }

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
                    title = { Text("Daily Report") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sort by Name") },
                                onClick = { viewModel.updateSortOption(SortOption.NAME); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Quantity") },
                                onClick = { viewModel.updateSortOption(SortOption.QUANTITY); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Amount") },
                                onClick = { viewModel.updateSortOption(SortOption.AMOUNT); showSortMenu = false }
                            )
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Customers: ${uiState.filteredItems.size}", fontWeight = FontWeight.Bold)
                            Text("Total Qty: $totalQty", fontWeight = FontWeight.Bold)
                            Text("Total Amt: ₹$totalAmount", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    val file = PdfGenerator.generateDailyReportPdf(context, dateStr, uiState.filteredItems, totalQty, totalAmount)
                                    if (file != null) viewModel.onPdfGenerated(file)
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                enabled = uiState.filteredItems.isNotEmpty()
                            ) {
                                Text("Generate PDF")
                            }
                            
                            if (uiState.generatedPdf != null) {
                                OutlinedButton(
                                    onClick = { printPdf(context, uiState.generatedPdf!!, "DailyReport_$dateStr") },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = "Date", modifier = Modifier.padding(end = 8.dp))
                        Text(dateStr)
                    }
                }
                
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search customer...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                if (uiState.filteredItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No records found for this date.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(uiState.filteredItems, key = { it.sale.id }) { row ->
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
                                        Text(row.customer.shopName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(row.sale.productName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${row.sale.quantity}", fontWeight = FontWeight.Bold)
                                        Text("Qty", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("₹${row.sale.rate}", fontWeight = FontWeight.Bold)
                                        Text("Rate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                        Text("₹${row.sale.amount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("Amount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let { viewModel.selectDate(it) }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
