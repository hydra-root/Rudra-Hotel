package com.teasupplier.app.feature.tableorder

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: ViewModel managing hotel dining table orders, live database queries,
 * payment recording, table bill generation with UPI QR code, and table clearance.
 */

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teasupplier.app.data.local.dao.CustomerDao
import com.teasupplier.app.data.local.dao.TableOrderDao
import com.teasupplier.app.data.local.entity.*
import com.teasupplier.app.data.preferences.AppPreferences
import com.teasupplier.app.data.preferences.BusinessProfile
import com.teasupplier.app.domain.repository.PaymentRepository
import com.teasupplier.app.domain.repository.ProductRepository
import com.teasupplier.app.domain.repository.SaleRepository
import com.teasupplier.app.utils.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class TableOrderUiState(
    val tableNumber: Int = 1,
    val orders: List<TableOrderEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val selectedProduct: ProductEntity? = null,
    val quantity: String = "1",
    val grandTotal: Double = 0.0,
    val totalItems: Int = 0,
    val isSaving: Boolean = false,
    val message: String? = null,
    val businessProfile: BusinessProfile = BusinessProfile()
)

@HiltViewModel
class TableOrderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tableOrderDao: TableOrderDao,
    private val productRepository: ProductRepository,
    private val saleRepository: SaleRepository,
    private val paymentRepository: PaymentRepository,
    private val customerDao: CustomerDao,
    private val appPreferences: AppPreferences
) : ViewModel() {

    val tableNumber: Int = savedStateHandle.get<Int>("tableNumber") ?: 1

    private val _message = MutableStateFlow<String?>(null)
    private val _isSaving = MutableStateFlow(false)
    private val _selectedProduct = MutableStateFlow<ProductEntity?>(null)
    private val _quantity = MutableStateFlow("1")

    val uiState: StateFlow<TableOrderUiState> = combine(
        tableOrderDao.getOrdersByTable(tableNumber),
        productRepository.getActiveProducts(),
        _selectedProduct,
        _quantity,
        _isSaving,
        _message,
        appPreferences.businessProfile
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val orders = params[0] as List<TableOrderEntity>
        @Suppress("UNCHECKED_CAST")
        val products = params[1] as List<ProductEntity>
        val selectedProd = params[2] as? ProductEntity ?: products.firstOrNull()
        val qty = params[3] as String
        val saving = params[4] as Boolean
        val msg = params[5] as? String
        val profile = params[6] as BusinessProfile

        TableOrderUiState(
            tableNumber = tableNumber,
            orders = orders,
            products = products,
            selectedProduct = selectedProd,
            quantity = qty,
            grandTotal = orders.sumOf { it.amount },
            totalItems = orders.size,
            isSaving = saving,
            message = msg,
            businessProfile = profile
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TableOrderUiState(tableNumber = tableNumber)
    )

    fun onProductSelected(product: ProductEntity) {
        _selectedProduct.value = product
    }

    fun onQuantityChanged(qty: String) {
        _quantity.value = qty
    }

    fun adjustQuantity(delta: Int) {
        val current = _quantity.value.toIntOrNull() ?: 0
        val newQty = (current + delta).coerceAtLeast(1)
        _quantity.value = newQty.toString()
    }

    fun quickAddProduct(product: ProductEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = tableOrderDao.getOrderByTableAndProduct(tableNumber, product.id)
            if (existing != null) {
                val newQty = existing.quantity + 1.0
                tableOrderDao.update(
                    existing.copy(
                        quantity = newQty,
                        amount = newQty * existing.rate
                    )
                )
            } else {
                val order = TableOrderEntity(
                    tableNumber = tableNumber,
                    productId = product.id,
                    productName = product.name,
                    quantity = 1.0,
                    rate = product.defaultRate,
                    amount = product.defaultRate
                )
                tableOrderDao.insert(order)
            }
        }
    }

    fun decrementOrderItem(order: TableOrderEntity) {
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

    fun incrementOrderItem(order: TableOrderEntity) {
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

    fun addItem() {
        val product = uiState.value.selectedProduct ?: return
        val qty = _quantity.value.toDoubleOrNull() ?: return
        if (qty <= 0) return

        viewModelScope.launch(Dispatchers.IO) {
            val existing = tableOrderDao.getOrderByTableAndProduct(tableNumber, product.id)
            if (existing != null) {
                val newQty = existing.quantity + qty
                tableOrderDao.update(
                    existing.copy(
                        quantity = newQty,
                        amount = newQty * existing.rate
                    )
                )
            } else {
                val order = TableOrderEntity(
                    tableNumber = tableNumber,
                    productId = product.id,
                    productName = product.name,
                    quantity = qty,
                    rate = product.defaultRate,
                    amount = qty * product.defaultRate
                )
                tableOrderDao.insert(order)
            }
            _quantity.value = "1"
            _message.value = "${product.name} × ${qty.toInt()} added ✓"
        }
    }

    fun removeItem(orderId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            tableOrderDao.deleteById(orderId)
        }
    }

    fun clearTable(onCleared: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            tableOrderDao.clearTable(tableNumber)
            withContext(Dispatchers.Main) {
                _message.value = "Table $tableNumber cleared ✓"
                onCleared()
            }
        }
    }

    fun generateTableBill(context: Context, onBillReady: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSaving.value = true
            try {
                val state = uiState.value
                val file = PdfGenerator.generateTableBill(
                    context = context,
                    tableNumber = tableNumber,
                    orders = state.orders,
                    grandTotal = state.grandTotal,
                    hotelNameMarathi = state.businessProfile.hotelNameMarathi,
                    hotelNameEnglish = state.businessProfile.hotelNameEnglish,
                    hotelAddress = state.businessProfile.address,
                    hotelPhone = state.businessProfile.phone,
                    upiId = state.businessProfile.upiId
                )
                if (file != null) {
                    val encodedPath = android.net.Uri.encode(file.absolutePath)
                    withContext(Dispatchers.Main) {
                        onBillReady(encodedPath)
                    }
                } else {
                    _message.value = "Failed to generate bill"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun printAndClearTable(context: Context, onBillReady: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSaving.value = true
            try {
                val state = uiState.value
                val orders = state.orders
                if (orders.isEmpty()) return@launch

                val total = state.grandTotal

                // 1. Record into sales & payments for hotel revenue & ledger calculation
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
                        notes = "Auto-created for Restaurant POS Table orders"
                    )
                    val newId = customerDao.insert(newCustomer)
                    tableCustomer = newCustomer.copy(id = newId)
                }

                val now = System.currentTimeMillis()
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

                val payment = PaymentEntity(
                    customerId = tableCustomer.id,
                    date = now,
                    amount = total,
                    mode = PaymentMode.CASH,
                    notes = "Table #$tableNumber Settle"
                )
                paymentRepository.insertPayment(payment)

                // 2. Generate PDF bill
                val file = PdfGenerator.generateTableBill(
                    context = context,
                    tableNumber = tableNumber,
                    orders = orders,
                    grandTotal = total,
                    hotelNameMarathi = state.businessProfile.hotelNameMarathi,
                    hotelNameEnglish = state.businessProfile.hotelNameEnglish,
                    hotelAddress = state.businessProfile.address,
                    hotelPhone = state.businessProfile.phone,
                    upiId = state.businessProfile.upiId
                )

                // 3. Clear table after recording revenue & generating bill
                tableOrderDao.clearTable(tableNumber)

                if (file != null) {
                    val encodedPath = android.net.Uri.encode(file.absolutePath)
                    withContext(Dispatchers.Main) {
                        _message.value = "Bill printed & Table $tableNumber settled ✓"
                        onBillReady(encodedPath)
                    }
                } else {
                    _message.value = "Failed to generate bill"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
