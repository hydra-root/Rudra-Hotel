package com.teasupplier.app.feature.reports.monthly

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.teasupplier.app.utils.PdfGenerator
import com.teasupplier.app.utils.printPdf
import java.text.DateFormatSymbols

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPdfViewer: (String) -> Unit,
    viewModel: MonthlyReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var monthDropdownExpanded by remember { mutableStateOf(false) }
    var yearDropdownExpanded by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val months = DateFormatSymbols().months.filter { it.isNotEmpty() }
    val years = (2020..2030).toList()

    val totalQty = uiState.filteredItems.sumOf { it.totalQuantity }
    val totalRevenue = uiState.filteredItems.sumOf { it.totalAmount }
    val totalPaid = uiState.filteredItems.sumOf { it.totalPaid }
    val totalOutstanding = uiState.filteredItems.sumOf { it.outstanding }

    val monthStr = "${months[uiState.monthIndex]} ${uiState.year}"

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
                    title = { Text("Monthly Report") },
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
                            DropdownMenuItem(text = { Text("Sort by Name") }, onClick = { viewModel.updateSortOption(MonthlySortOption.NAME); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Sort by Quantity") }, onClick = { viewModel.updateSortOption(MonthlySortOption.QUANTITY); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Sort by Amount") }, onClick = { viewModel.updateSortOption(MonthlySortOption.AMOUNT); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Sort by Outstanding") }, onClick = { viewModel.updateSortOption(MonthlySortOption.OUTSTANDING); showSortMenu = false })
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
                            Text("Revenue: ₹$totalRevenue", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Received: ₹$totalPaid", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                            Text("Pending: ₹$totalOutstanding", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = {
                                    val file = PdfGenerator.generateMonthlyReportPdf(context, monthStr, uiState.filteredItems, totalQty, totalRevenue, totalPaid, totalOutstanding)
                                    if (file != null) viewModel.onPdfGenerated(file)
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                enabled = uiState.filteredItems.isNotEmpty()
                            ) {
                                Text("Generate PDF")
                            }
                            if (uiState.generatedPdf != null) {
                                OutlinedButton(
                                    onClick = { printPdf(context, uiState.generatedPdf!!, "MonthlyReport_$monthStr") },
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = monthDropdownExpanded,
                        onExpandedChange = { monthDropdownExpanded = !monthDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = months[uiState.monthIndex],
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthDropdownExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = monthDropdownExpanded, onDismissRequest = { monthDropdownExpanded = false }) {
                            months.forEachIndexed { index, month ->
                                DropdownMenuItem(text = { Text(month) }, onClick = { viewModel.selectMonth(index); monthDropdownExpanded = false })
                            }
                        }
                    }
                    
                    ExposedDropdownMenuBox(
                        expanded = yearDropdownExpanded,
                        onExpandedChange = { yearDropdownExpanded = !yearDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = uiState.year.toString(),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearDropdownExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = yearDropdownExpanded, onDismissRequest = { yearDropdownExpanded = false }) {
                            years.forEach { year ->
                                DropdownMenuItem(text = { Text(year.toString()) }, onClick = { viewModel.selectYear(year); yearDropdownExpanded = false })
                            }
                        }
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
                        Text("No records found for this month.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(uiState.filteredItems, key = { it.customer.id }) { row ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(row.customer.shopName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("Qty", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${row.totalQuantity}", fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Text("Amount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("₹${row.totalAmount}", fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Text("Paid", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("₹${row.totalPaid}", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Pending", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("₹${row.outstanding}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
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
