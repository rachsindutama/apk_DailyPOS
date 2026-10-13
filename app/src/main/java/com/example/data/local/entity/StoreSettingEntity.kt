package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_settings")
data class StoreSettingEntity(
    @PrimaryKey
    val id: Int = 1,
    val storeName: String = "Toko Kelontong Berkah",
    val slogan: String = "Sembako Murah & Lengkap",
    val address: String = "Jl. Pasar Baru No. 88, RT 03/RW 05",
    val phoneNumber: String = "0812-8888-9999",
    val receiptHeader: String = "SELAMAT BERBELANJA",
    val receiptFooter: String = "Terima kasih atas kunjungan Anda!\nBarang yang sudah dibeli tidak dapat ditukar/dikembalikan.",
    val printerPaperSize: String = "58mm",
    val taxRatePercent: Double = 11.0,
    val isTaxEnabledByDefault: Boolean = false,
    val qrisMerchantName: String = "TOKO KELONTONG BERKAH",
    val qrisNmid: String = "ID1020304050607",
    val cloudSyncEnabled: Boolean = true,
    val cloudDeviceId: String = "KASIR-UTAMA-01",
    val lastSyncTimestamp: Long = 0L,
    val userType: String = "admin", // "superadmin", "admin"
    val tenantType: String = "admin", // "superadmin", "admin"
    val role: String = "admin" // "superadmin", "admin"
)
