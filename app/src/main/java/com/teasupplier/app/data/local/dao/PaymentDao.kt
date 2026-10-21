package com.teasupplier.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.teasupplier.app.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity): Long

    @Update
    suspend fun update(payment: PaymentEntity)

    @Delete
    suspend fun delete(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY date DESC")
    fun getPaymentsByCustomer(customerId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT SUM(amount) FROM payments WHERE date >= :startDate AND date <= :endDate")
    fun getTotalPaymentsByDateRange(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT * FROM payments WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getAllPaymentsByDateRange(startDate: Long, endDate: Long): Flow<List<PaymentEntity>>
}
