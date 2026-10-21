package com.teasupplier.app

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Root ViewModel managing app-level security state, license/trial period,
 * and startup access restrictions.
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teasupplier.app.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _isLocked = MutableStateFlow<Boolean?>(null) // null = loading
    val isLocked: StateFlow<Boolean?> = _isLocked

    init {
        // Automatically lock editing on app startup
        viewModelScope.launch {
            appPreferences.setAllowEditing(false)
        }

        viewModelScope.launch {
            appPreferences.expiryDate.collectLatest { expiry ->
                val now = System.currentTimeMillis()
                if (expiry == null) {
                    // First time install, set expiry to 28 days from now
                    val newExpiry = now + (28L * 24 * 60 * 60 * 1000)
                    appPreferences.setExpiryDate(newExpiry)
                    _isLocked.value = false
                } else {
                    _isLocked.value = now > expiry
                }
            }
        }
    }
}
