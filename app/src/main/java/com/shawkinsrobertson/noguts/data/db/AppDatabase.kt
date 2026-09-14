package com.shawkinsrobertson.noguts.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shawkinsrobertson.noguts.data.db.dao.DailyLogDao
import com.shawkinsrobertson.noguts.data.db.dao.DailyLogFactorDao
import com.shawkinsrobertson.noguts.data.db.dao.FactorDao
import com.shawkinsrobertson.noguts.data.db.dao.PendingNotificationLogDao
import com.shawkinsrobertson.noguts.data.db.dao.ScoreSnapshotDao
import com.shawkinsrobertson.noguts.data.db.entity.DailyLogEntity
import com.shawkinsrobertson.noguts.data.db.entity.DailyLogFactorEntity
import com.shawkinsrobertson.noguts.data.db.entity.FactorEntity
import com.shawkinsrobertson.noguts.data.db.entity.PendingNotificationLogEntity
import com.shawkinsrobertson.noguts.data.db.entity.ScoreSnapshotEntity

@Database(
    entities = [
        FactorEntity::class,
        DailyLogEntity::class,
        DailyLogFactorEntity::class,
        ScoreSnapshotEntity::class,
        PendingNotificationLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun factorDao(): FactorDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun dailyLogFactorDao(): DailyLogFactorDao
    abstract fun scoreSnapshotDao(): ScoreSnapshotDao
    abstract fun pendingNotificationLogDao(): PendingNotificationLogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "no-guts-no-glory.db"
                ).build().also { instance = it }
            }
    }
}
