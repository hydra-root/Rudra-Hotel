package com.teasupplier.app.domain.repository

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Domain repository interface defining hotel product catalog (Tea, Coffee, Snacks)
 * and custom per-customer rate mappings.
 */

import com.teasupplier.app.data.local.entity.ProductEntity
import com.teasupplier.app.data.local.entity.CustomerProductRateEntity
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getAllProducts(): Flow<List<ProductEntity>>
    fun getActiveProducts(): Flow<List<ProductEntity>>
    suspend fun getProductById(id: Long): ProductEntity?
    suspend fun insertProduct(product: ProductEntity): Long
    suspend fun updateProduct(product: ProductEntity)
    suspend fun deleteProduct(product: ProductEntity)

    fun getRatesForCustomer(customerId: Long): Flow<List<CustomerProductRateEntity>>
    suspend fun getRate(customerId: Long, productId: Long): CustomerProductRateEntity?
    suspend fun saveRate(customerId: Long, productId: Long, rate: Double)
}
