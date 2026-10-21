package com.teasupplier.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.teasupplier.app.data.local.entity.TableOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TableOrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: TableOrderEntity): Long

    @Update
    suspend fun update(order: TableOrderEntity)

    @Delete
    suspend fun delete(order: TableOrderEntity)

    @Query("SELECT * FROM table_orders WHERE tableNumber = :tableNumber ORDER BY createdAt ASC")
    fun getOrdersByTable(tableNumber: Int): Flow<List<TableOrderEntity>>

    @Query("SELECT DISTINCT tableNumber FROM table_orders")
    fun getActiveTableNumbers(): Flow<List<Int>>

    @Query("SELECT SUM(amount) FROM table_orders WHERE tableNumber = :tableNumber")
    fun getTotalByTable(tableNumber: Int): Flow<Double?>

    @Query("DELETE FROM table_orders WHERE tableNumber = :tableNumber")
    suspend fun clearTable(tableNumber: Int)

    @Query("DELETE FROM table_orders WHERE id = :orderId")
    suspend fun deleteById(orderId: Long)

    @Query("SELECT * FROM table_orders ORDER BY tableNumber ASC, createdAt ASC")
    fun getAllOrders(): Flow<List<TableOrderEntity>>

    @Query("SELECT tableNumber, SUM(amount) as total FROM table_orders GROUP BY tableNumber")
    fun getTableTotals(): Flow<List<TableTotal>>

    @Query("SELECT * FROM table_orders WHERE tableNumber = :tableNumber AND productId = :productId LIMIT 1")
    suspend fun getOrderByTableAndProduct(tableNumber: Int, productId: Long): TableOrderEntity?

    @Query("SELECT * FROM table_orders WHERE tableNumber = :tableNumber ORDER BY createdAt ASC")
    suspend fun getOrdersByTableSync(tableNumber: Int): List<TableOrderEntity>
}

data class TableTotal(
    val tableNumber: Int,
    val total: Double
)
