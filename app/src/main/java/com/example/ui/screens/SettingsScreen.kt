package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.StoreSettingEntity
import com.example.data.sync.SyncState
import com.example.ui.KasirViewModel
import com.example.util.CurrencyFormatter

enum class SettingsSectionTab(val title: String) {
    PROFIL_TOKO("Profil & Struk"),
    PRINTER("Printer"),
    PAJAK_QRIS("Pajak & QRIS"),
    CLOUD("Cloud Sync"),
    SATUAN_IMPOR("Master Satuan & Impor"),
    RESET_RESTORE("Reset & Restore")
}

@Composable
fun SettingsScreen(viewModel: KasirViewModel) {
    val context = LocalContext.current
    val storeSettings by viewModel.storeSettings.collectAsState()
    val isOnline by viewModel.cloudSyncManager.isOnline.collectAsState()
    val syncState by viewModel.cloudSyncManager.syncState.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val units by viewModel.units.collectAsState()

    var showImportDialog by remember { mutableStateOf(false) }
    var showAddUnitDialog by remember { mutableStateOf(false) }
    var selectedSectionTab by remember { mutableStateOf(SettingsSectionTab.PROFIL_TOKO) }

    var storeName by remember(storeSettings) { mutableStateOf(storeSettings.storeName) }
    var slogan by remember(storeSettings) { mutableStateOf(storeSettings.slogan) }
    var address by remember(storeSettings) { mutableStateOf(storeSettings.address) }
    var phone by remember(storeSettings) { mutableStateOf(storeSettings.phoneNumber) }
    var receiptHeader by remember(storeSettings) { mutableStateOf(storeSettings.receiptHeader) }
    var receiptFooter by remember(storeSettings) { mutableStateOf(storeSettings.receiptFooter) }
    var paperSize by remember(storeSettings) { mutableStateOf(storeSettings.printerPaperSize) }
    var isTaxEnabled by remember(storeSettings) { mutableStateOf(storeSettings.isTaxEnabledByDefault) }
    var taxRateText by remember(storeSettings) { mutableStateOf(storeSettings.taxRatePercent.toString()) }
    var qrisMerchantName by remember(storeSettings) { mutableStateOf(storeSettings.qrisMerchantName) }
    var qrisNmid by remember(storeSettings) { mutableStateOf(storeSettings.qrisNmid) }
    var userType by remember(storeSettings) { mutableStateOf(storeSettings.userType) }
    var tenantType by remember(storeSettings) { mutableStateOf(storeSettings.tenantType) }

    fun saveAllSettings() {
        val updated = storeSettings.copy(
            storeName = storeName.ifBlank { "Toko Kelontong Berkah" },
            slogan = slogan,
            address = address,
            phoneNumber = phone,
            receiptHeader = receiptHeader,
            receiptFooter = receiptFooter,
            printerPaperSize = paperSize,
            isTaxEnabledByDefault = isTaxEnabled,
            taxRatePercent = taxRateText.toDoubleOrNull() ?: 11.0,
            qrisMerchantName = qrisMerchantName.ifBlank { "TOKO KELONTONG BERKAH" },
            qrisNmid = qrisNmid.ifBlank { "ID1020304050607" },
            userType = userType.lowercase().trim(),
            tenantType = tenantType.lowercase().trim(),
            role = userType.lowercase().trim()
        )
        viewModel.updateStoreSettings(updated)
        Toast.makeText(context, "Semua pengaturan toko berhasil disimpan!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        bottomBar = {
            // Sticky Bottom Save Button (rule g3)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { saveAllSettings() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_settings_sticky_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Simpan", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan Semua Pengaturan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Screen Header Title & Subtitle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pengaturan Toko",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Profil toko, kop struk, NMID QRIS & sinkronisasi",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
            // Section Tab Selector (rule g3)
            ScrollableTabRow(
                selectedTabIndex = selectedSectionTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp
            ) {
                SettingsSectionTab.values().forEach { tab ->
                    Tab(
                        selected = selectedSectionTab == tab,
                        onClick = { selectedSectionTab = tab },
                        text = {
                            Text(
                                text = tab.title,
                                fontWeight = if (selectedSectionTab == tab) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Identitas Toko & Struk
                if (selectedSectionTab == SettingsSectionTab.PROFIL_TOKO) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_cash_register),
                                    contentDescription = "Toko",
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Identitas Toko & Struk Pembelian", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            OutlinedTextField(
                                value = storeName,
                                onValueChange = { storeName = it },
                                label = { Text("Nama Toko") },
                                modifier = Modifier.fillMaxWidth().testTag("store_name_input")
                            )

                            OutlinedTextField(
                                value = slogan,
                                onValueChange = { slogan = it },
                                label = { Text("Slogan / Deskripsi Singkat") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Alamat Toko") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Nomor Telepon / WhatsApp") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = receiptHeader,
                                onValueChange = { receiptHeader = it },
                                label = { Text("Header Struk Pembelian") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = receiptFooter,
                                onValueChange = { receiptFooter = it },
                                label = { Text("Footer / Pesan Penutup Struk") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Card Tipe Pengguna & Tenant (Hanya tampil jika status akun adalah superadmin)
                    if (storeSettings.userType.equals("superadmin", ignoreCase = true)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = "Role",
                                        tint = if (userType.equals("superadmin", ignoreCase = true)) Color(0xFF7C3AED) else Color(0xFF006C4C),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Tipe Pengguna & Tenant",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (userType.equals("superadmin", ignoreCase = true)) Color(0xFFEDE9FE) else Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = if (userType.equals("superadmin", ignoreCase = true)) "SUPERADMIN" else "ADMIN",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        color = if (userType.equals("superadmin", ignoreCase = true)) Color(0xFF6D28D9) else Color(0xFF15803D),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Pilih hak akses peran dan tipe tenant toko pada database:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Pilihan Admin
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            userType = "admin"
                                            tenantType = "admin"
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (!userType.equals("superadmin", ignoreCase = true)) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (!userType.equals("superadmin", ignoreCase = true)) 2.dp else 1.dp,
                                        color = if (!userType.equals("superadmin", ignoreCase = true)) Color(0xFF059669) else Color(0xFFCBD5E1)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Admin",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (!userType.equals("superadmin", ignoreCase = true)) Color(0xFF047857) else Color(0xFF334155)
                                            )
                                            if (!userType.equals("superadmin", ignoreCase = true)) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = Color(0xFF059669),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Value DB: \"admin\"\nPengelola operasional toko harian, stok & kasir.",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B),
                                            lineHeight = 14.sp
                                        )
                                    }
                                }

                                // Pilihan Superadmin
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            userType = "superadmin"
                                            tenantType = "superadmin"
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (userType.equals("superadmin", ignoreCase = true)) Color(0xFFF5F3FF) else Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (userType.equals("superadmin", ignoreCase = true)) 2.dp else 1.dp,
                                        color = if (userType.equals("superadmin", ignoreCase = true)) Color(0xFF7C3AED) else Color(0xFFCBD5E1)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Superadmin",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (userType.equals("superadmin", ignoreCase = true)) Color(0xFF6D28D9) else Color(0xFF334155)
                                            )
                                            if (userType.equals("superadmin", ignoreCase = true)) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = Color(0xFF7C3AED),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Value DB: \"superadmin\"\nHak akses penuh multi-tenant, audit & sistem.",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B),
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Tersimpan di Room SQLite (store_settings & users) dan Firestore Cloud: userType=\"$userType\", tenantType=\"$tenantType\"",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    }
                }

                // 2. Printer Kasir
                if (selectedSectionTab == SettingsSectionTab.PRINTER) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Print, contentDescription = "Printer", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Pengaturan Printer Thermal", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Text(text = "Ukuran Kertas Struk:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("58mm", "80mm").forEach { size ->
                                    FilterChip(
                                        selected = paperSize == size,
                                        onClick = { paperSize = size },
                                        label = { Text("Thermal $size") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Pajak & QRIS
                if (selectedSectionTab == SettingsSectionTab.PAJAK_QRIS) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Percent, contentDescription = "Pajak", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Pajak Pertambahan Nilai (PPN)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Terapkan PPN Otomatis", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(text = "Otomatis tambahkan pajak saat checkout kasir", fontSize = 11.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = isTaxEnabled,
                                    onCheckedChange = { isTaxEnabled = it }
                                )
                            }

                            if (isTaxEnabled) {
                                OutlinedTextField(
                                    value = taxRateText,
                                    onValueChange = { taxRateText = it },
                                    label = { Text("Tarif Pajak PPN (%)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCode, contentDescription = "QRIS", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Konfigurasi Dinamis QRIS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            OutlinedTextField(
                                value = qrisMerchantName,
                                onValueChange = { qrisMerchantName = it },
                                label = { Text("Nama Merchant di Struk QRIS") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = qrisNmid,
                                onValueChange = { qrisNmid = it },
                                label = { Text("NMID (National Merchant ID QRIS)") },
                                placeholder = { Text("Contoh: ID1020304050607") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 4. Cloud & Multi-Perangkat
                if (selectedSectionTab == SettingsSectionTab.CLOUD) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Devices, contentDescription = "Cloud", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Cloud Sync & Multi-Perangkat", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isOnline) "Online" else "Offline",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOnline) Color(0xFF16A34A) else Color.Gray,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Switch(
                                        checked = isOnline,
                                        onCheckedChange = { viewModel.cloudSyncManager.toggleSimulatedNetwork(it) }
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    !isOnline -> Color(0xFFF1F5F9)
                                    syncState is SyncState.Syncing -> Color(0xFFFEF3C7)
                                    unsyncedCount > 0 -> Color(0xFFFEF3C7)
                                    else -> Color(0xFFDCFCE7)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = when {
                                                !isOnline -> "Mode Offline"
                                                syncState is SyncState.Syncing -> "Menyinkronkan..."
                                                unsyncedCount > 0 -> "$unsyncedCount Data Belum Tersinkron"
                                                else -> "Cloud Real-Time Aktif & Tersinkron"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                !isOnline -> Color.DarkGray
                                                syncState is SyncState.Syncing -> Color(0xFFD97706)
                                                unsyncedCount > 0 -> Color(0xFFD97706)
                                                else -> Color(0xFF16A34A)
                                            }
                                        )
                                        Text(
                                            text = "Terakhir: ${CurrencyFormatter.formatDate(storeSettings.lastSyncTimestamp)}",
                                            fontSize = 10.sp,
                                            color = Color.DarkGray
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.cloudSyncManager.triggerAutoSync() },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        enabled = isOnline && syncState !is SyncState.Syncing
                                    ) {
                                        if (syncState is SyncState.Syncing) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White)
                                        } else {
                                            Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Sinkronkan", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Master Satuan Dinamis & Impor Data
                if (selectedSectionTab == SettingsSectionTab.SATUAN_IMPOR) {
                    // Master Satuan Dinamis
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Master Satuan Produk (${units.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                Button(
                                    onClick = { showAddUnitDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Satuan", fontSize = 11.sp)
                                }
                            }

                            Text(
                                text = "Daftar unit/satuan yang digunakan pada dropdown form produk:",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            // Unit chips list with delete action
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(units, key = { it.id }) { u ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = u.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            if (!u.isDefault && units.size > 1) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Hapus",
                                                    tint = Color.Red.copy(alpha = 0.7f),
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable { viewModel.deleteUnit(u) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Impor Data Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Impor Data Produk & Kategori", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text(
                                text = "Impor massal daftar produk beserta kategorinya dari berkas CSV, Excel (.csv), atau format JSON ke database kasir.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Button(
                                onClick = { showImportDialog = true },
                                modifier = Modifier.fillMaxWidth().testTag("open_import_dialog_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buka Menu Impor Data")
                            }
                        }
                    }
                }

                // 6. Reset & Restore Data Tab
                if (selectedSectionTab == SettingsSectionTab.RESET_RESTORE) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reset & Pemulihan Database",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            Text(
                                text = "Pilih kategori database di bawah ini untuk melakukan reset data lokal atau memulihkan data dari cloud.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )

                            // Dropdown Selector (Jancok Kudu Sesuai Dropdown!)
                            var dropdownExpanded by remember { mutableStateOf(false) }
                            val dbCategories = listOf(
                                "Semua Database",
                                "categories",
                                "products",
                                "promotions",
                                "settings",
                                "stock_movements",
                                "transactions",
                                "units"
                            )
                            var selectedDbCategory by remember { mutableStateOf("Semua Database") }

                            Column {
                                Text(
                                    text = "PILIH DATABASE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                
                                Box {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { dropdownExpanded = true }
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp)),
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF8FAFC)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = selectedDbCategory,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Pilih",
                                                tint = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    androidx.compose.material3.DropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false },
                                        modifier = Modifier.background(Color.White)
                                    ) {
                                        dbCategories.forEach { cat ->
                                            androidx.compose.material3.DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = cat,
                                                        fontWeight = if (selectedDbCategory == cat) FontWeight.Bold else FontWeight.Normal,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                },
                                                onClick = {
                                                    selectedDbCategory = cat
                                                    dropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFE2E8F0))

                            // Action States
                            var showResetConfirmDialog by remember { mutableStateOf(false) }
                            var showRestoreConfirmDialog by remember { mutableStateOf(false) }
                            var isActionProcessing by remember { mutableStateOf(false) }

                            if (isActionProcessing) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Sedang memproses database...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // 1. Reset Action Card
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEE2E2))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "RESET DATA LOKAL & CLOUD",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFDC2626)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Menghapus seluruh salinan data lokal DAN cloud database kategori '$selectedDbCategory'.",
                                                fontSize = 11.sp,
                                                color = Color(0xFF7F1D1D)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = { showResetConfirmDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Reset Data Lokal & Cloud", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // 2. Restore Action Card
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1FAE5))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "PULIHKAN DATA CLOUD (RESTORE)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF059669)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Mengunduh dan menimpa database lokal dengan cadangan terbaru dari cloud untuk kategori '$selectedDbCategory'.",
                                                fontSize = 11.sp,
                                                color = Color(0xFF064E3B)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = { showRestoreConfirmDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Restore Data Cloud", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // Confirmation Dialogs
                            if (showResetConfirmDialog) {
                                AlertDialog(
                                    onDismissRequest = { showResetConfirmDialog = false },
                                    containerColor = Color.White,
                                    icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                    title = { Text("Konfirmasi Reset") },
                                    text = {
                                        Text(
                                            text = "Apakah Anda yakin ingin MENGHAPUS data lokal DAN cloud database kategori '$selectedDbCategory'? Tindakan ini akan menghapus seluruh data secara permanen dan tidak dapat dibatalkan!",
                                            fontSize = 13.sp
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                showResetConfirmDialog = false
                                                isActionProcessing = true
                                                viewModel.resetSpecificDatabase(selectedDbCategory) { success ->
                                                    isActionProcessing = false
                                                    val msg = if (success) "Berhasil mereset data lokal dan cloud kategori '$selectedDbCategory'!" else "Gagal mereset data lokal."
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                        ) {
                                            Text("Ya, Reset")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showResetConfirmDialog = false }) {
                                            Text("Batal")
                                        }
                                    }
                                )
                            }

                            if (showRestoreConfirmDialog) {
                                AlertDialog(
                                    onDismissRequest = { showRestoreConfirmDialog = false },
                                    containerColor = Color.White,
                                    icon = { Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF059669)) },
                                    title = { Text("Konfirmasi Restore") },
                                    text = {
                                        Text(
                                            text = "Apakah Anda yakin ingin memulihkan (Restore) data lokal kategori '$selectedDbCategory' dari cadangan cloud? Tindakan ini akan menimpa data lokal Anda saat ini.",
                                            fontSize = 13.sp
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                showRestoreConfirmDialog = false
                                                isActionProcessing = true
                                                viewModel.restoreSpecificDatabase(selectedDbCategory) { success ->
                                                    isActionProcessing = false
                                                    val msg = if (success) "Berhasil memulihkan data kategori '$selectedDbCategory' dari Cloud!" else "Gagal memulihkan data dari Cloud. Pastikan koneksi online aktif."
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                        ) {
                                            Text("Ya, Restore")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showRestoreConfirmDialog = false }) {
                                            Text("Batal")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Add Unit Dialog
    if (showAddUnitDialog) {
        var newUnitName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddUnitDialog = false },
            containerColor = Color.White,
            title = { Text("Tambah Satuan Baru") },
            text = {
                OutlinedTextField(
                    value = newUnitName,
                    onValueChange = { newUnitName = it },
                    label = { Text("Nama Satuan") },
                    placeholder = { Text("Contoh: slop, galon, rol, ikat...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUnitName.isNotBlank()) {
                            viewModel.addUnit(newUnitName.trim())
                            showAddUnitDialog = false
                            Toast.makeText(context, "Satuan '${newUnitName.trim()}' berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = newUnitName.isNotBlank()
                ) {
                    Text("Tambah")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUnitDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showImportDialog) {
        com.example.ui.components.ImportProductDialog(
            onDismiss = { showImportDialog = false },
            onImportConfirmed = { items, onDone ->
                viewModel.importProducts(items, onDone)
            }
        )
    }
}
