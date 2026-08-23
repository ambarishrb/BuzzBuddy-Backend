package com.ambrxsh.buzzbuddy.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ambrxsh.buzzbuddy.model.SmartAlarm

@Database(entities = [SmartAlarm::class], version = 2)
abstract class SmartAlarmsDatabase : RoomDatabase() {

    abstract fun smartAlarmDao(): smartAlarmDao

    companion object {
        @Volatile
        private var instance: SmartAlarmsDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE smart_alarms ADD COLUMN serverId INTEGER")
            }
        }

        fun getDatabase(context: Context): SmartAlarmsDatabase {
            return instance ?: synchronized(this) {
                val tempInstance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartAlarmsDatabase::class.java,
                    "smart_alarms"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                instance = tempInstance
                tempInstance
            }
        }
    }
}
