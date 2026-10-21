package com.teasupplier.app.feature.reports.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teasupplier.app.domain.usecase.DailyReportRow
import com.teasupplier.app.domain.usecase.GetDailyReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.Calendar
import javax.inject.Inject

data class DailyReportUiState(
    val date: Long = run {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    },
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.NAME,
    val items: List<DailyReportRow> = emptyList(),
    val filteredItems: List<DailyReportRow> = emptyList(),
    val generatedPdf: File? = null
)

enum class SortOption {
    NAME, QUANTITY, AMOUNT
}

@HiltViewModel
class DailyReportViewModel @Inject constructor(
    private val getDailyReportUseCase: GetDailyReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyReportUiState())
    val uiState = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val reportFlow = _uiState
        .map { it.date }
        .distinctUntilChanged()
        .flatMapLatest { date ->
            val cal = Calendar.getInstance().apply { timeInMillis = date }
            cal.add(Calendar.DAY_OF_MONTH, 1)
            cal.add(Calendar.MILLISECOND, -1)
            getDailyReportUseCase(date, cal.timeInMillis)
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

    fun selectDate(date: Long) {
        _uiState.update { it.copy(date = date, generatedPdf = null) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredItems = applyFilters(state.items, query, state.sortOption)
            )
        }
    }

    fun updateSortOption(option: SortOption) {
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

    private fun applyFilters(items: List<DailyReportRow>, query: String, sort: SortOption): List<DailyReportRow> {
        val filtered = if (query.isBlank()) {
            items
        } else {
            items.filter { it.customer.shopName.contains(query, ignoreCase = true) }
        }

        return when (sort) {
            SortOption.NAME -> filtered.sortedBy { it.customer.shopName }
            SortOption.QUANTITY -> filtered.sortedByDescending { it.sale.quantity }
            SortOption.AMOUNT -> filtered.sortedByDescending { it.sale.amount }
        }
    }
}
