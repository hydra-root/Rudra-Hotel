package com.teasupplier.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long = System.currentTimeMillis(),       // epoch millis of the expense day
    val category: String,                              // e.g. "Milk", "Gas", "Salary", "Maintenance"
    val description: String = "",                      // optional free-text note
    val amount: Double,
    val createdAt: Long = System.currentTimeMillis()
)
