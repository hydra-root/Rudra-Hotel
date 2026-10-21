package com.teasupplier.app.feature.reports.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teasupplier.app.data.local.entity.CustomerEntity
import com.teasupplier.app.domain.repository.CustomerRepository
import com.teasupplier.app.domain.usecase.CustomerTransactionRow
import com.teasupplier.app.domain.usecase.GetCustomerReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CustomerReportUiState(
    val selectedCustomer: CustomerEntity? = null,
    val items: List<CustomerTransactionRow> = emptyList(),
    val generatedPdf: File? = null
)

@HiltViewModel
class CustomerReportViewModel @Inject constructor(
    private val getCustomerReportUseCase: GetCustomerReportUseCase,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerReportUiState())
    val uiState = _uiState.asStateFlow()

    private val _customers = MutableStateFlow<List<CustomerEntity>>(emptyList())
    val customers = _customers.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val reportFlow = _uiState
        .map { it.selectedCustomer }
        .distinctUntilChanged()
        .flatMapLatest { customer ->
            if (customer != null) {
                getCustomerReportUseCase(customer.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            customerRepository.getAllCustomers().collect { 
                _customers.value = it 
            }
        }
        
        reportFlow.onEach { items ->
            _uiState.update { it.copy(items = items) }
        }.launchIn(viewModelScope)
    }

    fun selectCustomer(customer: CustomerEntity) {
        _uiState.update { it.copy(selectedCustomer = customer, generatedPdf = null) }
    }

    fun onPdfGenerated(file: File?) {
        _uiState.update { it.copy(generatedPdf = file) }
    }
}
