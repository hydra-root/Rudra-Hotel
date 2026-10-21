package com.teasupplier.app.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teasupplier.app.data.local.dao.CustomerDao
import com.teasupplier.app.domain.repository.CustomerRepository
import com.teasupplier.app.data.local.AppDatabase
import com.teasupplier.app.data.preferences.AppPreferences
import com.teasupplier.app.utils.BackupHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val customerDao: CustomerDao,
    private val appDatabase: AppDatabase,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    val allowEditing: StateFlow<Boolean> = appPreferences.allowEditing
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val businessProfile: StateFlow<com.teasupplier.app.data.preferences.BusinessProfile> = appPreferences.businessProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.teasupplier.app.data.preferences.BusinessProfile()
        )

    val tableCount: StateFlow<Int> = appPreferences.tableCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 10
        )

    fun updateTableCount(count: Int) {
        viewModelScope.launch {
            appPreferences.setTableCount(count)
        }
    }

    fun toggleAllowEditing() {
        viewModelScope.launch {
            appPreferences.setAllowEditing(!allowEditing.value)
        }
    }

    fun updateBusinessProfile(profile: com.teasupplier.app.data.preferences.BusinessProfile) {
        viewModelScope.launch {
            appPreferences.updateBusinessProfile(profile)
            _uiState.value = SettingsUiState(message = "Hotel profile updated successfully ✓")
        }
    }

    fun exportCustomersCsv(@Suppress("UNUSED_PARAMETER") context: Context) {
        _uiState.value = SettingsUiState(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val customers = customerRepository.getAllCustomers().first()
                val path = BackupHelper.exportCustomersToCsv(customers)
                withContext(Dispatchers.Main) {
                    if (path != null) {
                        _uiState.value = SettingsUiState(message = "Exported successfully to:\n$path")
                    } else {
                        _uiState.value = SettingsUiState(message = "Failed to export customers.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = SettingsUiState(message = "Error: ${e.message}")
                }
            }
        }
    }

    fun importCustomersCsv(context: Context, uri: Uri) {
        _uiState.value = SettingsUiState(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = BackupHelper.importCustomersFromCsv(context, uri, customerDao)
                withContext(Dispatchers.Main) {
                    _uiState.value = SettingsUiState(message = "Successfully imported $count customers.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = SettingsUiState(message = "Error importing: ${e.message}")
                }
            }
        }
    }

    fun exportDatabase(context: Context) {
        _uiState.value = SettingsUiState(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val path = BackupHelper.exportDatabase(context, appDatabase)
                withContext(Dispatchers.Main) {
                    if (path != null) {
                        _uiState.value = SettingsUiState(message = "Database backup saved to:\n$path")
                    } else {
                        _uiState.value = SettingsUiState(message = "Failed to export database.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = SettingsUiState(message = "Error: ${e.message}")
                }
            }
        }
    }

    fun importDatabase(context: Context, uri: Uri) {
        _uiState.value = SettingsUiState(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = BackupHelper.importDatabase(context, uri)
                withContext(Dispatchers.Main) {
                    if (success) {
                        _uiState.value = SettingsUiState(message = "Database restored successfully. The app will now restart.")
                        // Give user a moment to read before restarting
                        kotlinx.coroutines.delay(2000)
                        // Restart the app properly instead of killing the process
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        Runtime.getRuntime().exit(0)
                    } else {
                        _uiState.value = SettingsUiState(message = "Failed to restore database.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = SettingsUiState(message = "Error: ${e.message}")
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }


}
