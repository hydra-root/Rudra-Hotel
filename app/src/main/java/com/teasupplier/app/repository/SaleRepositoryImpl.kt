package com.teasupplier.app.repository

import com.teasupplier.app.data.local.dao.SaleDao
import com.teasupplier.app.data.local.entity.SaleEntity
import com.teasupplier.app.domain.repository.SaleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SaleRepositoryImpl @Inject constructor(
    private val saleDao: SaleDao
) : SaleRepository {
    override suspend fun insertSale(sale: SaleEntity): Long = saleDao.insert(sale)
    
    override suspend fun insertSales(sales: List<SaleEntity>) = saleDao.insertAll(sales)
    
    override fun getSalesByCustomer(customerId: Long): Flow<List<SaleEntity>> = saleDao.getSalesByCustomer(customerId)
    
    override fun getSalesByCustomerAndDateRange(customerId: Long, startDate: Long, endDate: Long): Flow<List<SaleEntity>> = 
        saleDao.getSalesByCustomerAndDateRange(customerId, startDate, endDate)
        
    override fun getTotalQuantityByDateRange(startDate: Long, endDate: Long): Flow<Double?> = 
        saleDao.getTotalQuantityByDateRange(startDate, endDate)
        
    override fun getTotalSalesByDateRange(startDate: Long, endDate: Long): Flow<Double?> = 
        saleDao.getTotalSalesByDateRange(startDate, endDate)
        
    override fun getAllSalesByDateRange(startDate: Long, endDate: Long): Flow<List<SaleEntity>> =
        saleDao.getAllSalesByDateRange(startDate, endDate)
        
    override fun getSalesByDateOrderedBySequence(startOfDay: Long, endOfDay: Long): Flow<List<SaleEntity>> =
        saleDao.getSalesByDateOrderedBySequence(startOfDay, endOfDay)
        
    override suspend fun updateSale(sale: SaleEntity) = saleDao.update(sale)
    
    override suspend fun deleteSale(sale: SaleEntity) = saleDao.delete(sale)
    
    override suspend fun getSaleByCustomerAndDate(customerId: Long, startOfDay: Long, endOfDay: Long): SaleEntity? =
        saleDao.getSaleByCustomerAndDate(customerId, startOfDay, endOfDay)

    override suspend fun getMaxSequenceNumberForDate(startOfDay: Long, endOfDay: Long): Int? =
        saleDao.getMaxSequenceNumberForDate(startOfDay, endOfDay)

    override suspend fun getSalesListByDateRange(startOfDay: Long, endOfDay: Long): List<SaleEntity> =
        saleDao.getSalesListByDateRange(startOfDay, endOfDay)
}
