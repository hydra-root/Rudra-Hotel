package com.teasupplier.app.data.local.dao

import androidx.room.*
import com.teasupplier.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses ORDER BY date DESC, createdAt DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE date >= :startMs AND date <= :endMs ORDER BY date ASC")
    fun getExpensesByDateRange(startMs: Long, endMs: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount) FROM expenses WHERE date >= :startMs AND date <= :endMs")
    fun getTotalByDateRange(startMs: Long, endMs: Long): Flow<Double?>

    @Query("SELECT * FROM expenses WHERE date >= :startMs AND date <= :endMs ORDER BY date ASC")
    suspend fun getExpensesByDateRangeSync(startMs: Long, endMs: Long): List<ExpenseEntity>
}
