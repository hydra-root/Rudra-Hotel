package com.teasupplier.app.feature.settings

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Hotel identity & business profile screen allowing customization of hotel name
 * (Marathi & English), phone number, physical address, and UPI ID for payment QR code bills.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teasupplier.app.core.ui.theme.GlassBackground
import com.teasupplier.app.data.preferences.BusinessProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentProfile by viewModel.businessProfile.collectAsState()

    var nameMarathi by remember { mutableStateOf(currentProfile.hotelNameMarathi) }
    var nameEnglish by remember { mutableStateOf(currentProfile.hotelNameEnglish) }
    var address by remember { mutableStateOf(currentProfile.address) }
    var phone by remember { mutableStateOf(currentProfile.phone) }
    var upiId by remember { mutableStateOf(currentProfile.upiId) }

    // Sync when loaded from DataStore
    LaunchedEffect(currentProfile) {
        nameMarathi = currentProfile.hotelNameMarathi
        nameEnglish = currentProfile.hotelNameEnglish
        address = currentProfile.address
        phone = currentProfile.phone
        upiId = currentProfile.upiId
    }

    var showSavedMessage by remember { mutableStateOf(false) }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Hotel / Business Profile", fontWeight = FontWeight.Bold)
                            Text("हॉटेल आणि बिल माहिती", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ─── Header Live Preview Card ───
                Text(
                    text = "Bill Header Live Preview (बिल हेडर पूर्वावलोकन):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFFDF2) // Warm Cream matching PDF
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFDE047))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Saffron accent line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(Color(0xFFD97706), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = nameMarathi.ifBlank { "हॉटेलचे नाव (मराठी)" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD97706),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = nameEnglish.ifBlank { "Hotel Name (English)" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4E342E),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = address.ifBlank { "हॉटेलचा पत्ता / Address" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = phone.ifBlank { "मोबाईल क्रमांक / Phone" },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                        if (upiId.isNotBlank()) {
                            Text(
                                text = "UPI: $upiId",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ─── Input Fields ───
                Text(
                    text = "Edit Business Details (माहिती बदला):",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = nameMarathi,
                    onValueChange = { nameMarathi = it },
                    label = { Text("Hotel Name in Marathi (हॉटेलचे नाव - मराठी)") },
                    placeholder = { Text("उदा. हॉटेल रुद्रा अमृततुल्य") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = nameEnglish,
                    onValueChange = { nameEnglish = it },
                    label = { Text("Hotel Name in English (हॉटेलचे नाव - इंग्रजी)") },
                    placeholder = { Text("e.g. Hotel Rudra Amruttulya") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Location (पत्ता)") },
                    placeholder = { Text("उदा. एमआयडीसी, अहिल्यानगर") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Contact Phone / Mobile (फोन / मोबा. नं.)") },
                    placeholder = { Text("उदा. 8788138197 / 8830404860") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("UPI ID for Bill QR Code (पेमेंट QR कोड आयडी)") },
                    placeholder = { Text("उदा. 8788138197@ybl / GPay / PhonePe UPI") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (showSavedMessage) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                text = "Profile updated! All bills and dashboard will now use this hotel identity.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // ─── Actions: Save & Reset ───
                Button(
                    onClick = {
                        val updated = BusinessProfile(
                            hotelNameMarathi = nameMarathi.trim().ifBlank { "हॉटेल रुद्रा अमृततुल्य" },
                            hotelNameEnglish = nameEnglish.trim().ifBlank { "Hotel Rudra Amruttulya" },
                            address = address.trim().ifBlank { "कोतकर वस्ती, एमआयडीसी, अहिल्यानगर - ४१४१११" },
                            phone = phone.trim().ifBlank { "मोबा. नं.: 8788138197 / 8830404860" },
                            upiId = upiId.trim()
                        )
                        viewModel.updateBusinessProfile(updated)
                        showSavedMessage = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save", modifier = Modifier.padding(end = 8.dp))
                    Text("SAVE HOTEL PROFILE (प्रोफाईल सेव्ह करा)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                OutlinedButton(
                    onClick = {
                        nameMarathi = "हॉटेल रुद्रा अमृततुल्य"
                        nameEnglish = "Hotel Rudra Amruttulya"
                        address = "कोतकर वस्ती, एमआयडीसी, अहिल्यानगर - ४१४१११"
                        phone = "मोबा. नं.: 8788138197 / 8830404860"
                        upiId = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Reset to Defaults (डीफॉल्ट रुद्रा अमृततुल्य)")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
