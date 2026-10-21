package com.teasupplier.app.repository

import com.teasupplier.app.domain.repository.CustomerRepository
import com.teasupplier.app.domain.repository.SaleRepository
import com.teasupplier.app.domain.repository.ProductRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindCustomerRepository(
        customerRepositoryImpl: CustomerRepositoryImpl
    ): CustomerRepository

    @Binds
    abstract fun bindSaleRepository(
        saleRepositoryImpl: SaleRepositoryImpl
    ): SaleRepository

    @Binds
    abstract fun bindPaymentRepository(
        paymentRepositoryImpl: com.teasupplier.app.data.repository.PaymentRepositoryImpl
    ): com.teasupplier.app.domain.repository.PaymentRepository

    @Binds
    abstract fun bindProductRepository(
        productRepositoryImpl: com.teasupplier.app.data.repository.ProductRepositoryImpl
    ): ProductRepository
}
