package com.teasupplier.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.teasupplier.app.data.local.entity.SaleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sale: SaleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sales: List<SaleEntity>)

    @Update
    suspend fun update(sale: SaleEntity)

    @Delete
    suspend fun delete(sale: SaleEntity)

    @Query("SELECT * FROM sales WHERE customerId = :customerId ORDER BY date DESC")
    fun getSalesByCustomer(customerId: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE customerId = :customerId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getSalesByCustomerAndDateRange(customerId: Long, startDate: Long, endDate: Long): Flow<List<SaleEntity>>
    
    @Query("SELECT SUM(quantity) FROM sales WHERE date >= :startDate AND date <= :endDate")
    fun getTotalQuantityByDateRange(startDate: Long, endDate: Long): Flow<Double?>
    
    @Query("SELECT SUM(amount) FROM sales WHERE date >= :startDate AND date <= :endDate")
    fun getTotalSalesByDateRange(startDate: Long, endDate: Long): Flow<Double?>
    
    @Query("SELECT * FROM sales WHERE date >= :startDate AND date <= :endDate ORDER BY customerId, date DESC")
    fun getAllSalesByDateRange(startDate: Long, endDate: Long): Flow<List<SaleEntity>>

    @Query("SELECT MAX(sequenceNumber) FROM sales WHERE date >= :startOfDay AND date <= :endOfDay")
    suspend fun getMaxSequenceNumberForDate(startOfDay: Long, endOfDay: Long): Int?

    @Query("SELECT * FROM sales WHERE date >= :startOfDay AND date <= :endOfDay ORDER BY sequenceNumber ASC")
    fun getSalesByDateOrderedBySequence(startOfDay: Long, endOfDay: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE customerId = :customerId AND date >= :startOfDay AND date <= :endOfDay LIMIT 1")
    suspend fun getSaleByCustomerAndDate(customerId: Long, startOfDay: Long, endOfDay: Long): SaleEntity?

    @Query("SELECT * FROM sales WHERE date >= :startOfDay AND date <= :endOfDay ORDER BY sequenceNumber ASC")
    suspend fun getSalesListByDateRange(startOfDay: Long, endOfDay: Long): List<SaleEntity>
}
