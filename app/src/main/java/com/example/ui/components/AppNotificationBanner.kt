package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

@Composable
fun AppNotificationBanner(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isError = message != null && (
        message.contains("Gagal", ignoreCase = true) ||
        message.contains("habis", ignoreCase = true) ||
        message.contains("tidak", ignoreCase = true) ||
        message.contains("melebihi", ignoreCase = true) ||
        message.contains("kosong", ignoreCase = true)
    )
    val isSuccess = message != null && (
        message.contains("Ditambahkan", ignoreCase = true) ||
        message.contains("berhasil", ignoreCase = true) ||
        message.contains("Sukses", ignoreCase = true) ||
        message.contains("Disimpan", ignoreCase = true)
    )

    val bgColor = if (isError) Color(0xFFFEF2F2) else if (isSuccess) Color(0xFFECFDF5) else Color(0xFFEFF6FF)
    val borderColor = if (isError) Color(0xFFFECACA) else if (isSuccess) Color(0xFFA7F3D0) else Color(0xFFBFDBFE)
    val textColor = if (isError) Color(0xFF991B1B) else if (isSuccess) Color(0xFF047857) else Color(0xFF1E40AF)
    val icon = if (isError) Icons.Default.Error else if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Info

    LaunchedEffect(message) {
        if (message != null) {
            delay(3500L)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .zIndex(999f)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (message != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = bgColor,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = message,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
