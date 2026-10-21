package com.teasupplier.app.domain.repository

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Domain repository interface defining customer entity operations.
 */

import com.teasupplier.app.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    fun getAllCustomers(): Flow<List<CustomerEntity>>
    fun getActiveCustomers(): Flow<List<CustomerEntity>>
    suspend fun getCustomerById(id: Long): CustomerEntity?
    suspend fun insertCustomer(customer: CustomerEntity): Long
    suspend fun updateCustomer(customer: CustomerEntity)
    suspend fun deleteCustomer(customer: CustomerEntity)
}
