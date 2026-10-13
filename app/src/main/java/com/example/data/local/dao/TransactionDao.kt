package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

data class TransactionWithItems(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val items: List<TransactionItemEntity>
)

data class TopProductSummary(
    val productId: Long,
    val productName: String,
    val barcode: String,
    val totalQty: Int,
    val totalSales: Double,
    val totalProfit: Double
)

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsWithItems(): Flow<List<TransactionWithItems>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionWithItemsById(id: Long): TransactionWithItems?

    @Transaction
    @Query("SELECT * FROM transactions WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getTransactionByInvoice(invoiceNumber: String): TransactionWithItems?

    @Transaction
    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTimestamp AND :endTimestamp ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<TransactionWithItems>>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItemEntity>

    @Query("""
        SELECT ti.productId, ti.productName, ti.barcode, 
               SUM(ti.quantity) as totalQty, 
               SUM(ti.subtotal) as totalSales,
               SUM((ti.unitPrice - ti.costPrice) * ti.quantity) as totalProfit
        FROM transaction_items ti
        INNER JOIN transactions t ON ti.transactionId = t.id
        WHERE t.timestamp BETWEEN :startTimestamp AND :endTimestamp
        GROUP BY ti.productId, ti.productName, ti.barcode
        ORDER BY totalQty DESC
        LIMIT :limit
    """)
    fun getTopProducts(startTimestamp: Long, endTimestamp: Long, limit: Int = 10): Flow<List<TopProductSummary>>

    @Query("SELECT COUNT(*) FROM transactions WHERE isSynced = 0")
    fun getUnsyncedCount(): Flow<Int>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsDirect(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET isSynced = 1, syncTimestamp = :syncTime WHERE id IN (:transactionIds)")
    suspend fun markTransactionsSynced(transactionIds: List<Long>, syncTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("DELETE FROM transactions WHERE invoiceNumber = :invoiceNumber")
    suspend fun deleteTransactionByInvoice(invoiceNumber: String)

    @Query("DELETE FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun deleteTransactionItems(transactionId: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("DELETE FROM transaction_items")
    suspend fun clearAllTransactionItems()
}
