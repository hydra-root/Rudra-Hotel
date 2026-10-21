package com.teasupplier.app.feature.expense

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: ViewModel managing hotel expense streams, category totals, date range filters,
 * Room database writes, and PDF expense sheet document generation.
 */

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teasupplier.app.data.local.dao.ExpenseDao
import com.teasupplier.app.data.local.entity.ExpenseEntity
import com.teasupplier.app.data.preferences.AppPreferences
import com.teasupplier.app.data.preferences.BusinessProfile
import com.teasupplier.app.utils.DateUtils
import com.teasupplier.app.utils.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import javax.inject.Inject

data class ExpenseUiState(
    val expenses: List<ExpenseEntity> = emptyList(),
    val totalToday: Double = 0.0,
    val totalThisMonth: Double = 0.0,
    val isLoading: Boolean = false,
    val message: String? = null,
    val businessProfile: BusinessProfile = BusinessProfile(),
    // Filter
    val filterStartMs: Long = DateUtils.getStartOfDay(System.currentTimeMillis()),
    val filterEndMs: Long = DateUtils.getEndOfDay(System.currentTimeMillis())
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _filterStart = MutableStateFlow(DateUtils.getStartOfDay(System.currentTimeMillis()))
    private val _filterEnd   = MutableStateFlow(DateUtils.getEndOfDay(System.currentTimeMillis()))
    private val _message     = MutableStateFlow<String?>(null)

    private val startOfToday = DateUtils.getStartOfDay(System.currentTimeMillis())
    private val endOfToday   = DateUtils.getEndOfDay(System.currentTimeMillis())
    private val startOfMonth: Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val uiState: StateFlow<ExpenseUiState> = combine(
        _filterStart,
        _filterEnd,
        _message,
        appPreferences.businessProfile
    ) { start, end, msg, profile ->
        Triple(Pair(start, end), msg, profile)
    }.flatMapLatest { (range, msg, profile) ->
        val (start, end) = range
        combine(
            expenseDao.getExpensesByDateRange(start, end),
            expenseDao.getTotalByDateRange(startOfToday, endOfToday),
            expenseDao.getTotalByDateRange(startOfMonth, endOfToday)
        ) { filtered, todayTotal, monthTotal ->
            ExpenseUiState(
                expenses = filtered,
                totalToday = todayTotal ?: 0.0,
                totalThisMonth = monthTotal ?: 0.0,
                message = msg,
                businessProfile = profile,
                filterStartMs = start,
                filterEndMs = end
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpenseUiState()
    )

    fun addExpense(category: String, description: String, amount: Double, dateMs: Long) {
        if (category.isBlank() || amount <= 0) {
            _message.value = "Please enter category and a valid amount"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            expenseDao.insert(
                ExpenseEntity(
                    date = DateUtils.getStartOfDay(dateMs),
                    category = category.trim(),
                    description = description.trim(),
                    amount = amount
                )
            )
            withContext(Dispatchers.Main) {
                _message.value = "Expense added ✓"
            }
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            expenseDao.delete(expense)
            withContext(Dispatchers.Main) {
                _message.value = "Expense deleted"
            }
        }
    }

    fun setDateFilter(startMs: Long, endMs: Long) {
        _filterStart.value = startMs
        _filterEnd.value   = endMs
    }

    fun generateExpenseSheetPdf(context: Context, onReady: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val expenses = expenseDao.getExpensesByDateRangeSync(_filterStart.value, _filterEnd.value)
            val profile = uiState.value.businessProfile
            val file = PdfGenerator.generateExpenseSheet(
                context = context,
                expenses = expenses,
                startMs = _filterStart.value,
                endMs = _filterEnd.value,
                hotelNameMarathi = profile.hotelNameMarathi,
                hotelNameEnglish = profile.hotelNameEnglish,
                hotelAddress = profile.address,
                hotelPhone = profile.phone
            )
            if (file != null) {
                withContext(Dispatchers.Main) {
                    val encoded = android.net.Uri.encode(file.absolutePath)
                    onReady(encoded)
                }
            } else {
                withContext(Dispatchers.Main) {
                    _message.value = "No expenses found for selected period"
                }
            }
        }
    }

    fun clearMessage() { _message.value = null }
}
