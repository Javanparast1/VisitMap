package com.javanparast.visitmap.domain

import com.javanparast.visitmap.data.model.CustomerEntity

data class CustomerInput(
    val name: String,
    val customerType: String,
    val phone: String?,
    val contactName: String?,
    val description: String?
)

sealed interface CustomerValidationResult {
    data class Valid(val input: CustomerInput) : CustomerValidationResult
    data class Invalid(val message: String) : CustomerValidationResult
}

object CustomerValidation {
    fun validate(input: CustomerInput): CustomerValidationResult {
        val name = input.name.trim()
        if (name.isEmpty()) return CustomerValidationResult.Invalid("نام فروشگاه الزامی است.")
        if (input.customerType !in setOf(
                CustomerEntity.TYPE_SUPERMARKET,
                CustomerEntity.TYPE_TOBACCONIST
            )
        ) return CustomerValidationResult.Invalid("نوع فعالیت انتخاب‌شده معتبر نیست.")

        return CustomerValidationResult.Valid(
            input.copy(
                name = name,
                phone = input.phone.normalizedOrNull(),
                contactName = input.contactName.normalizedOrNull(),
                description = input.description.normalizedOrNull()
            )
        )
    }

    private fun String?.normalizedOrNull(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }
}
