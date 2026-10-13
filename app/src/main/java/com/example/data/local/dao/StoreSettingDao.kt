package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.StoreSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreSettingDao {
    @Query("SELECT * FROM store_settings WHERE id = 1 LIMIT 1")
    fun getStoreSettings(): Flow<StoreSettingEntity?>

    @Query("SELECT * FROM store_settings WHERE id = 1 LIMIT 1")
    suspend fun getStoreSettingsDirect(): StoreSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: StoreSettingEntity)

    @Update
    suspend fun updateSettings(settings: StoreSettingEntity)

    @Query("UPDATE store_settings SET lastSyncTimestamp = :timestamp WHERE id = 1")
    suspend fun updateLastSyncTime(timestamp: Long)

    @Query("DELETE FROM store_settings")
    suspend fun clearStoreSettings()
}
