package com.application.polarapplication.ai.analysis

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.application.polarapplication.ai.database.SessionDao
import com.application.polarapplication.model.TrainingSessionEntity

// Aici îi spunem bazei de date ce tabele conține și ce versiune are
@TypeConverters(Converters::class)
@Database(entities = [TrainingSessionEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Dacă baza de date există deja, o returnăm, dacă nu, o creăm
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "polar_manager_db" // Numele fișierului bazei de date
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
