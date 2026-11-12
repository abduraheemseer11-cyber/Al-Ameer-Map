package com.scannerwireless.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val key: String,
    val ip: String,
    val mac: String?,
    val name: String?,
    val vendor: String?,
    val firstSeen: Long,
    val lastSeen: Long
)

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices")
    suspend fun getAll(): List<DeviceEntity>

    @Upsert
    suspend fun upsertAll(items: List<DeviceEntity>)
}

@Database(entities = [DeviceEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
}
