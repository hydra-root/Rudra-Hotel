package com.teasupplier.app.domain.repository

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Domain repository interface defining sales transactions and analytics queries.
 */

import com.teasupplier.app.data.local.entity.SaleEntity
import kotlinx.coroutines.flow.Flow

interface SaleRepository {
    suspend fun insertSale(sale: SaleEntity): Long
    suspend fun insertSales(sales: List<SaleEntity>)
    fun getSalesByCustomer(customerId: Long): Flow<List<SaleEntity>>
    fun getSalesByCustomerAndDateRange(customerId: Long, startDate: Long, endDate: Long): Flow<List<SaleEntity>>
    fun getTotalQuantityByDateRange(startDate: Long, endDate: Long): Flow<Double?>
    fun getTotalSalesByDateRange(startDate: Long, endDate: Long): Flow<Double?>
    fun getAllSalesByDateRange(startDate: Long, endDate: Long): Flow<List<SaleEntity>>
    fun getSalesByDateOrderedBySequence(startOfDay: Long, endOfDay: Long): Flow<List<SaleEntity>>
    suspend fun updateSale(sale: SaleEntity)
    suspend fun deleteSale(sale: SaleEntity)
    suspend fun getSaleByCustomerAndDate(customerId: Long, startOfDay: Long, endOfDay: Long): SaleEntity?
    suspend fun getMaxSequenceNumberForDate(startOfDay: Long, endOfDay: Long): Int?
    suspend fun getSalesListByDateRange(startOfDay: Long, endOfDay: Long): List<SaleEntity>
}
