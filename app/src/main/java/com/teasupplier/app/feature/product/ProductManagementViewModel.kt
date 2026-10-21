package com.teasupplier.app.feature.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teasupplier.app.data.local.entity.ProductEntity
import com.teasupplier.app.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductManagementUiState(
    val products: List<ProductEntity> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class ProductManagementViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductManagementUiState(isLoading = true))
    val uiState: StateFlow<ProductManagementUiState> = _uiState

    init {
        viewModelScope.launch {
            productRepository.getAllProducts().collectLatest { products ->
                if (products.isEmpty()) {
                    // Create default product
                    val defaultProduct = ProductEntity(name = "Tea", defaultRate = 10.0)
                    productRepository.insertProduct(defaultProduct)
                } else {
                    _uiState.value = _uiState.value.copy(
                        products = products,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun saveProduct(name: String, defaultRate: String, productToEdit: ProductEntity? = null) {
        if (name.isBlank()) {
            showMessage("Product name cannot be empty")
            return
        }
        val rateDouble = defaultRate.toDoubleOrNull()
        if (rateDouble == null || rateDouble < 0) {
            showMessage("Please enter a valid rate")
            return
        }

        viewModelScope.launch {
            if (productToEdit != null) {
                productRepository.updateProduct(productToEdit.copy(name = name, defaultRate = rateDouble))
                showMessage("Product updated")
            } else {
                productRepository.insertProduct(ProductEntity(name = name, defaultRate = rateDouble))
                showMessage("Product added")
            }
        }
    }

    fun toggleProductStatus(product: ProductEntity) {
        viewModelScope.launch {
            productRepository.updateProduct(product.copy(isActive = !product.isActive))
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            productRepository.deleteProduct(product)
            showMessage("Product deleted")
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun showMessage(msg: String) {
        _uiState.value = _uiState.value.copy(message = msg)
    }
}
