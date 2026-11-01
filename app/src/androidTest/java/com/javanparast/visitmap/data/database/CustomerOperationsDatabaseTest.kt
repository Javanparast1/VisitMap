package com.javanparast.visitmap.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.javanparast.visitmap.data.model.CustomerEntity
import com.javanparast.visitmap.data.repository.CustomerRepository
import com.javanparast.visitmap.domain.CustomerInput
import com.javanparast.visitmap.ui.CustomerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomerOperationsDatabaseTest {
    private lateinit var database: VisitMapDatabase
    private lateinit var dao: VisitMapDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, VisitMapDatabase::class.java)
            .allowMainThreadQueries().build()
        dao = database.visitMapDao()
    }

    @After
    fun tearDown() { database.close() }

    @Test
    fun registersCustomerWithoutAnyLocation() = runBlocking {
        val customer = sampleCustomer()
        dao.registerCustomer(customer)
        assertEquals(customer, dao.getCustomer(customer.customerId))
        assertNull(dao.getCurrentLocation(customer.customerId))
    }

    @Test
    fun inactiveCustomerRemainsInListAndUnifiedSearch() = runBlocking {
        val customer = sampleCustomer().copy(
            status = CustomerEntity.STATUS_INACTIVE,
            contactName = "مسئول آزمایشی",
            phone = "09120000000"
        )
        dao.registerCustomer(customer)
        assertEquals(listOf(customer), dao.observeCustomers().first())
        assertEquals(listOf(customer), dao.searchCustomers("مسئول آزمایشی").first())
        assertEquals(listOf(customer), dao.searchCustomers("09120000000").first())
    }

    @Test
    fun repeatedSaveCallsDuringOneRegistrationCreateOnlyOneCustomer() = runBlocking {
        val viewModel = CustomerViewModel(CustomerRepository(dao))
        val input = CustomerInput(
            name = "فروشگاه یکتا",
            customerType = CustomerEntity.TYPE_SUPERMARKET,
            phone = null,
            contactName = null,
            description = null
        )

        viewModel.save(input)
        viewModel.save(input)

        withTimeout(5_000L) {
            viewModel.state.filter { !it.isBusy && it.saveCompleted }.first()
        }
        assertEquals(1, dao.observeCustomers().first().size)
        assertEquals("فروشگاه یکتا", dao.observeCustomers().first().single().name)
    }

    @Test
    fun editingPreservesCustomerIdentity() = runBlocking {
        val original = sampleCustomer()
        dao.registerCustomer(original)
        val edited = original.copy(name = "نام تازه", updatedAt = 2_000L)
        assertEquals(1, dao.updateCustomer(edited))
        assertEquals(edited, dao.getCustomer(original.customerId))
    }

    private fun sampleCustomer() = CustomerEntity(
        customerId = "customer-operations-test",
        name = "فروشگاه آزمایشی",
        customerType = CustomerEntity.TYPE_SUPERMARKET,
        createdAt = 1_000L,
        updatedAt = 1_000L
    )
}
