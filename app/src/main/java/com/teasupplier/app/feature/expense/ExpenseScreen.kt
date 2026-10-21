package com.teasupplier.app.feature.expense

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Daily Hotel Expense Tracking UI providing KPI cards (Today's & Monthly totals),
 * date range filtering, fast-entry bottom sheet with category chips (Milk, Gas, Salary, Grocery),
 * and PDF expense sheet export.
 */

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teasupplier.app.core.ui.theme.GlassBackground
import com.teasupplier.app.core.ui.theme.KingFontFamily
import com.teasupplier.app.data.local.entity.ExpenseEntity
import com.teasupplier.app.utils.DateUtils
import com.teasupplier.app.utils.formatCurrency
import java.text.SimpleDateFormat
import java.util.*

// Preset expense categories for quick entry
private val EXPENSE_CATEGORIES = listOf(
    "Milk" to "🥛",
    "Gas / LPG" to "🔥",
    "Salary" to "👷",
    "Vegetables" to "🥦",
    "Grocery" to "🛒",
    "Electricity" to "⚡",
    "Water" to "💧",
    "Cleaning" to "🧹",
    "Maintenance" to "🔧",
    "Tea / Powder" to "🍵",
    "Packaging" to "📦",
    "Transport" to "🚗",
    "Other" to "💰"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPdfViewer: (String) -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // Add Expense Sheet state
    var showAddSheet by remember { mutableStateOf(false) }
    // Delete confirm
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }
    // Date filter pickers
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val startPickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.filterStartMs)
    val endPickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.filterEndMs)

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    // ─── Add Expense Bottom Sheet ───
    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var selectedCategory by remember { mutableStateOf("") }
        var customCategory by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var amountText by remember { mutableStateOf("") }
        var selectedDateMs by remember { mutableStateOf(System.currentTimeMillis()) }
        var showDatePicker by remember { mutableStateOf(false) }
        val entryDateState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMs)

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        showDatePicker = false
                        entryDateState.selectedDateMillis?.let { selectedDateMs = it }
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) { DatePicker(state = entryDateState) }
        }

        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Add Expense (खर्च नोंद)",
                    fontFamily = KingFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD97706)
                )

                // Date selector
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Date: ${dateFormatter.format(Date(selectedDateMs))}", fontWeight = FontWeight.SemiBold)
                }

                // Category quick chips
                Text("Category (प्रकार)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(EXPENSE_CATEGORIES) { (cat, emoji) ->
                        val selected = selectedCategory == cat
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedCategory = cat
                                customCategory = ""
                            },
                            label = { Text("$emoji $cat", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Custom category field
                OutlinedTextField(
                    value = customCategory,
                    onValueChange = {
                        customCategory = it
                        if (it.isNotBlank()) selectedCategory = ""
                    },
                    label = { Text("Or type custom category") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (वर्णन) — Optional") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ₹ (रक्कम)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Text("₹", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                )

                val finalCategory = customCategory.ifBlank { selectedCategory }
                Button(
                    onClick = {
                        viewModel.addExpense(
                            category = finalCategory,
                            description = description,
                            amount = amountText.toDoubleOrNull() ?: 0.0,
                            dateMs = selectedDateMs
                        )
                        showAddSheet = false
                    },
                    enabled = finalCategory.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE07B00))
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Expense", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }

    // ─── Delete Confirm Dialog ───
    expenseToDelete?.let { exp ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense?") },
            text = { Text("Delete \"${exp.category}\" — ₹${exp.amount.toInt()}?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteExpense(exp); expenseToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Date filter pickers
    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showStartPicker = false
                    startPickerState.selectedDateMillis?.let {
                        viewModel.setDateFilter(DateUtils.getStartOfDay(it), uiState.filterEndMs)
                    }
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = startPickerState) }
    }
    if (showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showEndPicker = false
                    endPickerState.selectedDateMillis?.let {
                        viewModel.setDateFilter(uiState.filterStartMs, DateUtils.getEndOfDay(it))
                    }
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = endPickerState) }
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
                                text = "Hotel Expenses",
                                fontFamily = KingFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFFD97706)
                            )
                            Text(
                                text = "हॉटेल खर्च नोंद",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        // Generate PDF button
                        IconButton(onClick = {
                            viewModel.generateExpenseSheetPdf(context, onNavigateToPdfViewer)
                        }) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "Export PDF",
                                tint = Color(0xFFE07B00)
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = Color(0xFFE07B00),
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    text = { Text("Add Expense", fontWeight = FontWeight.Bold) }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
            ) {
                // ─── Summary Stats Card ───
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ExpenseStatCell(
                                label = "Today's Expense",
                                value = uiState.totalToday.formatCurrency(),
                                color = if (uiState.totalToday > 0) MaterialTheme.colorScheme.error else Color(0xFF27AE60)
                            )
                            VerticalDivider(
                                modifier = Modifier.height(44.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            ExpenseStatCell(
                                label = "This Month",
                                value = uiState.totalThisMonth.formatCurrency(),
                                color = Color(0xFFD97706)
                            )
                            VerticalDivider(
                                modifier = Modifier.height(44.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            ExpenseStatCell(
                                label = "Filtered Total",
                                value = uiState.expenses.sumOf { it.amount }.formatCurrency(),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // ─── Date Filter ───
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Filter by Date Range",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                // Quick: Today / This Month
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AssistChip(
                                        onClick = {
                                            viewModel.setDateFilter(
                                                DateUtils.getStartOfDay(System.currentTimeMillis()),
                                                DateUtils.getEndOfDay(System.currentTimeMillis())
                                            )
                                        },
                                        label = { Text("Today", fontSize = 11.sp) }
                                    )
                                    AssistChip(
                                        onClick = {
                                            val cal = Calendar.getInstance()
                                            cal.set(Calendar.DAY_OF_MONTH, 1)
                                            cal.set(Calendar.HOUR_OF_DAY, 0)
                                            cal.set(Calendar.MINUTE, 0)
                                            cal.set(Calendar.SECOND, 0)
                                            cal.set(Calendar.MILLISECOND, 0)
                                            viewModel.setDateFilter(
                                                cal.timeInMillis,
                                                DateUtils.getEndOfDay(System.currentTimeMillis())
                                            )
                                        },
                                        label = { Text("Month", fontSize = 11.sp) }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showStartPicker = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(dateFormatter.format(Date(uiState.filterStartMs)), fontSize = 12.sp)
                                }
                                Text("→", modifier = Modifier.align(Alignment.CenterVertically))
                                OutlinedButton(
                                    onClick = { showEndPicker = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(dateFormatter.format(Date(uiState.filterEndMs)), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // ─── Expense List ───
                if (uiState.expenses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💰", fontSize = 48.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No expenses recorded",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Tap + Add Expense to start tracking",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                } else {
                    // Group by date
                    val grouped = uiState.expenses.groupBy { it.date }
                    val fmt = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
                    grouped.forEach { (dateMs, dayExpenses) ->
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = fmt.format(Date(dateMs)),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "₹${dayExpenses.sumOf { it.amount }.toInt()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        items(dayExpenses) { expense ->
                            ExpenseItem(
                                expense = expense,
                                onDelete = { expenseToDelete = expense }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseItem(expense: ExpenseEntity, onDelete: () -> Unit) {
    val emoji = EXPENSE_CATEGORIES.find { it.first == expense.category }?.second ?: "💰"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 20.sp, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (expense.description.isNotBlank()) {
                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "₹${expense.amount.toInt()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.error
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ExpenseStatCell(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
