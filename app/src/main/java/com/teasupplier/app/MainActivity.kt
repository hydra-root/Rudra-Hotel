package com.teasupplier.app

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Main entry Activity initializing Compose UI theme, app lock verification,
 * and high-level navigation router.
 */

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.viewModels
import com.teasupplier.app.core.navigation.AppNavigation
import com.teasupplier.app.feature.lock.AppLockScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            com.teasupplier.app.core.ui.theme.TeaSupplierTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel by viewModels()
                    val isLocked by viewModel.isLocked.collectAsState()

                    if (isLocked == true) {
                        AppLockScreen(onUnlockSuccess = { /* ViewModel automatically updates isLocked to false */ })
                    } else if (isLocked == false) {
                        AppNavigation()
                    }
                }
            }
        }
    }
}
