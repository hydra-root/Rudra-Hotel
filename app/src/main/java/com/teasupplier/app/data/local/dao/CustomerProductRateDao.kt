package com.teasupplier.app.data.local.dao

import androidx.room.*
import com.teasupplier.app.data.local.entity.CustomerProductRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerProductRateDao {
    @Query("SELECT * FROM customer_product_rates")
    fun getAllRates(): Flow<List<CustomerProductRateEntity>>

    @Query("SELECT * FROM customer_product_rates WHERE customerId = :customerId")
    fun getRatesForCustomer(customerId: Long): Flow<List<CustomerProductRateEntity>>

    @Query("SELECT * FROM customer_product_rates WHERE customerId = :customerId AND productId = :productId")
    suspend fun getRate(customerId: Long, productId: Long): CustomerProductRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: CustomerProductRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<CustomerProductRateEntity>)

    @Query("DELETE FROM customer_product_rates WHERE customerId = :customerId AND productId = :productId")
    suspend fun deleteRate(customerId: Long, productId: Long)
}
