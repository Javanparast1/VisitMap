package com.javanparast.visitmap.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "location_history",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["customerId"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["customerId", "changedAt"])
    ]
)
data class LocationHistoryEntity(
    @PrimaryKey
    val historyId: String = UUID.randomUUID().toString(),
    val customerId: String,
    val oldLatitude: Double,
    val oldLongitude: Double,
    val newLatitude: Double,
    val newLongitude: Double,
    val oldAccuracyMeters: Double? = null,
    val newAccuracyMeters: Double? = null,
    val oldProvider: String,
    val newProvider: String,
    val oldCapturedAt: Long,
    val newCapturedAt: Long,
    val oldIsManual: Boolean,
    val newIsManual: Boolean,
    val changedAt: Long,
    val reason: String? = null
)
