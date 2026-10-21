package com.teasupplier.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "transitions",
    primaryKeys = ["fromCustomerId", "toCustomerId"],
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromCustomerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["toCustomerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("fromCustomerId"),
        Index("toCustomerId")
    ]
)
data class TransitionEntity(
    val fromCustomerId: Long,
    val toCustomerId: Long,
    val transitionCount: Int,
    val weight: Double,
    val lastUpdated: Long
)
