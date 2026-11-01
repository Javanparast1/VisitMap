package com.javanparast.visitmap.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.javanparast.visitmap.data.model.CustomerEntity
import com.javanparast.visitmap.data.model.CustomerLocationEntity
import com.javanparast.visitmap.data.model.LocationHistoryEntity
import com.javanparast.visitmap.data.model.LocationObservationEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisitMapDatabaseTest {
    private lateinit var database: VisitMapDatabase
    private lateinit var dao: VisitMapDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            VisitMapDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.visitMapDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun registrationPersistsCustomerCurrentLocationAndObservation() = runBlocking {
        val customer = sampleCustomer()
        val location = sampleLocation(customer.customerId)
        val observation = sampleObservation(customer.customerId, location)

        dao.registerCustomer(customer, location, observation)

        assertEquals(customer, dao.getCustomer(customer.customerId))
        assertEquals(location, dao.getCurrentLocation(customer.customerId))
        assertEquals(
            listOf(observation),
            dao.getLocationObservations(customer.customerId)
        )
    }

    @Test
    fun locationUpdateKeepsObservationAndHistoryAndReplacesCurrentLocation() = runBlocking {
        val customer = sampleCustomer()
        val oldLocation = sampleLocation(customer.customerId)
        val firstObservation = sampleObservation(customer.customerId, oldLocation)
        dao.registerCustomer(customer, oldLocation, firstObservation)

        val newLocation = oldLocation.copy(
            latitude = 37.2,
            longitude = 49.6,
            accuracyMeters = 8.0,
            provider = CustomerLocationEntity.PROVIDER_GNSS,
            capturedAt = 2_000L
        )
        val secondObservation = sampleObservation(customer.customerId, newLocation).copy(
            observationId = "observation-2"
        )
        val history = LocationHistoryEntity(
            historyId = "history-1",
            customerId = customer.customerId,
            oldLatitude = oldLocation.latitude,
            oldLongitude = oldLocation.longitude,
            newLatitude = newLocation.latitude,
            newLongitude = newLocation.longitude,
            oldAccuracyMeters = oldLocation.accuracyMeters,
            newAccuracyMeters = newLocation.accuracyMeters,
            oldProvider = oldLocation.provider,
            newProvider = newLocation.provider,
            oldCapturedAt = oldLocation.capturedAt,
            newCapturedAt = newLocation.capturedAt,
            oldIsManual = oldLocation.isManual,
            newIsManual = newLocation.isManual,
            changedAt = 3_000L,
            reason = "test"
        )

        dao.updateCurrentLocation(newLocation, secondObservation, history)

        assertEquals(newLocation, dao.getCurrentLocation(customer.customerId))
        assertEquals(2, dao.getLocationObservations(customer.customerId).size)
        assertEquals(listOf(history), dao.getLocationHistory(customer.customerId))
    }

    private fun sampleCustomer() = CustomerEntity(
        customerId = "customer-1",
        name = "نمونه آزمایشی",
        customerType = CustomerEntity.TYPE_SUPERMARKET,
        createdAt = 1_000L,
        updatedAt = 1_000L
    )

    private fun sampleLocation(customerId: String) = CustomerLocationEntity(
        customerId = customerId,
        latitude = 37.1,
        longitude = 49.5,
        accuracyMeters = 12.0,
        provider = CustomerLocationEntity.PROVIDER_FUSED,
        capturedAt = 1_000L,
        isManual = false,
        province = "گیلان"
    )

    private fun sampleObservation(
        customerId: String,
        location: CustomerLocationEntity
    ) = LocationObservationEntity(
        observationId = "observation-1",
        customerId = customerId,
        latitude = location.latitude,
        longitude = location.longitude,
        accuracyMeters = location.accuracyMeters,
        provider = location.provider,
        capturedAt = location.capturedAt,
        isManual = location.isManual,
        reviewStatus = LocationObservationEntity.REVIEW_ACCEPTED,
        reviewedAt = 1_500L
    )
}
