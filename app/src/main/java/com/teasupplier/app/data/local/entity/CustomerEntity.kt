package com.teasupplier.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val shopName: String,
    val mobile: String,
    val address: String,
    val area: String,
    val defaultRate: Double,
    val openingBalance: Double,
    val balance: Double,
    val status: Boolean = true, // true = active, false = inactive
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
