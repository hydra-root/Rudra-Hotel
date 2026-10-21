package com.teasupplier.app.data.local.di

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Dagger/Hilt Dependency Injection module providing singleton Room AppDatabase
 * instance and DAO providers.
 */

import android.content.Context
import androidx.room.Room
import com.teasupplier.app.data.local.AppDatabase
import com.teasupplier.app.data.local.dao.CustomerDao
import com.teasupplier.app.data.local.dao.ExpenseDao
import com.teasupplier.app.data.local.dao.PaymentDao
import com.teasupplier.app.data.local.dao.SaleDao
import com.teasupplier.app.data.local.dao.ProductDao
import com.teasupplier.app.data.local.dao.CustomerProductRateDao
import com.teasupplier.app.data.local.dao.TableOrderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.teasupplier.app.data.local.dao.RouteDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "tea_supplier_db"
        )
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5
        )
        .build()
    }

    @Provides
    fun provideCustomerDao(database: AppDatabase): CustomerDao = database.customerDao()

    @Provides
    fun provideSaleDao(database: AppDatabase): SaleDao = database.saleDao()

    @Provides
    fun providePaymentDao(database: AppDatabase): PaymentDao = database.paymentDao()

    @Provides
    fun provideRouteDao(database: AppDatabase): RouteDao = database.routeDao()

    @Provides
    fun provideProductDao(database: AppDatabase): ProductDao = database.productDao()

    @Provides
    fun provideCustomerProductRateDao(database: AppDatabase): CustomerProductRateDao = database.customerProductRateDao()

    @Provides
    fun provideTableOrderDao(database: AppDatabase): TableOrderDao = database.tableOrderDao()

    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao = database.expenseDao()
}
