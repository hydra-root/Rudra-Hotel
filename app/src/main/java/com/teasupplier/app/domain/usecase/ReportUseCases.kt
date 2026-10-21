package com.teasupplier.app.domain.usecase

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Clean Architecture Use Cases aggregating daily sales, monthly customer statements,
 * outstanding balance ledger calculations, and reporting data models.
 */

import com.teasupplier.app.data.local.entity.CustomerEntity
import com.teasupplier.app.data.local.entity.PaymentEntity
import com.teasupplier.app.data.local.entity.SaleEntity
import com.teasupplier.app.domain.repository.CustomerRepository
import com.teasupplier.app.domain.repository.PaymentRepository
import com.teasupplier.app.domain.repository.SaleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class DailyReportRow(
    val customer: CustomerEntity,
    val sale: SaleEntity
)

data class MonthlyReportRow(
    val customer: CustomerEntity,
    val totalQuantity: Double,
    val totalAmount: Double,
    val totalPaid: Double,
    val outstanding: Double
)

data class CustomerTransactionRow(
    val id: String, // unique id for lazy column
    val date: Long,
    val type: TransactionType,
    val quantity: Double?,
    val rate: Double?,
    val amount: Double
)

enum class TransactionType {
    SALE, PAYMENT
}

class GetDailyReportUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
    private val customerRepository: CustomerRepository
) {
    operator fun invoke(startDate: Long, endDate: Long): Flow<List<DailyReportRow>> {
        return combine(
            saleRepository.getAllSalesByDateRange(startDate, endDate),
            customerRepository.getAllCustomers()
        ) { sales, customers ->
            val customerMap = customers.associateBy { it.id }
            sales.mapNotNull { sale ->
                val customer = customerMap[sale.customerId]
                if (customer != null) DailyReportRow(customer, sale) else null
            }
        }
    }
}

class GetMonthlyReportUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
    private val paymentRepository: PaymentRepository,
    private val customerRepository: CustomerRepository
) {
    operator fun invoke(startDate: Long, endDate: Long): Flow<List<MonthlyReportRow>> {
        return combine(
            saleRepository.getAllSalesByDateRange(startDate, endDate),
            paymentRepository.getAllPaymentsByDateRange(startDate, endDate),
            customerRepository.getAllCustomers()
        ) { sales, payments, customers ->
            customers.map { customer ->
                val customerSales = sales.filter { it.customerId == customer.id }
                val customerPayments = payments.filter { it.customerId == customer.id }
                
                val totalQty = customerSales.sumOf { it.quantity }
                val totalAmount = customerSales.sumOf { it.amount }
                val totalPaid = customerPayments.sumOf { it.amount }
                val outstanding = totalAmount - totalPaid
                
                MonthlyReportRow(
                    customer = customer,
                    totalQuantity = totalQty,
                    totalAmount = totalAmount,
                    totalPaid = totalPaid,
                    outstanding = outstanding
                )
            }.filter { it.totalAmount > 0 || it.totalPaid > 0 } // Only show active customers in this period
        }
    }
}

class GetCustomerReportUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
    private val paymentRepository: PaymentRepository
) {
    operator fun invoke(customerId: Long): Flow<List<CustomerTransactionRow>> {
        return combine(
            saleRepository.getSalesByCustomer(customerId),
            paymentRepository.getPaymentsByCustomer(customerId)
        ) { sales, payments ->
            val saleRows = sales.map { 
                CustomerTransactionRow("s_${it.id}", it.date, TransactionType.SALE, it.quantity, it.rate, it.amount) 
            }
            val paymentRows = payments.map { 
                CustomerTransactionRow("p_${it.id}", it.date, TransactionType.PAYMENT, null, null, it.amount) 
            }
            
            (saleRows + paymentRows).sortedByDescending { it.date }
        }
    }
}
