package com.rexaps.rexwarp.data
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RexWarpUsageEntity::class], version = 1, exportSchema = true)
abstract class RexWarpDatabase : RoomDatabase() {
    abstract fun usageDao(): RexWarpUsageDao

    companion object {
        @Volatile private var instance: RexWarpDatabase? = null
        fun get(context: Context): RexWarpDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, RexWarpDatabase::class.java, "rexwarp.db")
                .build().also { instance = it }
        }
    }
}