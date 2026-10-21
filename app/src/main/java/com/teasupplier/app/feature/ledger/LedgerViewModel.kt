package com.teasupplier.app.feature.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teasupplier.app.data.local.dao.CustomerDao
import com.teasupplier.app.data.local.entity.CustomerEntity
import com.teasupplier.app.data.local.entity.PaymentEntity
import com.teasupplier.app.data.local.entity.SaleEntity
import com.teasupplier.app.domain.repository.CustomerRepository
import com.teasupplier.app.domain.repository.PaymentRepository
import com.teasupplier.app.domain.repository.SaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val saleRepository: SaleRepository,
    private val paymentRepository: PaymentRepository,
    private val customerDao: CustomerDao
) : ViewModel() {

    val customers: StateFlow<List<CustomerEntity>> = customerRepository.getAllCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(LedgerUiState())
    val uiState: StateFlow<LedgerUiState> = _uiState

    fun selectCustomer(customer: CustomerEntity) {
        _uiState.value = _uiState.value.copy(selectedCustomer = customer)
        fetchLedgerForCustomer(customer.id)
    }

    private var fetchJob: kotlinx.coroutines.Job? = null

    private fun fetchLedgerForCustomer(customerId: Long) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            combine(
                saleRepository.getSalesByCustomer(customerId),
                paymentRepository.getPaymentsByCustomer(customerId)
            ) { sales, payments ->
                val entries = mutableListOf<LedgerEntry>()
                sales.forEach { entries.add(LedgerEntry.Sale(it)) }
                payments.forEach { entries.add(LedgerEntry.Payment(it)) }
                
                // Sort chronologically
                entries.sortBy { it.timestamp }
                
                // Calculate running balance
                var balance = 0.0
                entries.forEach { entry ->
                    when (entry) {
                        is LedgerEntry.Sale -> balance += entry.amount
                        is LedgerEntry.Payment -> balance -= entry.amount
                    }
                    entry.runningBalance = balance
                }
                
                // Return sorted descending for UI
                entries.reversed()
            }.collect { entries ->
                _uiState.value = _uiState.value.copy(ledgerEntries = entries)
            }
        }
    }

    fun addPayment(amount: Double, description: String) {
        val customer = _uiState.value.selectedCustomer ?: return
        viewModelScope.launch {
            val payment = PaymentEntity(
                customerId = customer.id,
                date = System.currentTimeMillis(),
                amount = amount,
                mode = com.teasupplier.app.data.local.entity.PaymentMode.CASH,
                notes = description
            )
            paymentRepository.insertPayment(payment)
            
            // Use atomic SQL update to avoid stale balance
            customerDao.updateBalance(customer.id, -amount)
        }
    }
}

sealed class LedgerEntry(
    val timestamp: Long,
    val amount: Double,
    var runningBalance: Double = 0.0
) {
    class Sale(val sale: SaleEntity) : LedgerEntry(sale.date, sale.amount)
    class Payment(val payment: PaymentEntity) : LedgerEntry(payment.date, payment.amount)
}

data class LedgerUiState(
    val selectedCustomer: CustomerEntity? = null,
    val ledgerEntries: List<LedgerEntry> = emptyList()
)
