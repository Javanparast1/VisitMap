package com.javanparast.visitmap.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.javanparast.visitmap.data.model.CustomerEntity
import com.javanparast.visitmap.data.model.CustomerLocationEntity
import com.javanparast.visitmap.data.model.LocationHistoryEntity
import com.javanparast.visitmap.data.model.LocationObservationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitMapDao {
    @Query("SELECT * FROM customers ORDER BY updatedAt DESC")
    fun observeCustomers(): Flow<List<CustomerEntity>>

    @Query(
        """
        SELECT * FROM customers
        WHERE name LIKE '%' || :query || '%' COLLATE NOCASE
           OR COALESCE(contactName, '') LIKE '%' || :query || '%' COLLATE NOCASE
           OR COALESCE(phone, '') LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY updatedAt DESC
        """
    )
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE customerId = :customerId LIMIT 1")
    suspend fun getCustomer(customerId: String): CustomerEntity?

    @Query("SELECT * FROM customer_locations WHERE customerId = :customerId LIMIT 1")
    suspend fun getCurrentLocation(customerId: String): CustomerLocationEntity?

    @Query("SELECT * FROM location_observations WHERE observationId = :observationId LIMIT 1")
    suspend fun getLocationObservation(observationId: String): LocationObservationEntity?

    @Query("SELECT * FROM location_observations WHERE customerId = :customerId ORDER BY capturedAt DESC")
    suspend fun getLocationObservations(customerId: String): List<LocationObservationEntity>

    @Query("SELECT * FROM location_history WHERE customerId = :customerId ORDER BY changedAt DESC")
    fun observeLocationHistory(customerId: String): Flow<List<LocationHistoryEntity>>

    @Query("SELECT * FROM location_history WHERE customerId = :customerId ORDER BY changedAt DESC")
    suspend fun getLocationHistory(customerId: String): List<LocationHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Upsert
    suspend fun upsertCurrentLocation(location: CustomerLocationEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLocationObservation(observation: LocationObservationEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLocationHistory(history: LocationHistoryEntity)

    @Query(
        """
        UPDATE location_observations
        SET reviewStatus = 'ACCEPTED', reviewedAt = :reviewedAt
        WHERE observationId = :observationId
          AND reviewStatus = 'PENDING'
          AND reviewedAt IS NULL
        """
    )
    suspend fun markObservationAccepted(observationId: String, reviewedAt: Long): Int

    @Query(
        """
        UPDATE location_observations
        SET reviewStatus = 'REJECTED', reviewedAt = :reviewedAt
        WHERE observationId = :observationId
          AND reviewStatus = 'PENDING'
          AND reviewedAt IS NULL
        """
    )
    suspend fun markObservationRejected(observationId: String, reviewedAt: Long): Int

    /**
     * A new customer is persisted independently from its location.
     * Its first location remains a pending proposal until the user accepts it.
     */
    @Transaction
    suspend fun registerCustomer(
        customer: CustomerEntity,
        initialObservation: LocationObservationEntity
    ) {
        require(customer.customerId == initialObservation.customerId)
        require(initialObservation.reviewStatus == LocationObservationEntity.REVIEW_PENDING)
        require(initialObservation.reviewedAt == null)

        insertCustomer(customer)
        insertLocationObservation(initialObservation)
    }

    /**
     * Stores one pending location proposal for an existing customer.
     * The current approved location is not changed by recording a proposal.
     */
    suspend fun recordLocationObservation(observation: LocationObservationEntity) {
        require(observation.reviewStatus == LocationObservationEntity.REVIEW_PENDING)
        require(observation.reviewedAt == null)
        insertLocationObservation(observation)
    }

    /**
     * Accepts an already-persisted proposal and promotes it to the current location.
     * The first accepted location has no history row; replacements must append history.
     */
    @Transaction
    suspend fun acceptLocationObservation(
        observationId: String,
        reviewedAt: Long,
        acceptedLocation: CustomerLocationEntity,
        history: LocationHistoryEntity? = null
    ): Int {
        val observation = getLocationObservation(observationId) ?: return 0
        if (
            observation.reviewStatus != LocationObservationEntity.REVIEW_PENDING ||
            observation.reviewedAt != null
        ) {
            return 0
        }

        require(observation.customerId == acceptedLocation.customerId)
        require(observation.matches(acceptedLocation))

        val previous = getCurrentLocation(observation.customerId)
        if (previous == null) {
            require(history == null)
        } else {
            require(history != null)
            require(history.customerId == observation.customerId)
            require(history.matchesPrevious(previous))
            require(history.matchesNew(acceptedLocation))
        }

        val updated = markObservationAccepted(observationId, reviewedAt)
        if (updated != 1) return 0

        upsertCurrentLocation(acceptedLocation)
        if (history != null) {
            insertLocationHistory(history)
        }
        return 1
    }

    /**
     * Rejects a pending proposal without changing the current approved location.
     * Accepted and rejected observations are terminal.
     */
    suspend fun rejectLocationObservation(
        observationId: String,
        reviewedAt: Long
    ): Int = markObservationRejected(observationId, reviewedAt)

    private fun LocationObservationEntity.matches(
        location: CustomerLocationEntity
    ): Boolean =
        latitude == location.latitude &&
            longitude == location.longitude &&
            accuracyMeters == location.accuracyMeters &&
            provider == location.provider &&
            capturedAt == location.capturedAt &&
            isManual == location.isManual

    private fun LocationHistoryEntity.matchesPrevious(
        location: CustomerLocationEntity
    ): Boolean =
        oldLatitude == location.latitude &&
            oldLongitude == location.longitude &&
            oldAccuracyMeters == location.accuracyMeters &&
            oldProvider == location.provider &&
            oldCapturedAt == location.capturedAt &&
            oldIsManual == location.isManual

    private fun LocationHistoryEntity.matchesNew(
        location: CustomerLocationEntity
    ): Boolean =
        newLatitude == location.latitude &&
            newLongitude == location.longitude &&
            newAccuracyMeters == location.accuracyMeters &&
            newProvider == location.provider &&
            newCapturedAt == location.capturedAt &&
            newIsManual == location.isManual
}
