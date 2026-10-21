package com.teasupplier.app.feature.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teasupplier.app.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun verifyPin(pin: String) {
        val expectedPin = calculateDynamicPin()
        if (pin == expectedPin) {
            // Unlock app for another 28 days
            viewModelScope.launch {
                val newExpiry = System.currentTimeMillis() + (28L * 24 * 60 * 60 * 1000)
                appPreferences.setExpiryDate(newExpiry)
                _isUnlocked.value = true
                _error.value = null
            }
        } else {
            _error.value = "Incorrect PIN code"
        }
    }
    
    // Package-private or public for testing/access
    fun calculateDynamicPin(): String {
        val today = LocalDate.now()
        var nextAug21 = LocalDate.of(today.year, 8, 21)
        
        // If today is after Aug 21, the next Aug 21 is next year
        if (today.isAfter(nextAug21)) {
            nextAug21 = LocalDate.of(today.year + 1, 8, 21)
        }
        
        val daysDiff = ChronoUnit.DAYS.between(today, nextAug21)
        return "${daysDiff}${today.year}"
    }

    fun clearError() {
        _error.value = null
    }
}
