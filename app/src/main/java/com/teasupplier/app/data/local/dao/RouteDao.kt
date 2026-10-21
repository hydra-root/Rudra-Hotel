package com.teasupplier.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.teasupplier.app.data.local.entity.FirstCustomerHistoryEntity
import com.teasupplier.app.data.local.entity.TransitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Query("SELECT * FROM transitions WHERE fromCustomerId = :fromId ORDER BY (transitionCount * weight) DESC")
    suspend fun getTransitionsFrom(fromId: Long): List<TransitionEntity>
    
    @Query("SELECT * FROM transitions WHERE fromCustomerId = :fromId AND toCustomerId = :toId LIMIT 1")
    suspend fun getTransition(fromId: Long, toId: Long): TransitionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTransition(transition: TransitionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFirstCustomer(history: FirstCustomerHistoryEntity)
    
    @Query("SELECT customerId, COUNT(customerId) as frequency FROM first_customer_history GROUP BY customerId ORDER BY frequency DESC")
    suspend fun getMostFrequentFirstCustomers(): List<CustomerFrequency>
    
    @Query("SELECT * FROM first_customer_history WHERE date = :date LIMIT 1")
    suspend fun getFirstCustomerByDate(date: Long): FirstCustomerHistoryEntity?
}

data class CustomerFrequency(
    val customerId: Long,
    val frequency: Int
)
