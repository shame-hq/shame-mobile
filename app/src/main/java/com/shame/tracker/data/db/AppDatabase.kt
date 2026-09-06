package com.shame.tracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shame.tracker.data.db.dao.GpsPointDao
import com.shame.tracker.data.db.dao.LapDao
import com.shame.tracker.data.db.dao.SessionDao
import com.shame.tracker.data.db.dao.SessionWithLapsDao
import com.shame.tracker.data.db.entity.GpsPoint
import com.shame.tracker.data.db.entity.Lap
import com.shame.tracker.data.db.entity.Session

@Database(
    entities = [Session::class, Lap::class, GpsPoint::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun lapDao(): LapDao
    abstract fun gpsPointDao(): GpsPointDao
    abstract fun sessionWithLapsDao(): SessionWithLapsDao
}
