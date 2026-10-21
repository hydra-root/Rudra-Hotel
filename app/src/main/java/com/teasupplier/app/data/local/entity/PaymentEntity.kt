package com.teasupplier.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val date: Long,
    val amount: Double,
    val mode: PaymentMode,
    val reference: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class PaymentMode {
    CASH, UPI, BANK, CHEQUE
}
