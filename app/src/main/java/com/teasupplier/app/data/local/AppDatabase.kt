package com.teasupplier.app.data.local

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Room Database schema definition, entity registry, DAO declarations,
 * and version migrations (v1 through v5).
 */

import androidx.room.Database
import androidx.room.RoomDatabase
import com.teasupplier.app.data.local.dao.CustomerDao
import com.teasupplier.app.data.local.dao.ExpenseDao
import com.teasupplier.app.data.local.dao.PaymentDao
import com.teasupplier.app.data.local.dao.SaleDao
import com.teasupplier.app.data.local.entity.CustomerEntity
import com.teasupplier.app.data.local.entity.ExpenseEntity
import com.teasupplier.app.data.local.entity.PaymentEntity
import com.teasupplier.app.data.local.entity.SaleEntity

import com.teasupplier.app.data.local.entity.FirstCustomerHistoryEntity
import com.teasupplier.app.data.local.entity.TransitionEntity
import com.teasupplier.app.data.local.entity.ProductEntity
import com.teasupplier.app.data.local.entity.CustomerProductRateEntity
import com.teasupplier.app.data.local.entity.TableOrderEntity
import com.teasupplier.app.data.local.dao.ProductDao
import com.teasupplier.app.data.local.dao.CustomerProductRateDao
import com.teasupplier.app.data.local.dao.TableOrderDao
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.teasupplier.app.data.local.dao.RouteDao

@Database(
    entities = [
        CustomerEntity::class,
        SaleEntity::class,
        PaymentEntity::class,
        TransitionEntity::class,
        FirstCustomerHistoryEntity::class,
        ProductEntity::class,
        CustomerProductRateEntity::class,
        TableOrderEntity::class,
        ExpenseEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    abstract fun paymentDao(): PaymentDao
    abstract fun routeDao(): RouteDao
    abstract fun productDao(): ProductDao
    abstract fun customerProductRateDao(): CustomerProductRateDao
    abstract fun tableOrderDao(): TableOrderDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sales ADD COLUMN sequenceNumber INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS `transitions` (`fromCustomerId` INTEGER NOT NULL, `toCustomerId` INTEGER NOT NULL, `transitionCount` INTEGER NOT NULL, `weight` REAL NOT NULL, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`fromCustomerId`, `toCustomerId`), FOREIGN KEY(`fromCustomerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`toCustomerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transitions_fromCustomerId` ON `transitions` (`fromCustomerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transitions_toCustomerId` ON `transitions` (`toCustomerId`)")
                
                db.execSQL("CREATE TABLE IF NOT EXISTS `first_customer_history` (`date` INTEGER NOT NULL, `customerId` INTEGER NOT NULL, PRIMARY KEY(`date`), FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_first_customer_history_customerId` ON `first_customer_history` (`customerId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create new tables
                db.execSQL("CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `defaultRate` REAL NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `customer_product_rates` (`customerId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `rate` REAL NOT NULL, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`customerId`, `productId`), FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_product_rates_customerId` ON `customer_product_rates` (`customerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_product_rates_productId` ON `customer_product_rates` (`productId`)")

                // 2. Re-create sales table with new columns and foreign keys
                db.execSQL("CREATE TABLE IF NOT EXISTS `sales_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `customerId` INTEGER NOT NULL, `productId` INTEGER, `productName` TEXT NOT NULL, `date` INTEGER NOT NULL, `quantity` REAL NOT NULL, `rate` REAL NOT NULL, `amount` REAL NOT NULL, `notes` TEXT NOT NULL, `sequenceNumber` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
                db.execSQL("INSERT INTO `sales_new` (`id`, `customerId`, `productId`, `productName`, `date`, `quantity`, `rate`, `amount`, `notes`, `sequenceNumber`, `createdAt`) SELECT `id`, `customerId`, NULL, 'Tea', `date`, `quantity`, `rate`, `amount`, `notes`, `sequenceNumber`, `createdAt` FROM `sales`")
                db.execSQL("DROP TABLE `sales`")
                db.execSQL("ALTER TABLE `sales_new` RENAME TO `sales`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_customerId` ON `sales` (`customerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_productId` ON `sales` (`productId`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `table_orders` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`tableNumber` INTEGER NOT NULL, " +
                    "`productId` INTEGER, " +
                    "`productName` TEXT NOT NULL, " +
                    "`quantity` REAL NOT NULL, " +
                    "`rate` REAL NOT NULL, " +
                    "`amount` REAL NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_table_orders_tableNumber` ON `table_orders` (`tableNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_table_orders_productId` ON `table_orders` (`productId`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `expenses` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`date` INTEGER NOT NULL, " +
                    "`category` TEXT NOT NULL, " +
                    "`description` TEXT NOT NULL, " +
                    "`amount` REAL NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_date` ON `expenses` (`date`)")
            }
        }
    }
}
