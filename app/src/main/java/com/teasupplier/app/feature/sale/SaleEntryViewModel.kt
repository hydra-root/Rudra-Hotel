package com.teasupplier.app.feature.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teasupplier.app.data.local.entity.CustomerEntity
import com.teasupplier.app.data.local.entity.SaleEntity
import com.teasupplier.app.domain.repository.CustomerRepository
import com.teasupplier.app.domain.repository.SaleRepository
import com.teasupplier.app.data.local.AppDatabase
import com.teasupplier.app.data.local.dao.CustomerDao
import com.teasupplier.app.data.local.dao.CustomerProductRateDao
import com.teasupplier.app.data.local.entity.ProductEntity
import com.teasupplier.app.data.local.entity.CustomerProductRateEntity
import com.teasupplier.app.domain.repository.ProductRepository
import com.teasupplier.app.data.preferences.AppPreferences
import com.teasupplier.app.domain.engine.RoutePredictionEngine
import com.teasupplier.app.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SaleEntryViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val saleRepository: SaleRepository,
    private val productRepository: ProductRepository,
    private val customerProductRateDao: CustomerProductRateDao,
    private val routePredictionEngine: RoutePredictionEngine,
    private val appDatabase: AppDatabase,
    private val customerDao: CustomerDao,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())

    // Message for snackbar
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    // Edit lock from settings
    val allowEditing: StateFlow<Boolean> = appPreferences.allowEditing
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Today's entries — derived reactively from the date
    private val _todayEntries: StateFlow<List<SaleEntryRow>> = _selectedDate
        .flatMapLatest { date ->
            val startOfDay = DateUtils.getStartOfDay(date)
            val endOfDay = DateUtils.getEndOfDay(date)
            saleRepository.getSalesByDateOrderedBySequence(startOfDay, endOfDay)
                .map { sales ->
                    val customerIds = sales.map { it.customerId }.distinct()
                    val customerMap = withContext(Dispatchers.IO) {
                        val map = mutableMapOf<Long, CustomerEntity>()
                        customerIds.forEach { id ->
                            customerRepository.getCustomerById(id)?.let { map[id] = it }
                        }
                        map
                    }
                    sales.mapIndexed { index, sale ->
                        SaleEntryRow(
                            sale = sale,
                            customer = customerMap[sale.customerId],
                            displayIndex = index + 1
                        )
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<SaleEntryUiState> = combine(
        customerRepository.getActiveCustomers(),
        productRepository.getActiveProducts(),
        customerProductRateDao.getAllRates(),
        _selectedDate,
        _todayEntries
    ) { customers, products, allRates, date, entries ->
        SaleEntryUiState(
            customers = customers,
            products = products,
            customerRates = allRates,
            selectedDate = date,
            entries = entries
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SaleEntryUiState()
    )

    fun onDateSelected(date: Long) {
        _selectedDate.value = date
    }

    /** Add a NEW entry for a customer */
    fun addEntry(customerId: Long, product: ProductEntity, quantity: Double) {
        if (quantity <= 0) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val customer = customerRepository.getCustomerById(customerId) ?: return@launch
                val date = _selectedDate.value
                val startOfDay = DateUtils.getStartOfDay(date)
                val endOfDay = DateUtils.getEndOfDay(date)
                val seq = (saleRepository.getMaxSequenceNumberForDate(startOfDay, endOfDay) ?: 0) + 1

                val customRate = productRepository.getRate(customerId, product.id)
                val rate = customRate?.rate ?: product.defaultRate
                
                val amount = quantity * rate

                val sale = SaleEntity(
                    customerId = customerId,
                    productId = product.id,
                    productName = product.name,
                    date = date,
                    quantity = quantity,
                    rate = rate,
                    amount = amount,
                    sequenceNumber = seq
                )
                saleRepository.insertSale(sale)
                try {
                    routePredictionEngine.recordSaleTransition(customerId, date)
                } catch (_: Exception) { /* best-effort route tracking */ }
                customerDao.updateBalance(customerId, amount)

                val qtyStr = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
                val rateStr = if (rate % 1.0 == 0.0) rate.toInt().toString() else rate.toString()
                _message.value = "${customer.shopName}: $qtyStr × ₹$rateStr added ✓"
            } catch (e: Exception) {
                _message.value = "Error saving: ${e.message}"
            }
        }
    }

    /** Repeat yesterday's entries for the currently selected date */
    fun repeatYesterday() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentDate = _selectedDate.value
                val currentStartOfDay = DateUtils.getStartOfDay(currentDate)
                val currentEndOfDay = DateUtils.getEndOfDay(currentDate)

                // Check if today already has entries
                val currentSales = saleRepository.getSalesListByDateRange(currentStartOfDay, currentEndOfDay)
                if (currentSales.isNotEmpty()) {
                    _message.value = "Cannot repeat: Entries already exist for this date."
                    return@launch
                }

                val yesterday = currentDate - 86400000 // subtract 1 day
                val yesterdayStart = DateUtils.getStartOfDay(yesterday)
                val yesterdayEnd = DateUtils.getEndOfDay(yesterday)

                val yesterdaySales = saleRepository.getSalesListByDateRange(yesterdayStart, yesterdayEnd)
                
                if (yesterdaySales.isEmpty()) {
                    _message.value = "No entries found for yesterday."
                    return@launch
                }

                // We need to verify which customers are still active, and calculate updated balances
                val newSales = mutableListOf<SaleEntity>()
                var currentSeq = 1

                for (oldSale in yesterdaySales) {
                    // Make sure customer still exists
                    val customer = customerRepository.getCustomerById(oldSale.customerId) ?: continue
                    
                    val newSale = oldSale.copy(
                        id = 0, // Reset ID for new insert
                        date = currentDate,
                        sequenceNumber = currentSeq++,
                        createdAt = System.currentTimeMillis()
                    )
                    newSales.add(newSale)
                    
                    // Update balance immediately for each duplicated sale
                    customerDao.updateBalance(oldSale.customerId, oldSale.amount)
                }

                saleRepository.insertSales(newSales)
                _message.value = "Copied ${newSales.size} entries from yesterday ✓"

            } catch (e: Exception) {
                _message.value = "Error copying entries: ${e.message}"
            }
        }
    }

    /** Edit an existing entry's quantity */
    fun editEntry(entryId: Long, newQuantity: Double) {
        if (newQuantity <= 0) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entry = _todayEntries.value.find { it.sale.id == entryId } ?: return@launch
                val oldSale = entry.sale
                val newAmount = newQuantity * oldSale.rate
                val diffAmount = newAmount - oldSale.amount

                val updatedSale = oldSale.copy(quantity = newQuantity, amount = newAmount)
                saleRepository.updateSale(updatedSale)
                customerDao.updateBalance(oldSale.customerId, diffAmount)

                _message.value = "Entry updated ✓"
            } catch (e: Exception) {
                _message.value = "Error updating: ${e.message}"
            }
        }
    }

    /** Delete an entry and reverse the balance */
    fun deleteEntry(entryId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entry = _todayEntries.value.find { it.sale.id == entryId } ?: return@launch
                saleRepository.deleteSale(entry.sale)
                customerDao.updateBalance(entry.sale.customerId, -entry.sale.amount)
                _message.value = "Entry deleted"
            } catch (e: Exception) {
                _message.value = "Error deleting: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

data class SaleEntryRow(
    val sale: SaleEntity,
    val customer: CustomerEntity?,
    val displayIndex: Int
)

data class SaleEntryUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val customerRates: List<CustomerProductRateEntity> = emptyList(),
    val selectedDate: Long = System.currentTimeMillis(),
    val entries: List<SaleEntryRow> = emptyList()
)
