package com.shame.tracker.di

import android.content.Context
import androidx.room.Room
import com.shame.tracker.data.db.AppDatabase
import com.shame.tracker.data.db.dao.GpsPointDao
import com.shame.tracker.data.db.dao.LapDao
import com.shame.tracker.data.db.dao.SessionDao
import com.shame.tracker.data.db.dao.SessionWithLapsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "shame_tracker.db"
        ).build()

    @Provides
    fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideLapDao(db: AppDatabase): LapDao = db.lapDao()

    @Provides
    fun provideGpsPointDao(db: AppDatabase): GpsPointDao = db.gpsPointDao()

    @Provides
    fun provideSessionWithLapsDao(db: AppDatabase): SessionWithLapsDao = db.sessionWithLapsDao()
}
