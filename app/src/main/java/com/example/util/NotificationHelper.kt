package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object NotificationHelper {
    private const val CHANNEL_ID = "low_stock_channel"
    private const val CHANNEL_NAME = "Peringatan Stok Minimum"
    private const val CHANNEL_DESC = "Notifikasi saat stok barang toko kelontong menipis atau habis"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showLowStockNotification(
        context: Context,
        productName: String,
        currentStock: Int,
        minStock: Int,
        unit: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = if (currentStock <= 0) "Peringatan: Stok Habis!" else "Peringatan: Stok Menipis!"
        val message = if (currentStock <= 0) {
            "Barang '$productName' telah habis (0 $unit). Segera lakukan pemesanan ke supplier!"
        } else {
            "Stok '$productName' tersisa $currentStock $unit (Batas minimum: $minStock $unit)."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationId = (productName.hashCode() and 0x7FFFFFFF)
        notificationManager.notify(notificationId, builder.build())
    }
}
