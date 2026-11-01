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
import org.junit.Assert.assertNull
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
    fun registrationPersistsCustomerAndPendingProposalWithoutCurrentLocation() = runBlocking {
        val customer = sampleCustomer()
        val proposal = sampleObservation(customer.customerId, sampleLocation(customer.customerId))

        dao.registerCustomer(customer, proposal)

        assertEquals(customer, dao.getCustomer(customer.customerId))
        assertNull(dao.getCurrentLocation(customer.customerId))
        assertEquals(
            listOf(proposal),
            dao.getLocationObservations(customer.customerId)
        )
        assertEquals(LocationObservationEntity.REVIEW_PENDING, proposal.reviewStatus)
        assertNull(proposal.reviewedAt)
    }

    @Test
    fun acceptingFirstProposalCreatesCurrentLocationWithoutHistory() = runBlocking {
        val customer = sampleCustomer()
        val location = sampleLocation(customer.customerId)
        val proposal = sampleObservation(customer.customerId, location)
        dao.registerCustomer(customer, proposal)

        val accepted = dao.acceptLocationObservation(
            observationId = proposal.observationId,
            reviewedAt = 1_500L,
            acceptedLocation = location
        )

        assertEquals(1, accepted)
        assertEquals(location, dao.getCurrentLocation(customer.customerId))
        assertEquals(emptyList<LocationHistoryEntity>(), dao.getLocationHistory(customer.customerId))
        val storedProposal = dao.getLocationObservation(proposal.observationId)
        assertEquals(LocationObservationEntity.REVIEW_ACCEPTED, storedProposal?.reviewStatus)
        assertEquals(1_500L, storedProposal?.reviewedAt)
    }

    @Test
    fun rejectingFirstProposalLeavesCustomerWithoutCurrentLocation() = runBlocking {
        val customer = sampleCustomer()
        val proposal = sampleObservation(customer.customerId, sampleLocation(customer.customerId))
        dao.registerCustomer(customer, proposal)

        assertEquals(1, dao.rejectLocationObservation(proposal.observationId, 1_600L))

        assertNull(dao.getCurrentLocation(customer.customerId))
        val storedProposal = dao.getLocationObservation(proposal.observationId)
        assertEquals(LocationObservationEntity.REVIEW_REJECTED, storedProposal?.reviewStatus)
        assertEquals(1_600L, storedProposal?.reviewedAt)
        assertEquals(0, dao.acceptLocationObservation(
            proposal.observationId,
            1_700L,
            sampleLocation(customer.customerId)
        ))
    }

    @Test
    fun manualCorrectionRetainsGpsProposalAndRequiresItsOwnAcceptance() = runBlocking {
        val customer = sampleCustomer()
        val gpsLocation = sampleLocation(customer.customerId)
        val gpsProposal = sampleObservation(customer.customerId, gpsLocation)
        dao.registerCustomer(customer, gpsProposal)

        val manualLocation = gpsLocation.copy(
            latitude = 37.25,
            longitude = 49.65,
            accuracyMeters = null,
            provider = CustomerLocationEntity.PROVIDER_MANUAL,
            capturedAt = 2_000L,
            isManual = true
        )
        val manualProposal = sampleObservation(customer.customerId, manualLocation).copy(
            observationId = "manual-observation-1"
        )

        assertEquals(
            1,
            dao.recordManualCorrection(
                sourceObservationId = gpsProposal.observationId,
                manualProposal = manualProposal,
                rejectedAt = 1_800L
            )
        )

        assertNull(dao.getCurrentLocation(customer.customerId))
        assertEquals(
            LocationObservationEntity.REVIEW_REJECTED,
            dao.getLocationObservation(gpsProposal.observationId)?.reviewStatus
        )
        assertEquals(
            LocationObservationEntity.REVIEW_PENDING,
            dao.getLocationObservation(manualProposal.observationId)?.reviewStatus
        )

        assertEquals(
            1,
            dao.acceptLocationObservation(
                manualProposal.observationId,
                2_500L,
                manualLocation
            )
        )
        assertEquals(manualLocation, dao.getCurrentLocation(customer.customerId))
    }

    @Test
    fun manualCorrectionCannotUseAnotherManualProposalAsItsSource() = runBlocking {
        val customer = sampleCustomer()
        val manualLocation = sampleLocation(customer.customerId).copy(
            provider = CustomerLocationEntity.PROVIDER_MANUAL,
            isManual = true
        )
        val source = sampleObservation(customer.customerId, manualLocation)
        dao.registerCustomer(customer, source)

        val anotherManualProposal = sampleObservation(customer.customerId, manualLocation).copy(
            observationId = "manual-observation-2",
            capturedAt = 2_000L
        )

        var rejected = false
        try {
            dao.recordManualCorrection(
                sourceObservationId = source.observationId,
                manualProposal = anotherManualProposal,
                rejectedAt = 2_100L
            )
        } catch (_: IllegalArgumentException) {
            rejected = true
        }

        assertEquals(true, rejected)
        assertEquals(
            LocationObservationEntity.REVIEW_PENDING,
            dao.getLocationObservation(source.observationId)?.reviewStatus
        )
        assertNull(dao.getLocationObservation(anotherManualProposal.observationId))
    }

    @Test
    fun invalidReplacementHistoryRollsBackAcceptanceAndLocationUpdate() = runBlocking {
        val customer = sampleCustomer()
        val oldLocation = sampleLocation(customer.customerId)
        val firstProposal = sampleObservation(customer.customerId, oldLocation)
        dao.registerCustomer(customer, firstProposal)
        dao.acceptLocationObservation(firstProposal.observationId, 1_500L, oldLocation)

        val newLocation = oldLocation.copy(
            latitude = 37.2,
            longitude = 49.6,
            capturedAt = 2_000L
        )
        val secondProposal = sampleObservation(customer.customerId, newLocation).copy(
            observationId = "observation-rollback"
        )
        dao.recordLocationObservation(secondProposal)
        val invalidHistory = sampleHistory(customer.customerId, oldLocation, newLocation).copy(
            oldLatitude = 0.0
        )

        var rejected = false
        try {
            dao.acceptLocationObservation(
                secondProposal.observationId,
                2_500L,
                newLocation,
                invalidHistory
            )
        } catch (_: IllegalArgumentException) {
            rejected = true
        }

        assertEquals(true, rejected)
        assertEquals(oldLocation, dao.getCurrentLocation(customer.customerId))
        assertEquals(
            LocationObservationEntity.REVIEW_PENDING,
            dao.getLocationObservation(secondProposal.observationId)?.reviewStatus
        )
        assertEquals(emptyList<LocationHistoryEntity>(), dao.getLocationHistory(customer.customerId))
    }

    @Test
    fun pendingReplacementDoesNotChangeCurrentLocationUntilAccepted() = runBlocking {
        val customer = sampleCustomer()
        val oldLocation = sampleLocation(customer.customerId)
        val firstProposal = sampleObservation(customer.customerId, oldLocation)
        dao.registerCustomer(customer, firstProposal)
        dao.acceptLocationObservation(firstProposal.observationId, 1_500L, oldLocation)

        val newLocation = oldLocation.copy(
            latitude = 37.2,
            longitude = 49.6,
            accuracyMeters = 8.0,
            provider = CustomerLocationEntity.PROVIDER_GNSS,
            capturedAt = 2_000L
        )
        val secondProposal = sampleObservation(customer.customerId, newLocation).copy(
            observationId = "observation-2"
        )
        dao.recordLocationObservation(secondProposal)

        assertEquals(oldLocation, dao.getCurrentLocation(customer.customerId))
        assertEquals(LocationObservationEntity.REVIEW_PENDING,
            dao.getLocationObservation(secondProposal.observationId)?.reviewStatus)

        val history = sampleHistory(customer.customerId, oldLocation, newLocation)
        assertEquals(
            1,
            dao.acceptLocationObservation(
                secondProposal.observationId,
                2_500L,
                newLocation,
                history
            )
        )

        assertEquals(newLocation, dao.getCurrentLocation(customer.customerId))
        assertEquals(listOf(history), dao.getLocationHistory(customer.customerId))
        assertEquals(2, dao.getLocationObservations(customer.customerId).size)
        assertEquals(
            LocationObservationEntity.REVIEW_ACCEPTED,
            dao.getLocationObservation(secondProposal.observationId)?.reviewStatus
        )
    }

    @Test
    fun rejectingReplacementPreservesCurrentLocationAndDoesNotCreateHistory() = runBlocking {
        val customer = sampleCustomer()
        val oldLocation = sampleLocation(customer.customerId)
        val firstProposal = sampleObservation(customer.customerId, oldLocation)
        dao.registerCustomer(customer, firstProposal)
        dao.acceptLocationObservation(firstProposal.observationId, 1_500L, oldLocation)

        val newLocation = oldLocation.copy(
            latitude = 37.2,
            longitude = 49.6,
            capturedAt = 2_000L
        )
        val secondProposal = sampleObservation(customer.customerId, newLocation).copy(
            observationId = "observation-2"
        )
        dao.recordLocationObservation(secondProposal)

        assertEquals(1, dao.rejectLocationObservation(secondProposal.observationId, 2_500L))

        assertEquals(oldLocation, dao.getCurrentLocation(customer.customerId))
        assertEquals(emptyList<LocationHistoryEntity>(), dao.getLocationHistory(customer.customerId))
        assertEquals(
            LocationObservationEntity.REVIEW_REJECTED,
            dao.getLocationObservation(secondProposal.observationId)?.reviewStatus
        )
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
        isManual = location.isManual
    )

    private fun sampleHistory(
        customerId: String,
        oldLocation: CustomerLocationEntity,
        newLocation: CustomerLocationEntity
    ) = LocationHistoryEntity(
        historyId = "history-1",
        customerId = customerId,
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
}
