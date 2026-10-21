package com.teasupplier.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.teasupplier.app.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Delete
    suspend fun delete(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE status = :isActive ORDER BY name ASC")
    fun getCustomersByStatus(isActive: Boolean): Flow<List<CustomerEntity>>

    @Query("UPDATE customers SET balance = balance + :amount WHERE id = :id")
    suspend fun updateBalance(id: Long, amount: Double)

    @Query("SELECT * FROM customers WHERE shopName = :shopName LIMIT 1")
    suspend fun getCustomerByShopName(shopName: String): CustomerEntity?
}
