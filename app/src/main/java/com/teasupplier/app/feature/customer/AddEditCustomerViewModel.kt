package com.teasupplier.app.feature.customer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teasupplier.app.data.local.entity.CustomerEntity
import com.teasupplier.app.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.teasupplier.app.domain.repository.ProductRepository
import com.teasupplier.app.data.local.entity.ProductEntity

@HiltViewModel
class AddEditCustomerViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val customerId: Long? = savedStateHandle.get<String>("customerId")?.toLongOrNull()

    private val _uiState = MutableStateFlow(AddEditCustomerUiState())
    val uiState: StateFlow<AddEditCustomerUiState> = _uiState

    init {
        viewModelScope.launch {
            val activeProducts = productRepository.getActiveProducts().first()
            val initialRates = activeProducts.map { product ->
                ProductRateInput(
                    product = product,
                    rateText = "",
                    hasCustomRate = false
                )
            }
            _uiState.update { it.copy(productRates = initialRates) }

            if (customerId != null && customerId != -1L) {
                val customer = customerRepository.getCustomerById(customerId)
                val customerRates = productRepository.getRatesForCustomer(customerId).first()

                customer?.let {
                    _uiState.value = _uiState.value.copy(
                        name = it.name,
                        shopName = it.shopName,
                        mobile = it.mobile,
                        address = it.address,
                        area = it.area,
                        openingBalance = it.openingBalance.toString(),
                        isEditMode = true,
                        isActive = it.status
                    )
                }

                // Apply custom rates
                val updatedRates = _uiState.value.productRates.map { input ->
                    val customRate = customerRates.find { rate -> rate.productId == input.product.id }
                    if (customRate != null) {
                        input.copy(rateText = customRate.rate.toString(), hasCustomRate = true)
                    } else {
                        input.copy(rateText = input.product.defaultRate.toString(), hasCustomRate = false)
                    }
                }
                _uiState.update { it.copy(productRates = updatedRates) }
            }
        }
    }

    fun onEvent(event: AddEditCustomerEvent) {
        when (event) {
            is AddEditCustomerEvent.NameChanged -> _uiState.update { it.copy(name = event.name) }
            is AddEditCustomerEvent.ShopNameChanged -> _uiState.update { it.copy(shopName = event.shopName, shopNameError = null) }
            is AddEditCustomerEvent.MobileChanged -> _uiState.update { it.copy(mobile = event.mobile, mobileError = null) }
            is AddEditCustomerEvent.AddressChanged -> _uiState.update { it.copy(address = event.address) }
            is AddEditCustomerEvent.AreaChanged -> _uiState.update { it.copy(area = event.area) }
            is AddEditCustomerEvent.OpeningBalanceChanged -> _uiState.update { it.copy(openingBalance = event.balance) }
            is AddEditCustomerEvent.ToggleStatus -> toggleStatus()
            is AddEditCustomerEvent.ProductRateChanged -> updateProductRate(event.productId, event.rate)
            AddEditCustomerEvent.SaveCustomer -> saveCustomer()
        }
    }

    private fun updateProductRate(productId: Long, rate: String) {
        val updatedRates = _uiState.value.productRates.map {
            if (it.product.id == productId) it.copy(rateText = rate) else it
        }
        _uiState.update { it.copy(productRates = updatedRates) }
    }



    private fun saveCustomer() {
        val state = _uiState.value
        
        // Validate required fields
        var hasError = false
        if (state.shopName.isBlank()) {
            _uiState.update { it.copy(shopNameError = "Shop name is required") }
            hasError = true
        }
        if (state.mobile.isNotBlank() && (state.mobile.length < 10 || !state.mobile.all { it.isDigit() })) {
            _uiState.update { it.copy(mobileError = "Enter a valid 10-digit mobile number") }
            hasError = true
        }
        if (hasError) return
        
        viewModelScope.launch {
            val balance = state.openingBalance.toDoubleOrNull() ?: 0.0

            val isEditing = customerId != null && customerId != -1L

            // For existing customers, preserve their current balance
            val currentBalance = if (isEditing) {
                customerRepository.getCustomerById(customerId!!)?.balance ?: balance
            } else {
                balance
            }

            val customer = CustomerEntity(
                id = if (isEditing) customerId!! else 0,
                name = state.name,
                shopName = state.shopName,
                mobile = state.mobile,
                address = state.address,
                area = state.area,
                defaultRate = 0.0, // Deprecated, unused
                openingBalance = balance,
                balance = currentBalance,
                status = state.isActive
            )

            val savedCustomerId = if (isEditing) {
                customerRepository.updateCustomer(customer)
                customerId!!
            } else {
                customerRepository.insertCustomer(customer)
            }

            // Save product custom rates
            state.productRates.forEach { input ->
                val customRateVal = input.rateText.toDoubleOrNull()
                if (customRateVal != null) {
                    productRepository.saveRate(savedCustomerId, input.product.id, customRateVal)
                }
            }

            _uiState.update { it.copy(isSaved = true) }
        }
    }
    
    private fun toggleStatus() {
        viewModelScope.launch {
            if (customerId != null && customerId != -1L) {
                val customer = customerRepository.getCustomerById(customerId) ?: return@launch
                val updatedCustomer = customer.copy(status = !customer.status)
                customerRepository.updateCustomer(updatedCustomer)
                _uiState.update { it.copy(isActive = updatedCustomer.status) }
            }
        }
    }
}

data class AddEditCustomerUiState(
    val name: String = "",
    val shopName: String = "",
    val mobile: String = "",
    val address: String = "",
    val area: String = "",
    val openingBalance: String = "",
    val isSaved: Boolean = false,
    val isEditMode: Boolean = false,
    val isActive: Boolean = true,
    // Validation errors
    val shopNameError: String? = null,
    val mobileError: String? = null,
    val productRates: List<ProductRateInput> = emptyList()
)

data class ProductRateInput(
    val product: ProductEntity,
    val rateText: String,
    val hasCustomRate: Boolean
)

sealed class AddEditCustomerEvent {
    data class NameChanged(val name: String) : AddEditCustomerEvent()
    data class ShopNameChanged(val shopName: String) : AddEditCustomerEvent()
    data class MobileChanged(val mobile: String) : AddEditCustomerEvent()
    data class AddressChanged(val address: String) : AddEditCustomerEvent()
    data class AreaChanged(val area: String) : AddEditCustomerEvent()
    data class OpeningBalanceChanged(val balance: String) : AddEditCustomerEvent()
    data class ProductRateChanged(val productId: Long, val rate: String) : AddEditCustomerEvent()
    object ToggleStatus : AddEditCustomerEvent()
    object SaveCustomer : AddEditCustomerEvent()
}
