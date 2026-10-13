package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<StockMovementEntity>)

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC, id DESC")
    fun getAllMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE type = :type ORDER BY timestamp DESC, id DESC")
    fun getMovementsByType(type: String): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC, id DESC")
    fun getMovementsForProduct(productId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE timestamp BETWEEN :startTimestamp AND :endTimestamp ORDER BY timestamp DESC, id DESC")
    fun getMovementsBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE isSynced = 0")
    suspend fun getUnsyncedMovements(): List<StockMovementEntity>

    @Query("UPDATE stock_movements SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markMovementsSynced(ids: List<Long>)

    @androidx.room.Update
    suspend fun updateMovement(movement: StockMovementEntity)

    @Query("SELECT * FROM stock_movements WHERE type = 'KELUAR'")
    suspend fun getAllStockOutMovements(): List<StockMovementEntity>

    @Query("SELECT * FROM stock_movements WHERE type = 'KELUAR' OR (type = 'PENYESUAIAN' AND quantity < 0) OR isNsb = 1 OR lossAmount > 0")
    suspend fun getAllPotentialNsbMovements(): List<StockMovementEntity>

    @androidx.room.Delete
    suspend fun deleteMovement(movement: StockMovementEntity)

    @Query("SELECT * FROM stock_movements WHERE id = :id LIMIT 1")
    suspend fun getMovementById(id: Long): StockMovementEntity?

    @Query("SELECT * FROM stock_movements WHERE productId = :productId AND timestamp = :timestamp LIMIT 1")
    suspend fun getMovementByProductAndTimestamp(productId: Long, timestamp: Long): StockMovementEntity?

    @Query("SELECT * FROM stock_movements WHERE productId = :productId AND ABS(timestamp - :timestamp) < 5000 LIMIT 1")
    suspend fun getNearDuplicateMovement(productId: Long, timestamp: Long): StockMovementEntity?

    @Query("SELECT * FROM stock_movements WHERE productId = :productId AND type = :type AND quantity = :quantity AND timestamp >= :minTimestamp LIMIT 1")
    suspend fun getRecentMovement(productId: Long, type: String, quantity: Int, minTimestamp: Long): StockMovementEntity?

    @Query("SELECT currentStock FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun getLatestStockForProduct(productId: Long): Int?

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun getLatestMovementForProduct(productId: Long): StockMovementEntity?

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC, id DESC")
    suspend fun getAllMovementsList(): List<StockMovementEntity>

    @Query("SELECT * FROM stock_movements WHERE reason LIKE '%' || :invoiceNumber || '%'")
    suspend fun getMovementsForInvoice(invoiceNumber: String): List<StockMovementEntity>

    @Query("DELETE FROM stock_movements WHERE id IN (:ids)")
    suspend fun deleteMovementsByIds(ids: List<Long>)

    @Query("DELETE FROM stock_movements WHERE id = :id")
    suspend fun deleteMovementById(id: Long)

    @Query("DELETE FROM stock_movements WHERE productId = :productId AND timestamp = :timestamp")
    suspend fun deleteMovementByProductAndTimestamp(productId: Long, timestamp: Long)

    @Query("DELETE FROM stock_movements WHERE productId = :productId")
    suspend fun deleteMovementsByProductId(productId: Long)

    @Query("DELETE FROM stock_movements")
    suspend fun clearAllMovements()
}
