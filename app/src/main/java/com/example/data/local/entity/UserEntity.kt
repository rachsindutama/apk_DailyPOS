package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val email: String = "",
    val name: String = "",
    val userType: String = "admin", // "superadmin", "admin"
    val tenantType: String = "admin", // "superadmin", "admin"
    val role: String = "admin", // "superadmin", "admin"
    val storeName: String = "Toko Kelontong Berkah",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
