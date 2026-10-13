package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["invoiceNumber"], unique = true), Index(value = ["timestamp"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val discountNote: String = "",
    val taxAmount: Double = 0.0,
    val taxPercent: Double = 0.0,
    val isTaxApplied: Boolean = false,
    val grandTotal: Double,
    val paymentMethod: String = "TUNAI", // TUNAI, QRIS, TRANSFER
    val cashReceived: Double = 0.0,
    val changeAmount: Double = 0.0,
    val cashierName: String = "Kasir 1",
    val customerName: String = "Pelanggan Umum",
    val isSynced: Boolean = false,
    val syncTimestamp: Long = 0L
)
