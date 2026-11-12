package com.scannerwireless.app.di

import android.content.Context
import androidx.room.Room
import com.scannerwireless.app.data.local.AppDatabase
import com.scannerwireless.app.data.local.DeviceDao
import com.scannerwireless.app.data.repository.DeviceRepositoryImpl
import com.scannerwireless.app.data.repository.SpeedTestRepositoryImpl
import com.scannerwireless.app.domain.repository.DeviceRepository
import com.scannerwireless.app.domain.repository.SpeedTestRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppProvidesModule {
    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "scanner.db").build()

    @Provides
    fun provideDeviceDao(db: AppDatabase): DeviceDao = db.deviceDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindSpeedRepository(impl: SpeedTestRepositoryImpl): SpeedTestRepository
}
