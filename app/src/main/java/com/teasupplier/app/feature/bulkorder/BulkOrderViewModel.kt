package com.teasupplier.app.feature.bulkorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teasupplier.app.data.local.dao.CustomerDao
import com.teasupplier.app.data.local.dao.CustomerProductRateDao
import com.teasupplier.app.data.local.entity.CustomerEntity
import com.teasupplier.app.data.local.entity.ProductEntity
import com.teasupplier.app.data.local.entity.SaleEntity
import com.teasupplier.app.domain.engine.RoutePredictionEngine
import com.teasupplier.app.domain.repository.CustomerRepository
import com.teasupplier.app.domain.repository.ProductRepository
import com.teasupplier.app.domain.repository.SaleRepository
import com.teasupplier.app.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

enum class ShiftType(val label: String) {
    MORNING("Morning (सकाळ)"),
    EVENING("Evening (संध्याकाळ)")
}

data class CustomerBulkRow(
    val customer: CustomerEntity,
    val rate: Double,
    val quantityText: String = "",
    val existingTodayQty: Double = 0.0
) {
    val quantityDouble: Double
        get() = quantityText.toDoubleOrNull() ?: 0.0

    val lineTotal: Double
        get() = quantityDouble * rate
}

data class BulkOrderUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val selectedProduct: ProductEntity? = null,
    val selectedDate: Long = System.currentTimeMillis(),
    val selectedShift: ShiftType = run {
        val cal = Calendar.getInstance()
        if (cal.get(Calendar.HOUR_OF_DAY) < 13) ShiftType.MORNING else ShiftType.EVENING
    },
    val quantities: Map<Long, String> = emptyMap(),
    val customerRates: Map<Long, Double> = emptyMap(),
    val existingTodaySales: Map<Long, Double> = emptyMap(),
    val searchQuery: String = "",
    val isSaving: Boolean = false,
    val message: String? = null
) {
    val rows: List<CustomerBulkRow>
        get() {
            val query = searchQuery.trim().lowercase()
            val filteredCustomers = if (query.isEmpty()) {
                customers
            } else {
                customers.filter {
                    it.shopName.lowercase().contains(query) ||
                    it.name.lowercase().contains(query) ||
                    it.area.lowercase().contains(query)
                }
            }

            return filteredCustomers.map { customer ->
                val rate = customerRates[customer.id] ?: selectedProduct?.defaultRate ?: customer.defaultRate
                CustomerBulkRow(
                    customer = customer,
                    rate = rate,
                    quantityText = quantities[customer.id] ?: "",
                    existingTodayQty = existingTodaySales[customer.id] ?: 0.0
                )
            }
        }

    val totalOrderedCount: Int
        get() = quantities.values.count { (it.toDoubleOrNull() ?: 0.0) > 0.0 }

    val grandTotalQuantity: Double
        get() = quantities.values.sumOf { it.toDoubleOrNull() ?: 0.0 }

    val grandTotalAmount: Double
        get() = customers.sumOf { customer ->
            val rate = customerRates[customer.id] ?: selectedProduct?.defaultRate ?: customer.defaultRate
            val qty = quantities[customer.id]?.toDoubleOrNull() ?: 0.0
            qty * rate
        }
}

@HiltViewModel
class BulkOrderViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val saleRepository: SaleRepository,
    private val productRepository: ProductRepository,
    private val customerProductRateDao: CustomerProductRateDao,
    private val customerDao: CustomerDao,
    private val routePredictionEngine: RoutePredictionEngine
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    private val _selectedShift = MutableStateFlow(
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 13) ShiftType.MORNING else ShiftType.EVENING
    )
    private val _selectedProduct = MutableStateFlow<ProductEntity?>(null)
    private val _quantities = MutableStateFlow<Map<Long, String>>(emptyMap())
    private val _searchQuery = MutableStateFlow("")
    private val _isSaving = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)
    private val _existingTodaySales = MutableStateFlow<Map<Long, Double>>(emptyMap())

    val uiState: StateFlow<BulkOrderUiState> = combine(
        customerRepository.getActiveCustomers(),
        productRepository.getActiveProducts(),
        customerProductRateDao.getAllRates(),
        _selectedDate,
        _selectedShift,
        _selectedProduct,
        _quantities,
        _searchQuery,
        _isSaving,
        _message,
        _existingTodaySales
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val customers = params[0] as List<CustomerEntity>
        @Suppress("UNCHECKED_CAST")
        val products = params[1] as List<ProductEntity>
        @Suppress("UNCHECKED_CAST")
        val allRates = params[2] as List<com.teasupplier.app.data.local.entity.CustomerProductRateEntity>
        val date = params[3] as Long
        val shift = params[4] as ShiftType
        val selectedProd = params[5] as? ProductEntity ?: products.firstOrNull { it.name.equals("Tea", ignoreCase = true) } ?: products.firstOrNull()
        @Suppress("UNCHECKED_CAST")
        val quantities = params[6] as Map<Long, String>
        val searchQuery = params[7] as String
        val isSaving = params[8] as Boolean
        val message = params[9] as? String
        @Suppress("UNCHECKED_CAST")
        val existingSales = params[10] as Map<Long, Double>

        // Build customer rates map for the currently selected product
        val ratesMap = mutableMapOf<Long, Double>()
        if (selectedProd != null) {
            allRates.filter { it.productId == selectedProd.id }.forEach {
                ratesMap[it.customerId] = it.rate
            }
        }

        BulkOrderUiState(
            customers = customers,
            products = products,
            selectedProduct = selectedProd,
            selectedDate = date,
            selectedShift = shift,
            quantities = quantities,
            customerRates = ratesMap,
            existingTodaySales = existingSales,
            searchQuery = searchQuery,
            isSaving = isSaving,
            message = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BulkOrderUiState()
    )

    init {
        refreshExistingSales()
    }

    fun onDateSelected(date: Long) {
        _selectedDate.value = date
        refreshExistingSales()
    }

    fun onShiftSelected(shift: ShiftType) {
        _selectedShift.value = shift
    }

    fun onProductSelected(product: ProductEntity) {
        _selectedProduct.value = product
        refreshExistingSales()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun updateQuantity(customerId: Long, qtyText: String) {
        val current = _quantities.value.toMutableMap()
        if (qtyText.isBlank()) {
            current.remove(customerId)
        } else {
            current[customerId] = qtyText
        }
        _quantities.value = current
    }

    fun adjustQuantity(customerId: Long, delta: Double) {
        val current = _quantities.value.toMutableMap()
        val currentQty = current[customerId]?.toDoubleOrNull() ?: 0.0
        val newQty = (currentQty + delta).coerceAtLeast(0.0)
        if (newQty <= 0.0) {
            current.remove(customerId)
        } else {
            current[customerId] = if (newQty % 1.0 == 0.0) newQty.toInt().toString() else newQty.toString()
        }
        _quantities.value = current
    }

    fun clearAll() {
        _quantities.value = emptyMap()
        _message.value = "Cleared all quantities"
    }

    /** Pre-fill orders using yesterday's sales */
    fun copyYesterdayOrders() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentDate = _selectedDate.value
                val yesterday = currentDate - 86400000L
                val yesterdayStart = DateUtils.getStartOfDay(yesterday)
                val yesterdayEnd = DateUtils.getEndOfDay(yesterday)

                val yesterdaySales = saleRepository.getSalesListByDateRange(yesterdayStart, yesterdayEnd)
                val selectedProd = _selectedProduct.value ?: uiState.value.products.firstOrNull()

                val filtered = if (selectedProd != null) {
                    yesterdaySales.filter { it.productId == selectedProd.id || it.productName.equals(selectedProd.name, ignoreCase = true) }
                } else {
                    yesterdaySales
                }

                if (filtered.isEmpty()) {
                    _message.value = "No orders found for yesterday to copy"
                    return@launch
                }

                val newQuantities = mutableMapOf<Long, String>()
                val groupedByCustomer = filtered.groupBy { it.customerId }
                for ((customerId, sales) in groupedByCustomer) {
                    val totalQty = sales.sumOf { it.quantity }
                    if (totalQty > 0) {
                        newQuantities[customerId] = if (totalQty % 1.0 == 0.0) totalQty.toInt().toString() else totalQty.toString()
                    }
                }

                _quantities.value = newQuantities
                _message.value = "Copied orders for ${newQuantities.size} shops from yesterday ✓"
            } catch (e: Exception) {
                _message.value = "Error copying yesterday: ${e.message}"
            }
        }
    }

    private fun refreshExistingSales() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val date = _selectedDate.value
                val startOfDay = DateUtils.getStartOfDay(date)
                val endOfDay = DateUtils.getEndOfDay(date)
                val sales = saleRepository.getSalesListByDateRange(startOfDay, endOfDay)
                val selectedProd = _selectedProduct.value

                val filtered = if (selectedProd != null) {
                    sales.filter { it.productId == selectedProd.id || it.productName.equals(selectedProd.name, ignoreCase = true) }
                } else {
                    sales
                }

                val map = filtered.groupBy { it.customerId }
                    .mapValues { (_, sList) -> sList.sumOf { it.quantity } }

                _existingTodaySales.value = map
            } catch (_: Exception) {}
        }
    }

    fun saveAllOrders(onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = uiState.value
            val product = state.selectedProduct
            if (product == null) {
                _message.value = "Please select a product"
                return@launch
            }

            val entriesToSave = state.customers.mapNotNull { customer ->
                val qtyStr = state.quantities[customer.id]
                val qty = qtyStr?.toDoubleOrNull() ?: 0.0
                if (qty > 0.0) {
                    val rate = state.customerRates[customer.id] ?: product.defaultRate
                    CustomerBulkRow(customer, rate, qtyStr ?: "", 0.0)
                } else null
            }

            if (entriesToSave.isEmpty()) {
                _message.value = "Please enter quantity for at least one customer"
                return@launch
            }

            _isSaving.value = true
            try {
                // Calculate timestamp based on date and selected shift
                val cal = Calendar.getInstance()
                cal.timeInMillis = state.selectedDate
                if (state.selectedShift == ShiftType.MORNING) {
                    cal.set(Calendar.HOUR_OF_DAY, 9)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                } else {
                    cal.set(Calendar.HOUR_OF_DAY, 16)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                }
                val saleTimestamp = cal.timeInMillis

                val startOfDay = DateUtils.getStartOfDay(saleTimestamp)
                val endOfDay = DateUtils.getEndOfDay(saleTimestamp)
                var currentSeq = (saleRepository.getMaxSequenceNumberForDate(startOfDay, endOfDay) ?: 0) + 1

                val salesToInsert = mutableListOf<SaleEntity>()
                var totalAmountSaved = 0.0

                for (row in entriesToSave) {
                    val sale = SaleEntity(
                        customerId = row.customer.id,
                        productId = product.id,
                        productName = product.name,
                        date = saleTimestamp,
                        quantity = row.quantityDouble,
                        rate = row.rate,
                        amount = row.lineTotal,
                        sequenceNumber = currentSeq++
                    )
                    salesToInsert.add(sale)
                    totalAmountSaved += row.lineTotal

                    // Update customer balance in real-time
                    customerDao.updateBalance(row.customer.id, row.lineTotal)

                    // Record transition for smart route ordering
                    try {
                        routePredictionEngine.recordSaleTransition(row.customer.id, saleTimestamp)
                    } catch (_: Exception) {}
                }

                saleRepository.insertSales(salesToInsert)

                _quantities.value = emptyMap()
                refreshExistingSales()

                withContext(Dispatchers.Main) {
                    _message.value = "Saved ${salesToInsert.size} orders! Total: ₹${if (totalAmountSaved % 1.0 == 0.0) totalAmountSaved.toInt() else totalAmountSaved} ✓"
                    onSuccess()
                }
            } catch (e: Exception) {
                _message.value = "Error saving orders: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
