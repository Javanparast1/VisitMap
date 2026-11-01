package com.javanparast.visitmap.data.repository

import com.javanparast.visitmap.data.database.VisitMapDao
import com.javanparast.visitmap.data.model.CustomerEntity
import com.javanparast.visitmap.data.model.CustomerLocationEntity
import com.javanparast.visitmap.data.model.LocationHistoryEntity
import com.javanparast.visitmap.data.model.LocationObservationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Local persistence boundary for customer records.
 *
 * Location proposals are persisted separately from approved current locations.
 * A proposal becomes current only through explicit acceptance.
 */
class CustomerRepository(
    private val dao: VisitMapDao
) {
    fun observeCustomers(): Flow<List<CustomerEntity>> = dao.observeCustomers()

    fun searchCustomers(query: String): Flow<List<CustomerEntity>> =
        dao.searchCustomers(query.trim())

    suspend fun getCustomer(customerId: String): CustomerEntity? =
        dao.getCustomer(customerId)

    suspend fun getCurrentLocation(customerId: String): CustomerLocationEntity? =
        dao.getCurrentLocation(customerId)

    suspend fun registerCustomer(
        customer: CustomerEntity,
        initialObservation: LocationObservationEntity
    ) = dao.registerCustomer(customer, initialObservation)

    suspend fun updateCustomer(customer: CustomerEntity): Int =
        dao.updateCustomer(customer)

    suspend fun getLocationObservations(customerId: String): List<LocationObservationEntity> =
        dao.getLocationObservations(customerId)

    suspend fun recordLocationObservation(
        observation: LocationObservationEntity
    ) = dao.recordLocationObservation(observation)

    suspend fun recordManualCorrection(
        sourceObservationId: String,
        manualProposal: LocationObservationEntity,
        rejectedAt: Long
    ): Int = dao.recordManualCorrection(
        sourceObservationId = sourceObservationId,
        manualProposal = manualProposal,
        rejectedAt = rejectedAt
    )

    suspend fun acceptLocationObservation(
        observationId: String,
        reviewedAt: Long,
        acceptedLocation: CustomerLocationEntity,
        history: LocationHistoryEntity? = null
    ): Int = dao.acceptLocationObservation(
        observationId = observationId,
        reviewedAt = reviewedAt,
        acceptedLocation = acceptedLocation,
        history = history
    )

    suspend fun rejectLocationObservation(
        observationId: String,
        reviewedAt: Long
    ): Int = dao.rejectLocationObservation(observationId, reviewedAt)

    fun observeLocationHistory(customerId: String): Flow<List<LocationHistoryEntity>> =
        dao.observeLocationHistory(customerId)
}
