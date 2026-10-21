package com.teasupplier.app.data.repository

import com.teasupplier.app.data.local.dao.ProductDao
import com.teasupplier.app.data.local.dao.CustomerProductRateDao
import com.teasupplier.app.data.local.entity.ProductEntity
import com.teasupplier.app.data.local.entity.CustomerProductRateEntity
import com.teasupplier.app.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val customerProductRateDao: CustomerProductRateDao
) : ProductRepository {

    override fun getAllProducts(): Flow<List<ProductEntity>> = productDao.getAllProducts()

    override fun getActiveProducts(): Flow<List<ProductEntity>> = productDao.getActiveProducts()

    override suspend fun getProductById(id: Long): ProductEntity? = productDao.getProductById(id)

    override suspend fun insertProduct(product: ProductEntity): Long = productDao.insertProduct(product)

    override suspend fun updateProduct(product: ProductEntity) = productDao.updateProduct(product)

    override suspend fun deleteProduct(product: ProductEntity) = productDao.deleteProduct(product)

    override fun getRatesForCustomer(customerId: Long): Flow<List<CustomerProductRateEntity>> {
        return customerProductRateDao.getRatesForCustomer(customerId)
    }

    override suspend fun getRate(customerId: Long, productId: Long): CustomerProductRateEntity? {
        return customerProductRateDao.getRate(customerId, productId)
    }

    override suspend fun saveRate(customerId: Long, productId: Long, rate: Double) {
        val entity = CustomerProductRateEntity(
            customerId = customerId,
            productId = productId,
            rate = rate
        )
        customerProductRateDao.insertRate(entity)
    }
}
