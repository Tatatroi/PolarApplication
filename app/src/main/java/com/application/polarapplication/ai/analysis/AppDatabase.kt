package com.application.polarapplication.ai.analysis

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.application.polarapplication.ai.database.SessionDao
import com.application.polarapplication.model.TrainingSessionEntity

@TypeConverters(Converters::class)
@Database(entities = [TrainingSessionEntity::class], version = 3)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE training_sessions ADD COLUMN durationSeconds INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE training_sessions ADD COLUMN activityType TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE training_sessions ADD COLUMN sessionGoal TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE training_sessions ADD COLUMN focusArea TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE training_sessions ADD COLUMN rpe INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "polar_manager_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
