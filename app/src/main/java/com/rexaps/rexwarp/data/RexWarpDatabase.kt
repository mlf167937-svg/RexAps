package com.rexaps.rexwarp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RexWarpUsageEntity::class, RexWarpMinuteEntity::class, RexWarpSpeedTestEntity::class],
    version = 3,
    exportSchema = true
)
abstract class RexWarpDatabase : RoomDatabase() {
    abstract fun usageDao(): RexWarpUsageDao
    abstract fun speedTestDao(): RexWarpSpeedTestDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `speed_test_history` (" +
                        "`id` TEXT NOT NULL, `startedAtEpochMs` INTEGER NOT NULL, " +
                        "`completedAtEpochMs` INTEGER NOT NULL, `downloadMbps` REAL, " +
                        "`uploadMbps` REAL, `latencyMs` REAL, `jitterMs` REAL, " +
                        "`durationMs` INTEGER, `vpnConnected` INTEGER, `isSuccessful` INTEGER NOT NULL, " +
                        "`errorSummary` TEXT, `source` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_speed_test_history_completedAtEpochMs` " +
                        "ON `speed_test_history` (`completedAtEpochMs`)"
                )
            }
        }

        @Volatile private var instance: RexWarpDatabase? = null
        fun get(context: Context): RexWarpDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, RexWarpDatabase::class.java, "rexwarp.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { instance = it }
        }
    }
}
