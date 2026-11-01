package com.javanparast.visitmap.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "location_observations",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["customerId"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["customerId", "capturedAt"]),
        Index(value = ["reviewStatus"])
    ]
)
data class LocationObservationEntity(
    @PrimaryKey
    val observationId: String = UUID.randomUUID().toString(),
    val customerId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double? = null,
    val provider: String,
    val capturedAt: Long,
    val isManual: Boolean,
    val reviewStatus: String = REVIEW_PENDING,
    val reviewedAt: Long? = null
) {
    companion object {
        const val REVIEW_PENDING = "PENDING"
        const val REVIEW_ACCEPTED = "ACCEPTED"
        const val REVIEW_REJECTED = "REJECTED"
    }
}
