package com.teasupplier.app.feature.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teasupplier.app.data.local.entity.CustomerEntity
import com.teasupplier.app.data.local.entity.SaleEntity
import com.teasupplier.app.domain.repository.CustomerRepository
import com.teasupplier.app.domain.repository.SaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val saleRepository: SaleRepository,
    private val appPreferences: com.teasupplier.app.data.preferences.AppPreferences
) : ViewModel() {

    val customers: StateFlow<List<CustomerEntity>> = customerRepository.getAllCustomers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val businessProfile: StateFlow<com.teasupplier.app.data.preferences.BusinessProfile> = appPreferences.businessProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.teasupplier.app.data.preferences.BusinessProfile()
        )

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState

    fun selectCustomer(customer: CustomerEntity) {
        _uiState.value = _uiState.value.copy(selectedCustomer = customer, generatedPdf = null)
        fetchSalesForSelectedDateRange()
    }

    fun selectDateRange(start: Long, end: Long) {
        _uiState.value = _uiState.value.copy(startDate = start, endDate = end, generatedPdf = null)
        fetchSalesForSelectedDateRange()
    }

    fun selectBillDate(date: Long) {
        _uiState.value = _uiState.value.copy(billDate = date, generatedPdf = null)
    }

    private var fetchJob: kotlinx.coroutines.Job? = null

    private fun fetchSalesForSelectedDateRange() {
        val customer = _uiState.value.selectedCustomer ?: return
        val start = _uiState.value.startDate
        val end = _uiState.value.endDate

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            saleRepository.getSalesByCustomerAndDateRange(customer.id, start, end)
                .collect { sales ->
                    _uiState.value = _uiState.value.copy(salesForMonth = sales)
                }
        }
    }

    fun onPdfGenerated(file: File?) {
        _uiState.value = _uiState.value.copy(generatedPdf = file)
    }

    fun generateMasterBill(context: android.content.Context, billingPeriodStr: String) {
        val start = _uiState.value.startDate
        val end = _uiState.value.endDate
        val billDate = _uiState.value.billDate
        val profile = businessProfile.value

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val customersList = customers.value
            val allSales = saleRepository.getAllSalesByDateRange(start, end).first()
            
            val salesByCustomer = customersList.associateWith { customer ->
                allSales.filter { it.customerId == customer.id }
            }
            val file = com.teasupplier.app.utils.PdfGenerator.generateMasterBill(
                context = context,
                customersWithSales = salesByCustomer,
                billingPeriodStr = billingPeriodStr,
                shopNameMarathi = profile.hotelNameMarathi,
                shopNameEnglish = profile.hotelNameEnglish,
                shopAddress = profile.address,
                shopPhone = profile.phone,
                billDate = billDate,
                upiId = profile.upiId
            )
            if (file != null) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onPdfGenerated(file)
                }
            }
        }
    }
}

data class BillingUiState(
    val selectedCustomer: CustomerEntity? = null,
    val startDate: Long = run {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.timeInMillis
    },
    val endDate: Long = run {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        cal.timeInMillis
    },
    val billDate: Long = System.currentTimeMillis(),
    val salesForMonth: List<SaleEntity> = emptyList(),
    val generatedPdf: File? = null
)
