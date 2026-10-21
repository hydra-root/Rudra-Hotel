package com.teasupplier.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "table_orders",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["tableNumber"]),
        Index(value = ["productId"])
    ]
)
data class TableOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableNumber: Int,
    val productId: Long? = null,
    val productName: String,
    val quantity: Double,
    val rate: Double,
    val amount: Double,
    val createdAt: Long = System.currentTimeMillis()
)
