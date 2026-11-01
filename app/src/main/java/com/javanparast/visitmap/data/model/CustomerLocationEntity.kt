package com.javanparast.visitmap.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "customer_locations",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["customerId"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class CustomerLocationEntity(
    @PrimaryKey
    val customerId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double? = null,
    val provider: String,
    val capturedAt: Long,
    val isManual: Boolean,
    val province: String? = null,
    val county: String? = null,
    val city: String? = null,
    val neighborhood: String? = null
) {
    companion object {
        const val PROVIDER_FUSED = "FUSED"
        const val PROVIDER_GNSS = "GNSS"
        const val PROVIDER_NETWORK = "NETWORK"
        const val PROVIDER_MANUAL = "MANUAL"
        const val PROVIDER_UNKNOWN = "UNKNOWN"
    }
}
