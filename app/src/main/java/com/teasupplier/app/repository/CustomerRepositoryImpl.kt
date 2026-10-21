package com.teasupplier.app.repository

import com.teasupplier.app.data.local.dao.CustomerDao
import com.teasupplier.app.data.local.entity.CustomerEntity
import com.teasupplier.app.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao
) : CustomerRepository {
    override fun getAllCustomers(): Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    override fun getActiveCustomers(): Flow<List<CustomerEntity>> = customerDao.getCustomersByStatus(true)

    override suspend fun getCustomerById(id: Long): CustomerEntity? = customerDao.getCustomerById(id)

    override suspend fun insertCustomer(customer: CustomerEntity): Long = customerDao.insert(customer)

    override suspend fun updateCustomer(customer: CustomerEntity) = customerDao.update(customer)

    override suspend fun deleteCustomer(customer: CustomerEntity) = customerDao.delete(customer)
}
