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
                val instance = Room.databaseBuilder(
                    context.applicationContext,
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
