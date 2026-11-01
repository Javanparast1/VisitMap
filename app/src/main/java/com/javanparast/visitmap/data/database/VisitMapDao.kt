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

    @Query("SELECT * FROM location_observations WHERE customerId = :customerId ORDER BY capturedAt DESC")
    suspend fun getLocationObservations(customerId: String): List<LocationObservationEntity>

    @Query("SELECT * FROM location_history WHERE customerId = :customerId ORDER BY changedAt DESC")
    fun observeLocationHistory(customerId: String): Flow<List<LocationHistoryEntity>>

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

    @Transaction
    suspend fun registerCustomer(
        customer: CustomerEntity,
        location: CustomerLocationEntity,
        observation: LocationObservationEntity
    ) {
        require(customer.customerId == location.customerId)
        require(customer.customerId == observation.customerId)

        insertCustomer(customer)
        upsertCurrentLocation(location)
        insertLocationObservation(observation)
    }

    @Transaction
    suspend fun updateCurrentLocation(
        location: CustomerLocationEntity,
        observation: LocationObservationEntity,
        history: LocationHistoryEntity
    ) {
        require(location.customerId == observation.customerId)
        require(location.customerId == history.customerId)

        upsertCurrentLocation(location)
        insertLocationObservation(observation)
        insertLocationHistory(history)
    }
}
