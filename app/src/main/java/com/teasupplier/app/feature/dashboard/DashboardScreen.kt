package com.teasupplier.app.feature.dashboard

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Core Hotel Dashboard UI displaying dynamic dining table grid (1 to 20 tables),
 * live occupancy counters, quick table order pop-up bottom sheet, sandwich navigation menu,
 * and hotel revenue statistics.
 */

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teasupplier.app.core.ui.theme.GlassBackground
import com.teasupplier.app.core.ui.theme.KingFontFamily
import com.teasupplier.app.utils.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTableOrder: (Int) -> Unit,
    onNavigateToBulkOrder: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToReports: () -> Unit = {},
    onNavigateToBilling: () -> Unit = {},
    onNavigateToAddCustomer: () -> Unit = {},
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToExpense: () -> Unit = {},
    onNavigateToPdfViewer: (String) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    // ─── Table Quick Order Pop-Up Menu (ModalBottomSheet) ───
    if (uiState.selectedTableNumber != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val selectedTable = uiState.selectedTableNumber!!
        var showClearDialog by remember { mutableStateOf(false) }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear Table #$selectedTable?") },
                text = { Text("Are you sure you want to clear all active orders for Table #$selectedTable?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearDialog = false
                            viewModel.clearCurrentTable()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear Table")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        ModalBottomSheet(
            onDismissRequest = { viewModel.selectTable(null) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color(0xCC000000).copy(alpha = 0.55f),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: Table Number & Running Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Table #$selectedTable",
                            fontFamily = KingFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (uiState.selectedTableOrders.isNotEmpty()) Color(0xFFF59E0B) else Color(0xFF10B981)
                        ) {
                            Text(
                                text = if (uiState.selectedTableOrders.isNotEmpty()) "${uiState.selectedTableOrders.size} items" else "Free",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (uiState.selectedTableOrders.isNotEmpty()) {
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        IconButton(onClick = {
                            val t = uiState.selectedTableNumber ?: 1
                            viewModel.selectTable(null)
                            onNavigateToTableOrder(t)
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Full Screen",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.selectTable(null) }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                // ─── Fast Product Tap Grid (Tap to Add +1) ───
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap Product to Add (+1)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "1-Tap Quick Add",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (uiState.products.isEmpty()) {
                    Text(
                        text = "No products found. Add products in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.products) { product ->
                            val currentItem = uiState.selectedTableOrders.find { it.productId == product.id }
                            val currentQty = currentItem?.quantity ?: 0.0

                            Card(
                                modifier = Modifier
                                    .width(135.dp)
                                    .height(75.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        viewModel.quickAddProduct(product)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (currentQty > 0)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (currentQty > 0) 2.dp else 1.dp,
                                    if (currentQty > 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                ),
                                shape = RoundedCornerShape(14.dp),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (currentQty > 0) 4.dp else 1.dp
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.align(Alignment.CenterStart)
                                    ) {
                                        Text(
                                            text = product.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "₹${product.defaultRate.toInt()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (currentQty > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.align(Alignment.TopEnd)
                                        ) {
                                            val qStr = if (currentQty % 1.0 == 0.0) currentQty.toInt().toString() else currentQty.toString()
                                            Text(
                                                text = "×$qStr",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ─── Current Orders List with Instant Minus / Plus ───
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Orders (${uiState.selectedTableOrders.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.selectedTableOrders.isNotEmpty()) {
                        Text(
                            text = "Tap [-] to minus accidental adds",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                if (uiState.selectedTableOrders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Table is free. Tap any product above to add order.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 210.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.selectedTableOrders, key = { it.id }) { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant
                                ),
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.productName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "₹${item.rate.toInt()} each",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Stepper [-] QTY [+] and Subtotal
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // MINUS BUTTON (Tap to decrease or remove accidental entry)
                                        IconButton(
                                            onClick = { viewModel.decrementProduct(item) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                                    CircleShape
                                                )
                                        ) {
                                            Text(
                                                text = "-",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }

                                        Text(
                                            text = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )

                                        // PLUS BUTTON
                                        IconButton(
                                            onClick = { viewModel.incrementProduct(item) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Plus",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Text(
                                            text = "₹${item.amount.toInt()}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFD97706)
                                        )

                                        IconButton(
                                            onClick = { viewModel.removeOrderItem(item.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ─── Total Amount Display ───
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Running Total",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.selectedTableOrders.size} items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = uiState.selectedTableTotal.formatCurrency(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD97706)
                        )
                    }
                }

                // ─── Action Buttons: Print Bill & Bill & Settle (Revenue Recorded) ───
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (uiState.selectedTableOrders.isNotEmpty()) {
                                viewModel.printTableBill(context) { encodedPath ->
                                    onNavigateToPdfViewer(encodedPath)
                                }
                            }
                        },
                        enabled = uiState.selectedTableOrders.isNotEmpty() && !uiState.isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Print Bill", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (uiState.selectedTableOrders.isNotEmpty()) {
                                viewModel.billAndSettleTable(context) { encodedPath ->
                                    onNavigateToPdfViewer(encodedPath)
                                }
                            }
                        },
                        enabled = uiState.selectedTableOrders.isNotEmpty() && !uiState.isSaving,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD97706)
                        )
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Bill & Settle ✓", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.businessProfile.hotelNameMarathi.ifBlank { "हॉटेल रुद्रा अमृततुल्य" },
                                fontFamily = KingFontFamily,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706),
                                fontSize = 23.sp
                            )
                            val subTitle = uiState.businessProfile.hotelNameEnglish.ifBlank { "Hotel Rudra Amruttulya" }
                            Text(
                                text = subTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        // ─── Hamburger / Sandwich Menu ───
                        var menuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Settings", fontWeight = FontWeight.Medium) },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = { menuExpanded = false; onNavigateToSettings() }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                DropdownMenuItem(
                                    text = { Text("Reports", fontWeight = FontWeight.Medium) },
                                    leadingIcon = { Icon(Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = { menuExpanded = false; onNavigateToReports() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Generate Bill", fontWeight = FontWeight.Medium) },
                                    leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = { menuExpanded = false; onNavigateToBilling() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Daily Expenses", fontWeight = FontWeight.Medium) },
                                    leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFD97706)) },
                                    onClick = { menuExpanded = false; onNavigateToExpense() }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                DropdownMenuItem(
                                    text = { Text("Customers", fontWeight = FontWeight.Medium) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = { menuExpanded = false; onNavigateToCustomers() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add Customer", fontWeight = FontWeight.Medium) },
                                    leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF27AE60)) },
                                    onClick = { menuExpanded = false; onNavigateToAddCustomer() }
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ─── TODAY AT A GLANCE ─── Hotel revenue stats
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Today at a Glance",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatCell(
                                label = "Today Revenue",
                                value = uiState.todaySales.formatCurrency(),
                                valueColor = Color(0xFFD97706)
                            )
                            VerticalDivider(
                                modifier = Modifier.height(42.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            StatCell(
                                label = "Tables Active",
                                value = "${uiState.activeTablesCount} / ${uiState.tables.size}",
                                valueColor = if (uiState.activeTablesCount > 0) Color(0xFFE07B00) else Color(0xFF27AE60)
                            )
                            VerticalDivider(
                                modifier = Modifier.height(42.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            StatCell(
                                label = "Month Revenue",
                                value = uiState.monthlySales.formatCurrency(),
                                valueColor = MaterialTheme.colorScheme.primary
                            )
                            VerticalDivider(
                                modifier = Modifier.height(42.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            StatCell(
                                label = "Dues",
                                value = uiState.pendingAmount.formatCurrency(),
                                valueColor = if (uiState.pendingAmount > 0) MaterialTheme.colorScheme.error else Color(0xFF27AE60)
                            )
                        }
                        if (uiState.totalTableOrdersAmount > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE07B00).copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Active table orders running",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFD97706),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = uiState.totalTableOrdersAmount.formatCurrency(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "टेबल व्यवस्थापन  •  Table Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = KingFontFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Table Grid: Tap table opens the Pop-Up Menu!
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.tables, key = { it.tableNumber }) { table ->
                        TableGridCard(
                            table = table,
                            onClick = {
                                viewModel.selectTable(table.tableNumber)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TableGridCard(
    table: TableDashboardItem,
    onClick: () -> Unit
) {
    // Occupied: warm amber border + slightly tinted surface
    // Free: subtle outline, clean white/cream surface
    val isOccupied = table.isOccupied
    val cardColor = if (isOccupied)
        MaterialTheme.colorScheme.surface
    else
        MaterialTheme.colorScheme.surface

    val borderColor = if (isOccupied)
        Color(0xFFE07B00) // deep saffron
    else
        MaterialTheme.colorScheme.outlineVariant

    val borderWidth = if (isOccupied) 2.dp else 1.dp
    val elevation = if (isOccupied) 6.dp else 2.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Table name + status pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Table ${table.tableNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = KingFontFamily,
                    color = if (isOccupied) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isOccupied) Color(0xFFE07B00) else Color(0xFF27AE60)
                ) {
                    Text(
                        text = if (isOccupied) "● ${table.itemCount}" else "Free",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            // Body: order summary or free state
            if (isOccupied) {
                Text(
                    text = table.itemsSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "₹${String.format(java.util.Locale.getDefault(), "%.0f", table.totalAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFD97706)
                )
            } else {
                Text(
                    text = "No orders yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF27AE60), CircleShape)
                    )
                    Text(
                        text = "Available  •  Tap to order",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF27AE60),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─── Stat Cell for Today at a Glance ───
@Composable
fun StatCell(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
