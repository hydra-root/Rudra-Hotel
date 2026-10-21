package com.teasupplier.app.feature.dashboard

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: State management for Hotel Dashboard, table state tracking, real-time order sums,
 * table bill clearing, and PDF receipt trigger logic.
 */

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teasupplier.app.data.local.dao.CustomerDao
import com.teasupplier.app.data.local.dao.TableOrderDao
import com.teasupplier.app.data.local.entity.*
import com.teasupplier.app.data.preferences.AppPreferences
import com.teasupplier.app.data.preferences.BusinessProfile
import com.teasupplier.app.domain.engine.RoutePredictionEngine
import com.teasupplier.app.domain.repository.CustomerRepository
import com.teasupplier.app.domain.repository.PaymentRepository
import com.teasupplier.app.domain.repository.ProductRepository
import com.teasupplier.app.domain.repository.SaleRepository
import com.teasupplier.app.utils.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

data class TableDashboardItem(
    val tableNumber: Int,
    val isOccupied: Boolean,
    val itemCount: Int,
    val totalAmount: Double,
    val itemsSummary: String
)

data class DashboardUiState(
    val todaySales: Double = 0.0,
    val todayQuantity: Double = 0.0,
    val monthlySales: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val totalCustomers: Int = 0,
    val activeTablesCount: Int = 0,
    val totalTableOrdersAmount: Double = 0.0,
    val tables: List<TableDashboardItem> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val selectedTableNumber: Int? = null,
    val selectedTableOrders: List<TableOrderEntity> = emptyList(),
    val selectedTableTotal: Double = 0.0,
    val isSaving: Boolean = false,
    val message: String? = null,
    val businessProfile: BusinessProfile = BusinessProfile()
)

data class RoutePredictionState(
    val completedCustomers: List<CustomerEntity> = emptyList(),
    val predictedCustomer: CustomerEntity? = null,
    val confidence: Double = 0.0,
    val alternatives: List<CustomerEntity> = emptyList(),
    val remainingCustomers: List<CustomerEntity> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val customerDao: CustomerDao,
    private val saleRepository: SaleRepository,
    private val paymentRepository: PaymentRepository,
    private val tableOrderDao: TableOrderDao,
    private val productRepository: ProductRepository,
    private val routePredictionEngine: RoutePredictionEngine,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val startOfToday = com.teasupplier.app.utils.DateUtils.getStartOfDay(System.currentTimeMillis())
    private val endOfToday = com.teasupplier.app.utils.DateUtils.getEndOfDay(System.currentTimeMillis())
    private val startOfMonth: Long = run {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.timeInMillis
    }

    private val _selectedTableNumber = MutableStateFlow<Int?>(null)
    private val _isSaving = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        saleRepository.getTotalSalesByDateRange(startOfToday, endOfToday),
        saleRepository.getTotalQuantityByDateRange(startOfToday, endOfToday),
        saleRepository.getTotalSalesByDateRange(startOfMonth, endOfToday),
        customerRepository.getAllCustomers(),
        tableOrderDao.getAllOrders(),
        productRepository.getActiveProducts(),
        appPreferences.tableCount,
        appPreferences.businessProfile,
        _selectedTableNumber,
        _isSaving,
        _message
    ) { params ->
        val todaySales = params[0] as? Double ?: 0.0
        val todayQty = params[1] as? Double ?: 0.0
        val monthSales = params[2] as? Double ?: 0.0
        @Suppress("UNCHECKED_CAST")
        val customers = params[3] as List<CustomerEntity>
        @Suppress("UNCHECKED_CAST")
        val allTableOrders = params[4] as List<TableOrderEntity>
        @Suppress("UNCHECKED_CAST")
        val products = params[5] as List<ProductEntity>
        val tableCount = params[6] as Int
        val profile = params[7] as BusinessProfile
        val selectedTable = params[8] as? Int
        val saving = params[9] as Boolean
        val msg = params[10] as? String

        val pendingAmount = customers.sumOf { it.balance }

        val ordersByTable = allTableOrders.groupBy { it.tableNumber }
        val tableList = (1..tableCount).map { tableNum ->
            val tableOrders = ordersByTable[tableNum] ?: emptyList()
            val isOccupied = tableOrders.isNotEmpty()
            val totalAmt = tableOrders.sumOf { it.amount }
            val count = tableOrders.size
            val summary = if (isOccupied) {
                tableOrders.joinToString(", ") {
                    val qStr = if (it.quantity % 1.0 == 0.0) it.quantity.toInt().toString() else it.quantity.toString()
                    "${it.productName} × $qStr"
                }
            } else {
                "Available"
            }
            TableDashboardItem(
                tableNumber = tableNum,
                isOccupied = isOccupied,
                itemCount = count,
                totalAmount = totalAmt,
                itemsSummary = summary
            )
        }

        val activeCount = tableList.count { it.isOccupied }
        val totalActiveAmount = tableList.sumOf { it.totalAmount }

        val currentTableOrders = if (selectedTable != null) {
            allTableOrders.filter { it.tableNumber == selectedTable }
        } else {
            emptyList()
        }
        val currentTableTotal = currentTableOrders.sumOf { it.amount }

        DashboardUiState(
            todaySales = todaySales,
            todayQuantity = todayQty,
            monthlySales = monthSales,
            pendingAmount = pendingAmount,
            totalCustomers = customers.size,
            activeTablesCount = activeCount,
            totalTableOrdersAmount = totalActiveAmount,
            tables = tableList,
            products = products,
            selectedTableNumber = selectedTable,
            selectedTableOrders = currentTableOrders,
            selectedTableTotal = currentTableTotal,
            isSaving = saving,
            message = msg,
            businessProfile = profile
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun selectTable(tableNumber: Int?) {
        _selectedTableNumber.value = tableNumber
    }

    fun quickAddProduct(product: ProductEntity) {
        val tableNum = _selectedTableNumber.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = tableOrderDao.getOrderByTableAndProduct(tableNum, product.id)
            if (existing != null) {
                val newQty = existing.quantity + 1.0
                tableOrderDao.update(
                    existing.copy(
                        quantity = newQty,
                        amount = newQty * existing.rate
                    )
                )
            } else {
                val newOrder = TableOrderEntity(
                    tableNumber = tableNum,
                    productId = product.id,
                    productName = product.name,
                    quantity = 1.0,
                    rate = product.defaultRate,
                    amount = product.defaultRate
                )
                tableOrderDao.insert(newOrder)
            }
        }
    }

    fun decrementProduct(order: TableOrderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (order.quantity > 1.0) {
                val newQty = order.quantity - 1.0
                tableOrderDao.update(
                    order.copy(
                        quantity = newQty,
                        amount = newQty * order.rate
                    )
                )
            } else {
                tableOrderDao.deleteById(order.id)
            }
        }
    }

    fun incrementProduct(order: TableOrderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val newQty = order.quantity + 1.0
            tableOrderDao.update(
                order.copy(
                    quantity = newQty,
                    amount = newQty * order.rate
                )
            )
        }
    }

    fun removeOrderItem(orderId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            tableOrderDao.deleteById(orderId)
        }
    }

    fun clearCurrentTable() {
        val tableNum = _selectedTableNumber.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            tableOrderDao.clearTable(tableNum)
            withContext(Dispatchers.Main) {
                _message.value = "Table #$tableNum cleared ✓"
                _selectedTableNumber.value = null
            }
        }
    }

    fun billAndSettleTable(context: Context, onBillReady: (String) -> Unit) {
        val tableNum = _selectedTableNumber.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isSaving.value = true
            try {
                val orders = tableOrderDao.getOrdersByTableSync(tableNum)
                if (orders.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _message.value = "No items to bill on Table #$tableNum"
                    }
                    return@launch
                }
                val total = orders.sumOf { it.amount }

                // 1. Record in sales & payments for hotel revenue and ledger
                recordTableRevenue(tableNum, orders, total)

                // 2. Generate PDF receipt
                val profile = uiState.value.businessProfile
                val file = PdfGenerator.generateTableBill(
                    context = context,
                    tableNumber = tableNum,
                    orders = orders,
                    grandTotal = total,
                    hotelNameMarathi = profile.hotelNameMarathi,
                    hotelNameEnglish = profile.hotelNameEnglish,
                    hotelAddress = profile.address,
                    hotelPhone = profile.phone,
                    upiId = profile.upiId
                )

                // 3. Clear table orders
                tableOrderDao.clearTable(tableNum)

                withContext(Dispatchers.Main) {
                    _selectedTableNumber.value = null
                    _message.value = "Table #$tableNum settled & revenue recorded ✓"
                    if (file != null) {
                        val encodedPath = android.net.Uri.encode(file.absolutePath)
                        onBillReady(encodedPath)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _message.value = "Error settling table: ${e.message}"
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun printTableBill(context: Context, onBillReady: (String) -> Unit) {
        val tableNum = _selectedTableNumber.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isSaving.value = true
            try {
                val orders = tableOrderDao.getOrdersByTableSync(tableNum)
                if (orders.isEmpty()) return@launch
                val total = orders.sumOf { it.amount }
                val profile = uiState.value.businessProfile

                val file = PdfGenerator.generateTableBill(
                    context = context,
                    tableNumber = tableNum,
                    orders = orders,
                    grandTotal = total,
                    hotelNameMarathi = profile.hotelNameMarathi,
                    hotelNameEnglish = profile.hotelNameEnglish,
                    hotelAddress = profile.address,
                    hotelPhone = profile.phone,
                    upiId = profile.upiId
                )

                if (file != null) {
                    val encodedPath = android.net.Uri.encode(file.absolutePath)
                    withContext(Dispatchers.Main) {
                        onBillReady(encodedPath)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _message.value = "Error generating bill: ${e.message}"
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    private suspend fun recordTableRevenue(tableNumber: Int, orders: List<TableOrderEntity>, total: Double) {
        var tableCustomer = customerDao.getCustomerByShopName("Restaurant Table Orders (हॉटेल टेबल ऑर्डर)")
        if (tableCustomer == null) {
            val newCustomer = CustomerEntity(
                name = "Walk-in Customers (टेबल ग्राहक)",
                shopName = "Restaurant Table Orders (हॉटेल टेबल ऑर्डर)",
                mobile = "",
                address = "Hotel Premises",
                area = "Hotel",
                defaultRate = 10.0,
                openingBalance = 0.0,
                balance = 0.0,
                status = true,
                notes = "Auto-created for Restaurant Table POS"
            )
            val newId = customerDao.insert(newCustomer)
            tableCustomer = newCustomer.copy(id = newId)
        }

        val now = System.currentTimeMillis()
        // Record all items into sales table for revenue, daily/monthly reports & ledger
        val sales = orders.map { item ->
            SaleEntity(
                customerId = tableCustomer.id,
                productId = item.productId,
                productName = item.productName,
                date = now,
                quantity = item.quantity,
                rate = item.rate,
                amount = item.amount,
                notes = "Table #$tableNumber"
            )
        }
        saleRepository.insertSales(sales)

        // Record payment into payments table for balanced ledger
        val payment = PaymentEntity(
            customerId = tableCustomer.id,
            date = now,
            amount = total,
            mode = PaymentMode.CASH,
            notes = "Table #$tableNumber Settle"
        )
        paymentRepository.insertPayment(payment)
    }

    fun clearMessage() {
        _message.value = null
    }

    val routeState: StateFlow<RoutePredictionState> = combine(
        saleRepository.getSalesByDateOrderedBySequence(startOfToday, endOfToday),
        customerRepository.getAllCustomers()
    ) { sequencedSales: List<SaleEntity>, customers: List<CustomerEntity> ->
        val customerMap = customers.associateBy { it.id }

        val completedCustomers = sequencedSales.mapNotNull { customerMap[it.customerId] }.distinctBy { it.id }
        val remainingCustomers = customers.filter { it !in completedCustomers }

        if (completedCustomers.isEmpty()) {
            val prediction = routePredictionEngine.predictFirstCustomer()
            RoutePredictionState(
                completedCustomers = emptyList(),
                predictedCustomer = prediction?.predictedCustomer,
                confidence = prediction?.confidence ?: 0.0,
                alternatives = prediction?.alternatives ?: remainingCustomers,
                remainingCustomers = remainingCustomers
            )
        } else {
            val lastCustomer = completedCustomers.last()
            val prediction = routePredictionEngine.predictNextCustomer(lastCustomer.id)

            val predicted = prediction?.predictedCustomer
            val actualPredicted = if (predicted != null && predicted !in completedCustomers) predicted else null
            val alternatives = prediction?.alternatives?.filter { it !in completedCustomers } ?: emptyList()

            val actualAlternatives = if (actualPredicted == null) {
                remainingCustomers
            } else {
                val alts = alternatives.toMutableList()
                remainingCustomers.forEach { if (it != actualPredicted && it !in alts) alts.add(it) }
                alts
            }

            RoutePredictionState(
                completedCustomers = completedCustomers,
                predictedCustomer = actualPredicted,
                confidence = prediction?.confidence ?: 0.0,
                alternatives = actualAlternatives.filter { it != actualPredicted },
                remainingCustomers = remainingCustomers
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RoutePredictionState()
    )
}
