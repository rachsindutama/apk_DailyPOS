package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_movements",
    indices = [Index(value = ["productId"]), Index(value = ["timestamp"])]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val type: String, // MASUK, KELUAR, PENYESUAIAN, PENJUALAN
    val quantity: Int, // e.g. +10 or -2
    val previousStock: Int,
    val currentStock: Int,
    val reason: String = "",
    val notes: String = "",
    val costPrice: Double = 0.0,
    val isNsb: Boolean = false,
    val lossAmount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
