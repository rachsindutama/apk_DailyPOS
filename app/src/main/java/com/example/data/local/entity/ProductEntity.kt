package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["barcode"]),
        Index(value = ["sku"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val barcode: String,
    val sku: String = "",
    val name: String,
    val categoryId: Long,
    val buyPrice: Double,
    val sellPrice: Double,
    val stock: Int,
    val minStock: Int = 5,
    val unit: String = "pcs",
    val imageUrl: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
