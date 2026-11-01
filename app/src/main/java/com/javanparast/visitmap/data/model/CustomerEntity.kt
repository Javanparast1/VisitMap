package com.javanparast.visitmap.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["customerType"]),
        Index(value = ["status"]),
        Index(value = ["contactName"]),
        Index(value = ["phone"])
    ]
)
data class CustomerEntity(
    @PrimaryKey
    val customerId: String = UUID.randomUUID().toString(),
    val name: String,
    val customerType: String,
    val phone: String? = null,
    val contactName: String? = null,
    val status: String = STATUS_ACTIVE,
    val description: String? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val TYPE_SUPERMARKET = "SUPERMARKET"
        const val TYPE_TOBACCONIST = "TOBACCONIST"

        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_INACTIVE = "INACTIVE"
    }
}
