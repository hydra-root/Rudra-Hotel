package com.teasupplier.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val defaultRate: Double,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
