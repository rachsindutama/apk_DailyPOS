package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PromotionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PromotionDao {
    @Query("SELECT * FROM promotions ORDER BY id DESC")
    fun getAllPromotions(): Flow<List<PromotionEntity>>

    @Query("SELECT * FROM promotions WHERE isActive = 1")
    fun getActivePromotions(): Flow<List<PromotionEntity>>

    @Query("SELECT * FROM promotions WHERE isActive = 1")
    suspend fun getActivePromotionsList(): List<PromotionEntity>

    @Query("SELECT * FROM promotions ORDER BY id DESC")
    suspend fun getAllPromotionsDirect(): List<PromotionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromotion(promotion: PromotionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromotions(promotions: List<PromotionEntity>)

    @Update
    suspend fun updatePromotion(promotion: PromotionEntity)

    @Delete
    suspend fun deletePromotion(promotion: PromotionEntity)

    @Query("DELETE FROM promotions WHERE id = :id")
    suspend fun deletePromotionById(id: Long)

    @Query("UPDATE promotions SET isActive = :isActive WHERE id = :id")
    suspend fun togglePromotionStatus(id: Long, isActive: Boolean)

    @Query("DELETE FROM promotions")
    suspend fun clearAllPromotions()
}
