package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PairingConfig::class, TrackerLog::class], version = 3, exportSchema = false)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao

    companion object {
        @Volatile
        private var INSTANCE: TrackerDatabase? = null

        fun getDatabase(context: Context): TrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val storageContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    val deviceContext = appContext.createDeviceProtectedStorageContext()
                    // Automatically migrate database from credential-protected to device-protected storage if it exists there
                    if (!deviceContext.databaseList().contains("tracker_database") && 
                        appContext.databaseList().contains("tracker_database")) {
                        try {
                            deviceContext.moveDatabaseFrom(appContext, "tracker_database")
                        } catch (e: Exception) {
                            android.util.Log.e("TrackerDatabase", "Failed to migrate database to device-protected storage", e)
                        }
                    }
                    deviceContext
                } else {
                    appContext
                }

                val instance = Room.databaseBuilder(
                    storageContext,
                    TrackerDatabase::class.java,
                    "tracker_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
