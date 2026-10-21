package com.teasupplier.app.feature.settings

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Settings & Configuration interface for managing hotel business profile,
 * products, security passcodes, database backups, and developer credits.
 */

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teasupplier.app.core.ui.theme.GlassBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToBusinessProfile: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Launchers for picking files
    val importCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.importCustomersCsv(context, it) }
    }

    val importDbLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.importDatabase(context, it) }
    }

    var showRestoreConfirm by remember { mutableStateOf(false) }

    // Show dialog for messages
    if (uiState.message != null) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = { viewModel.clearMessage() },
            title = { Text("Information") },
            text = { Text(uiState.message!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessage() }) {
                    Text("OK")
                }
            }
        )
    }
    val allowEditing by viewModel.allowEditing.collectAsState()
    val tableCount by viewModel.tableCount.collectAsState()

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Settings & Backup", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // ─── Business Profile Section ───
                    item {
                        Text(
                            text = "Hotel Profile (हॉटेल माहिती)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )

                        SettingsActionRow(
                            title = "Hotel & Bill Profile (हॉटेल प्रोफाईल)",
                            subtitle = "Set hotel name (Marathi/English), address, phone, and UPI ID on bills",
                            icon = Icons.Default.Home,
                            iconTint = Color(0xFFFF9800),
                            onClick = onNavigateToBusinessProfile
                        )
                    }

                    // ─── Preferences Section ───
                    item {
                        Text(
                            text = "Preferences",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SettingsActionRow(
                                title = "Manage Products & Rates",
                                subtitle = "Add and edit items like Tea, Wadapav, Coffee",
                                icon = Icons.Default.ShoppingCart,
                                iconTint = MaterialTheme.colorScheme.primary,
                                onClick = onNavigateToProducts
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (!allowEditing) Icons.Default.Lock else Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Allow Editing Sales",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (allowEditing) "Entries can be edited and deleted"
                                                   else "Entries locked — prevents accidental changes",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = allowEditing,
                                        onCheckedChange = { viewModel.toggleAllowEditing() }
                                    )
                                }
                            }

                            // Table Count Configuration
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    color = Color(0xFFD97706).copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = null,
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Number of Tables (टेबलांची संख्या)",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Total $tableCount tables shown on Dashboard",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(6, 10, 15, 20).forEach { preset ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (tableCount == preset) Color(0xFFD97706) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.clickable { viewModel.updateTableCount(preset) }
                                                ) {
                                                    Text(
                                                        text = "$preset",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = if (tableCount == preset) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            IconButton(
                                                onClick = { if (tableCount > 1) viewModel.updateTableCount(tableCount - 1) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text(
                                                text = "$tableCount",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            IconButton(
                                                onClick = { if (tableCount < 50) viewModel.updateTableCount(tableCount + 1) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ─── CSV Backup Section ───
                    item {
                        Text(
                            text = "Customer Backups (CSV)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SettingsActionRow(
                                title = "Export Customers to CSV",
                                subtitle = "Save all customer names, phone numbers, and balances",
                                icon = Icons.Default.Share,
                                iconTint = Color(0xFF2196F3),
                                onClick = { viewModel.exportCustomersCsv(context) }
                            )
                            
                            SettingsActionRow(
                                title = "Import Customers from CSV",
                                subtitle = "Restore customer list from a CSV file",
                                icon = Icons.Default.Add,
                                iconTint = Color(0xFF4CAF50),
                                onClick = { importCsvLauncher.launch("*/*") }
                            )
                        }
                    }

                    // ─── Database Backup Section ───
                    item {
                        Text(
                            text = "Full Database Backup (Advanced)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SettingsActionRow(
                                title = "Backup Entire Database (.db)",
                                subtitle = "Create a complete snapshot of all sales, payments, and history",
                                icon = Icons.Default.Done,
                                iconTint = Color(0xFF4CAF50),
                                onClick = { viewModel.exportDatabase(context) }
                            )
                            
                            SettingsActionRow(
                                title = "Restore Entire Database",
                                subtitle = "Overwrite all current data from a backup file",
                                icon = Icons.Default.Warning,
                                iconTint = MaterialTheme.colorScheme.error,
                                isDestructive = true,
                                onClick = { showRestoreConfirm = true }
                            )
                        }
                    }

                    // ─── About Developer Section ───
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color(0xFFD97706).copy(alpha = 0.35f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Hotel Rudra Billing & Management",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFFD97706)
                                )
                                Text(
                                    text = "Designed & Programmed by",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Pavan Pawar",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Version 1.0.0 • All Rights Reserved",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
    
    // Confirmation dialog for destructive restore
    if (showRestoreConfirm) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = { showRestoreConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Restore Database?") },
            text = { 
                Text(
                    "⚠️ This will permanently overwrite ALL current data " +
                    "(customers, sales, payments, and ledger entries) with the backup file.\n\n" +
                    "This action cannot be undone. The app will restart after restore."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirm = false
                        importDbLauncher.launch("*/*")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Restore & Overwrite")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDestructive) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                             else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            else Color.White.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color = iconTint.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

