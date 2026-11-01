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
 * Domain validation and location-quality decisions belong in the domain layer,
 * not in this storage adapter.
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
        location: CustomerLocationEntity,
        observation: LocationObservationEntity
    ) = dao.registerCustomer(customer, location, observation)

    suspend fun updateCustomer(customer: CustomerEntity) =
        dao.updateCustomer(customer)

    suspend fun updateCurrentLocation(
        location: CustomerLocationEntity,
        observation: LocationObservationEntity,
        history: LocationHistoryEntity
    ) = dao.updateCurrentLocation(location, observation, history)

    suspend fun getLocationObservations(customerId: String): List<LocationObservationEntity> =
        dao.getLocationObservations(customerId)

    suspend fun recordLocationObservation(
        observation: LocationObservationEntity
    ) = dao.recordLocationObservation(observation)

    suspend fun updateObservationReview(
        observationId: String,
        reviewStatus: String,
        reviewedAt: Long?
    ): Int = dao.updateObservationReview(observationId, reviewStatus, reviewedAt)

    fun observeLocationHistory(customerId: String): Flow<List<LocationHistoryEntity>> =
        dao.observeLocationHistory(customerId)
}
