package com.teasupplier.app.domain.repository

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Domain repository interface defining customer payments and ledger transactions.
 */

import com.teasupplier.app.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    suspend fun insertPayment(payment: PaymentEntity): Long
    suspend fun updatePayment(payment: PaymentEntity)
    suspend fun deletePayment(payment: PaymentEntity)
    fun getPaymentsByCustomer(customerId: Long): Flow<List<PaymentEntity>>
    fun getTotalPaymentsByDateRange(startDate: Long, endDate: Long): Flow<Double?>
    fun getAllPaymentsByDateRange(startDate: Long, endDate: Long): Flow<List<PaymentEntity>>
}
