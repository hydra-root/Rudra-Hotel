package com.teasupplier.app.feature.lock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teasupplier.app.core.ui.theme.GlassBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(
    onUnlockSuccess: () -> Unit,
    viewModel: AppLockViewModel = hiltViewModel()
) {
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val error by viewModel.error.collectAsState()

    var pinText by remember { mutableStateOf("") }

    LaunchedEffect(isUnlocked) {
        if (isUnlocked) {
            onUnlockSuccess()
        }
    }

    GlassBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "App Locked",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )

                        Text(
                            text = "App Locked",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Your subscription has expired. Please contact developer.\n\nPlease enter the unlock code to continue.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = pinText,
                            onValueChange = { 
                                pinText = it
                                viewModel.clearError()
                            },
                            label = { Text("Unlock Code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (error != null) {
                            Text(
                                text = error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }

                        Button(
                            onClick = { viewModel.verifyPin(pinText) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = pinText.isNotBlank()
                        ) {
                            Text("Unlock")
                        }
                    }
                }
            }
        }
    }
}
