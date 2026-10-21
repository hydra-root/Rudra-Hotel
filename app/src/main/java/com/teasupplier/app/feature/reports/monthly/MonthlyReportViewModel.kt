package com.teasupplier.app.feature.reports.monthly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teasupplier.app.domain.usecase.GetMonthlyReportUseCase
import com.teasupplier.app.domain.usecase.MonthlyReportRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.Calendar
import javax.inject.Inject

data class MonthlyReportUiState(
    val monthIndex: Int = Calendar.getInstance().get(Calendar.MONTH), // 0 to 11
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val searchQuery: String = "",
    val sortOption: MonthlySortOption = MonthlySortOption.NAME,
    val items: List<MonthlyReportRow> = emptyList(),
    val filteredItems: List<MonthlyReportRow> = emptyList(),
    val generatedPdf: File? = null
)

enum class MonthlySortOption {
    NAME, QUANTITY, AMOUNT, OUTSTANDING
}

@HiltViewModel
class MonthlyReportViewModel @Inject constructor(
    private val getMonthlyReportUseCase: GetMonthlyReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthlyReportUiState())
    val uiState = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val reportFlow = _uiState
        .map { it.monthIndex to it.year }
        .distinctUntilChanged()
        .flatMapLatest { (month, year) ->
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            
            val startOfMonth = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            cal.add(Calendar.MILLISECOND, -1)
            val endOfMonth = cal.timeInMillis
            
            getMonthlyReportUseCase(startOfMonth, endOfMonth)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        reportFlow.onEach { items ->
            _uiState.update { state -> 
                val newFiltered = applyFilters(items, state.searchQuery, state.sortOption)
                state.copy(items = items, filteredItems = newFiltered) 
            }
        }.launchIn(viewModelScope)
    }

    fun selectMonth(monthIndex: Int) {
        _uiState.update { it.copy(monthIndex = monthIndex, generatedPdf = null) }
    }

    fun selectYear(year: Int) {
        _uiState.update { it.copy(year = year, generatedPdf = null) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredItems = applyFilters(state.items, query, state.sortOption)
            )
        }
    }

    fun updateSortOption(option: MonthlySortOption) {
        _uiState.update { state ->
            state.copy(
                sortOption = option,
                filteredItems = applyFilters(state.items, state.searchQuery, option)
            )
        }
    }

    fun onPdfGenerated(file: File?) {
        _uiState.update { it.copy(generatedPdf = file) }
    }

    private fun applyFilters(items: List<MonthlyReportRow>, query: String, sort: MonthlySortOption): List<MonthlyReportRow> {
        val filtered = if (query.isBlank()) {
            items
        } else {
            items.filter { it.customer.shopName.contains(query, ignoreCase = true) }
        }

        return when (sort) {
            MonthlySortOption.NAME -> filtered.sortedBy { it.customer.shopName }
            MonthlySortOption.QUANTITY -> filtered.sortedByDescending { it.totalQuantity }
            MonthlySortOption.AMOUNT -> filtered.sortedByDescending { it.totalAmount }
            MonthlySortOption.OUTSTANDING -> filtered.sortedByDescending { it.outstanding }
        }
    }
}
