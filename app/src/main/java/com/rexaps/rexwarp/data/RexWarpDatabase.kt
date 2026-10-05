package com.rexaps.rexwarp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RexWarpUsageEntity::class, RexWarpMinuteEntity::class],
    version = 2,
    exportSchema = true
)
abstract class RexWarpDatabase : RoomDatabase() {
    abstract fun usageDao(): RexWarpUsageDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `usage_minute` (`minute` INTEGER NOT NULL, " +
                        "`downloadBytes` INTEGER NOT NULL, `uploadBytes` INTEGER NOT NULL, " +
                        "`warp` INTEGER NOT NULL, PRIMARY KEY(`minute`))"
                )
            }
        }

        @Volatile private var instance: RexWarpDatabase? = null
        fun get(context: Context): RexWarpDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, RexWarpDatabase::class.java, "rexwarp.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
        }
    }
}
