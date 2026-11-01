package com.javanparast.visitmap.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.javanparast.visitmap.data.model.CustomerEntity
import com.javanparast.visitmap.data.model.CustomerLocationEntity
import com.javanparast.visitmap.data.model.LocationHistoryEntity
import com.javanparast.visitmap.data.model.LocationObservationEntity

@Database(
    entities = [
        CustomerEntity::class,
        CustomerLocationEntity::class,
        LocationObservationEntity::class,
        LocationHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class VisitMapDatabase : RoomDatabase() {
    abstract fun visitMapDao(): VisitMapDao

    companion object {
        private const val DATABASE_NAME = "visitmap.db"

        @Volatile
        private var instance: VisitMapDatabase? = null

        fun getInstance(context: Context): VisitMapDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VisitMapDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
        }
    }
}
