package com.javanparast.visitmap.domain

import com.javanparast.visitmap.data.model.CustomerEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerValidationTest {
    @Test
    fun trimsRequiredAndOptionalFields() {
        val result = CustomerValidation.validate(
            CustomerInput("  فروشگاه نمونه  ", CustomerEntity.TYPE_SUPERMARKET, "  0912  ", "  علی ", " توضیح ")
        )
        assertTrue(result is CustomerValidationResult.Valid)
        val input = (result as CustomerValidationResult.Valid).input
        assertEquals("فروشگاه نمونه", input.name)
        assertEquals("0912", input.phone)
        assertEquals("علی", input.contactName)
        assertEquals("توضیح", input.description)
    }

    @Test
    fun rejectsBlankName() {
        val result = CustomerValidation.validate(
            CustomerInput("  ", CustomerEntity.TYPE_SUPERMARKET, null, null, null)
        )
        assertEquals("نام فروشگاه الزامی است.", (result as CustomerValidationResult.Invalid).message)
    }

    @Test
    fun rejectsUnknownCustomerType() {
        val result = CustomerValidation.validate(
            CustomerInput("فروشگاه", "UNKNOWN", null, null, null)
        )
        assertTrue(result is CustomerValidationResult.Invalid)
    }

    @Test
    fun convertsBlankOptionalFieldsToNull() {
        val result = CustomerValidation.validate(
            CustomerInput("فروشگاه", CustomerEntity.TYPE_TOBACCONIST, " ", "", "  ")
        ) as CustomerValidationResult.Valid
        assertEquals(null, result.input.phone)
        assertEquals(null, result.input.contactName)
        assertEquals(null, result.input.description)
    }
}
