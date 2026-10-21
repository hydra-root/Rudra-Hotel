package com.teasupplier.app.data.repository

import com.teasupplier.app.data.local.dao.PaymentDao
import com.teasupplier.app.data.local.entity.PaymentEntity
import com.teasupplier.app.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val paymentDao: PaymentDao
) : PaymentRepository {
    override suspend fun insertPayment(payment: PaymentEntity): Long {
        return paymentDao.insert(payment)
    }

    override suspend fun updatePayment(payment: PaymentEntity) {
        paymentDao.update(payment)
    }

    override suspend fun deletePayment(payment: PaymentEntity) {
        paymentDao.delete(payment)
    }

    override fun getPaymentsByCustomer(customerId: Long): Flow<List<PaymentEntity>> {
        return paymentDao.getPaymentsByCustomer(customerId)
    }

    override fun getTotalPaymentsByDateRange(startDate: Long, endDate: Long): Flow<Double?> {
        return paymentDao.getTotalPaymentsByDateRange(startDate, endDate)
    }

    override fun getAllPaymentsByDateRange(startDate: Long, endDate: Long): Flow<List<PaymentEntity>> {
        return paymentDao.getAllPaymentsByDateRange(startDate, endDate)
    }
}
