package com.example.ui.screens

import android.app.TimePickerDialog
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PromotionEntity
import com.example.ui.KasirViewModel
import com.example.ui.components.PaginationControlBar
import com.example.ui.components.paginate
import com.example.util.CurrencyFormatter

@Composable
fun PromotionsScreen(viewModel: KasirViewModel) {
    val promotions by viewModel.promotions.collectAsState()
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var selectedSchemeFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var promoToEdit by remember { mutableStateOf<PromotionEntity?>(null) }
    var promoToDelete by remember { mutableStateOf<PromotionEntity?>(null) }
    var promoPage by remember { mutableIntStateOf(1) }
    var promoPageSize by remember { mutableIntStateOf(8) }

    val filteredPromotions = remember(promotions, selectedSchemeFilter) {
        if (selectedSchemeFilter == "ALL") {
            promotions
        } else {
            promotions.filter { it.promoScheme == selectedSchemeFilter }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_promo_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Promo")
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
                        text = "Promosi & Diskon",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Kelola Flash Sale, Grosir, Tebus Murah, Bundling, & Diskon Kategori",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Scheme Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterOptions = listOf(
                    "ALL" to "Semua",
                    "FLASH_SALE" to "Flash Sale",
                    "TIERED" to "Tiered / Grosir",
                    "PWP" to "Tebus Murah (PWP)",
                    "BUNDLING" to "Paket Bundling",
                    "DAY_SPECIFIC" to "Hari Tertentu",
                    "CATEGORY" to "Diskon Kategori"
                )
                items(filterOptions) { (key, label) ->
                    FilterChip(
                        selected = selectedSchemeFilter == key,
                        onClick = { selectedSchemeFilter = key },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (filteredPromotions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Discount, contentDescription = "Promo", tint = Color.Gray, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum Ada Promosi Aktif", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Klik tombol '+' untuk menambahkan skema promosi baru", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                val totalPages = kotlin.math.max(1, kotlin.math.ceil(filteredPromotions.size.toDouble() / promoPageSize).toInt())
                val clampedPage = promoPage.coerceIn(1, totalPages)
                val paginatedPromos = filteredPromotions.paginate(clampedPage, promoPageSize)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(paginatedPromos, key = { it.id }) { promo ->
                        PromotionCard(
                            promo = promo,
                            onToggleStatus = { isActive ->
                                viewModel.togglePromotion(promo.id, isActive)
                            },
                            onEdit = { promoToEdit = promo },
                            onDelete = { promoToDelete = promo }
                        )
                    }

                    item {
                        PaginationControlBar(
                            currentPage = clampedPage,
                            totalPages = totalPages,
                            totalItems = filteredPromotions.size,
                            pageSize = promoPageSize,
                            onPageChange = { promoPage = it },
                            onPageSizeChange = {
                                promoPageSize = it
                                promoPage = 1
                            },
                            pageSizeOptions = listOf(4, 8, 16, 32),
                            itemName = "promosi",
                            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }

    // Add Promo Dialog
    if (showAddDialog) {
        PromotionFormDialog(
            title = "Tambah Promosi Baru",
            promo = null,
            products = products,
            categories = categories,
            allPromotions = promotions,
            onDismiss = { showAddDialog = false },
            onSave = { newPromo ->
                viewModel.addPromotion(newPromo)
                showAddDialog = false
            }
        )
    }

    // Edit Promo Dialog
    if (promoToEdit != null) {
        PromotionFormDialog(
            title = "Edit Promosi",
            promo = promoToEdit,
            products = products,
            categories = categories,
            allPromotions = promotions,
            onDismiss = { promoToEdit = null },
            onSave = { updatedPromo ->
                viewModel.updatePromotion(updatedPromo)
                promoToEdit = null
            }
        )
    }

    // Delete Promo Dialog
    if (promoToDelete != null) {
        AlertDialog(
            onDismissRequest = { promoToDelete = null },
            containerColor = Color.White,
            title = { Text("Hapus Promosi?") },
            text = { Text("Apakah Anda yakin ingin menghapus promosi '${promoToDelete!!.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePromotion(promoToDelete!!)
                        promoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { promoToDelete = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun PromotionCard(
    promo: PromotionEntity,
    onToggleStatus: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (schemeBadgeText, schemeBadgeColor, schemeIcon) = when (promo.promoScheme) {
        "FLASH_SALE" -> Triple("FLASH SALE", Color(0xFFF97316), Icons.Default.FlashOn)
        "TIERED" -> Triple("TIERED / BERTINGKAT", Color(0xFF6366F1), Icons.Default.Layers)
        "PWP" -> Triple("TEBUS MURAH", Color(0xFFA855F7), Icons.Default.CardGiftcard)
        "BUNDLING" -> Triple("BUNDLING", Color(0xFF06B6D4), Icons.Default.Inventory2)
        "DAY_SPECIFIC" -> Triple("HARI TERTENTU", Color(0xFF10B981), Icons.Default.CalendarMonth)
        "CATEGORY" -> Triple("DISKON KATEGORI", Color(0xFF3B82F6), Icons.Default.Category)
        else -> Triple("PROMOSI UMUM", Color(0xFF64748B), Icons.Default.LocalOffer)
    }

    val discountDesc = when (promo.promoScheme) {
        "TIERED" -> if (promo.tierRules.isNotBlank()) "Bertingkat" else if (promo.type.equals("PERCENTAGE", ignoreCase = true)) "Diskon ${promo.value.toInt()}%" else "Potongan ${CurrencyFormatter.formatRupiah(promo.value)}"
        "PWP" -> if (promo.rewardSpecialPrice > 0) "Tebus ${CurrencyFormatter.formatRupiah(promo.rewardSpecialPrice)}" else "Diskon ${promo.value.toInt()}%"
        else -> if (promo.type.equals("PERCENTAGE", ignoreCase = true)) "Diskon ${promo.value.toInt()}%" else "Potongan ${CurrencyFormatter.formatRupiah(promo.value)}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Scheme Badge on left, Switch & Delete on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = schemeBadgeColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(schemeIcon, contentDescription = null, tint = schemeBadgeColor, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = schemeBadgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = schemeBadgeColor
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (promo.isActive) "Aktif" else "Nonaktif",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (promo.isActive) Color(0xFF16A34A) else Color.Gray,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Switch(
                        checked = promo.isActive,
                        onCheckedChange = onToggleStatus,
                        modifier = Modifier.scale(0.82f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = Color(0xFFDC2626).copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Promo Name & Discount Tag inline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = promo.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Text(
                        text = if (promo.promoScheme == "PWP" && promo.rewardSpecialPrice > 0) {
                            "Tebus ${CurrencyFormatter.formatRupiah(promo.rewardSpecialPrice)}"
                        } else discountDesc,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Target Info Summary in compact 1-line
            val detailText = when (promo.promoScheme) {
                "FLASH_SALE" -> "Jam: ${String.format("%02d:00", promo.startHour)} - ${String.format("%02d:00", promo.endHour)} WIB • ${promo.targetName.ifBlank { "Semua Produk" }}"
                "TIERED" -> {
                    if (promo.tierRules.isNotBlank()) {
                        "Tingkat: " + promo.tierRules.split(",").joinToString(" • ") { part ->
                            val t = part.split(":")
                            "≥${CurrencyFormatter.formatRupiah(t.getOrNull(0)?.toDoubleOrNull() ?: 0.0)} (${t.getOrNull(1)}%)"
                        }
                    } else "Beli ≥ ${promo.minQuantity} pcs • Target: ${promo.targetName.ifBlank { "Semua Produk" }}"
                }
                "PWP" -> "Belanja ≥ ${CurrencyFormatter.formatRupiah(if (promo.minPurchaseAmount > 0) promo.minPurchaseAmount else 50000.0)} → Tebus Diskon ${promo.value.toInt()}%: ${promo.multiProductNames.ifBlank { promo.rewardProductName.ifBlank { "Produk Pilihan" } }}"
                "BUNDLING" -> {
                    val count = if (promo.multiProductIds.isNotBlank()) promo.multiProductIds.split(",").size else 2
                    "Paket ($count Produk): ${promo.multiProductNames.ifBlank { "${promo.triggerProductName} + ${promo.rewardProductName}" }}"
                }
                "DAY_SPECIFIC" -> "Hari: ${promo.activeDays.ifBlank { "Setiap Hari" }} • Target: ${promo.multiProductNames.ifBlank { promo.targetName.ifBlank { "Semua Produk" } }}"
                "CATEGORY" -> "Kategori: ${promo.targetName.ifBlank { "Semua Kategori" }}"
                else -> "Target: ${promo.targetName.ifBlank { "Semua Produk" }}"
            }

            Text(
                text = detailText,
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

data class PromoTierItem(val minSpendText: String, val discountText: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionFormDialog(
    title: String,
    promo: PromotionEntity?,
    products: List<ProductEntity>,
    categories: List<CategoryEntity>,
    allPromotions: List<PromotionEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (PromotionEntity) -> Unit
) {
    val context = LocalContext.current

    // Build map of products used in other active promotions
    val usedInOtherPromosMap = remember(allPromotions, promo?.id) {
        val map = mutableMapOf<Long, String>()
        for (p in allPromotions) {
            if (!p.isActive || p.id == (promo?.id ?: 0L)) continue
            // TIERED applies to total cart shopping, NEVER locks individual products!
            if (p.promoScheme == "TIERED") continue
            // Flash sale on ALL_PRODUCTS does not lock individual products
            if (p.promoScheme == "FLASH_SALE" && p.targetType == "ALL_PRODUCTS") continue
            // Category promo applies to category, not specific single product locking
            if (p.promoScheme == "CATEGORY") continue

            if (p.targetType == "SPECIFIC_PRODUCT" && p.targetId > 0L) {
                map[p.targetId] = p.name
            }
            if (p.promoScheme in listOf("BUNDLING", "PWP", "DAY_SPECIFIC") && p.multiProductIds.isNotBlank()) {
                p.multiProductIds.split(",").mapNotNull { it.trim().toLongOrNull() }.forEach { id ->
                    map[id] = p.name
                }
            }
        }
        map
    }

    var name by remember { mutableStateOf(promo?.name ?: "") }
    var promoScheme by remember { mutableStateOf(promo?.promoScheme ?: "FLASH_SALE") }
    var promoType by remember { mutableStateOf(promo?.type ?: "PERCENTAGE") } // PERCENTAGE or NOMINAL
    var promoValueText by remember { mutableStateOf(promo?.value?.toLong()?.toString() ?: "10") }
    var targetType by remember { mutableStateOf(promo?.targetType ?: "SPECIFIC_PRODUCT") }
    val defaultProductId = remember(products, usedInOtherPromosMap, promo) {
        if (promo != null && promo.targetId > 0L) {
            promo.targetId
        } else {
            products.firstOrNull { it.id !in usedInOtherPromosMap.keys }?.id ?: products.firstOrNull()?.id ?: 0L
        }
    }
    var selectedProductId by remember { mutableStateOf(defaultProductId) }
    var selectedCategoryId by remember { mutableStateOf(promo?.targetId ?: categories.firstOrNull()?.id ?: 0L) }
    var note by remember { mutableStateOf(promo?.note ?: "") }

    // Flash sale digital hours
    var startHour by remember { mutableStateOf(promo?.startHour ?: 9) }
    var endHour by remember { mutableStateOf(promo?.endHour ?: 21) }
    var schemeDropdownExpanded by remember { mutableStateOf(false) }

    // 1. Bundling: bisa tambah produk bundling lebih dari satu (2+ produk)
    var bundleProductIds by remember {
        mutableStateOf(
            if (promo?.multiProductIds?.isNotBlank() == true) {
                promo.multiProductIds.split(",").mapNotNull { it.trim().toLongOrNull() }
            } else if ((promo?.triggerProductId ?: 0L) > 0L) {
                listOfNotNull(promo?.triggerProductId, promo?.rewardProductId?.takeIf { it > 0 })
            } else {
                emptyList()
            }
        )
    }
    var addBundleDropdownExpanded by remember { mutableStateOf(false) }

    // 2. Tiered: promo bertingkat (misal belanja > 50rb diskon 10%, 100rb diskon 20%, 150rb diskon 30%, dst)
    var tierItems by remember {
        mutableStateOf(
            if (promo?.tierRules?.isNotBlank() == true) {
                promo.tierRules.split(",").mapNotNull { part ->
                    val tokens = part.trim().split(":")
                    if (tokens.size >= 2) PromoTierItem(tokens[0], tokens[1]) else null
                }
            } else {
                listOf(
                    PromoTierItem("50000", "10"),
                    PromoTierItem("100000", "20"),
                    PromoTierItem("150000", "30")
                )
            }
        )
    }

    // 3. Tebus Murah: belanja > 50rb bisa tebus murah produk pilihan (bisa >1 produk) sebesar 10/20/30/40/50%
    var pwpMinSpendText by remember {
        mutableStateOf(
            if (promo != null && promo.minPurchaseAmount > 0.0) promo.minPurchaseAmount.toLong().toString() else "50000"
        )
    }
    var pwpRewardProductIds by remember {
        mutableStateOf(
            if (promo?.multiProductIds?.isNotBlank() == true) {
                promo.multiProductIds.split(",").mapNotNull { it.trim().toLongOrNull() }
            } else if ((promo?.rewardProductId ?: 0L) > 0L) {
                listOf(promo!!.rewardProductId)
            } else {
                emptyList()
            }
        )
    }
    var addPwpRewardDropdownExpanded by remember { mutableStateOf(false) }

    // 4. Hari Tertentu: bisa pilih lebih dari satu produk
    var selectedDays by remember {
        mutableStateOf(
            if (promo?.activeDays?.isNotBlank() == true) promo.activeDays.split(",").toSet()
            else setOf("JUMAT")
        )
    }
    var daySpecificTargetMode by remember {
        mutableStateOf(
            if (promo?.targetType == "ALL_PRODUCTS") "ALL_PRODUCTS" else "MULTI_PRODUCTS"
        )
    }
    var daySpecificProductIds by remember {
        mutableStateOf(
            if (promo?.multiProductIds?.isNotBlank() == true) {
                promo.multiProductIds.split(",").mapNotNull { it.trim().toLongOrNull() }
            } else if ((promo?.targetId ?: 0L) > 0L) {
                listOf(promo!!.targetId)
            } else {
                emptyList()
            }
        )
    }
    var addDaySpecificDropdownExpanded by remember { mutableStateOf(false) }

    var productDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (usedInOtherPromosMap.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${usedInOtherPromosMap.size} produk sedang aktif di promo lain & tidak dapat dipilih.",
                                fontSize = 11.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Promosi") },
                    placeholder = { Text("Contoh: Flash Sale Siang, Tebus Murah, dll") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Scheme Selector (Dropdown)
                val schemeOptions = listOf(
                    "FLASH_SALE" to "Flash Sale (Berdasarkan Jam)",
                    "TIERED" to "Tiered / Bertingkat (Berdasarkan Belanja)",
                    "PWP" to "Tebus Murah (Berdasarkan Belanja)",
                    "BUNDLING" to "Paket Bundling (> 1 Produk)",
                    "DAY_SPECIFIC" to "Hari Tertentu (Bisa > 1 Produk)",
                    "CATEGORY" to "Diskon Kategori"
                )
                val selectedSchemeLabel = schemeOptions.find { it.first == promoScheme }?.second ?: "Pilih Skema Diskon"

                Text(text = "Pilih Varian Skema Diskon:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                ExposedDropdownMenuBox(
                    expanded = schemeDropdownExpanded,
                    onExpandedChange = { schemeDropdownExpanded = !schemeDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedSchemeLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Varian Skema Diskon") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = schemeDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = schemeDropdownExpanded,
                        onDismissRequest = { schemeDropdownExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        schemeOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label, fontSize = 13.sp, fontWeight = if (promoScheme == key) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    promoScheme = key
                                    schemeDropdownExpanded = false
                                },
                                modifier = Modifier.background(Color.White)
                            )
                        }
                    }
                }

                // Scheme specific configuration
                when (promoScheme) {
                    "FLASH_SALE" -> {
                        Text(text = "Pengaturan Jam Digital Flash Sale (WIB):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, _ -> startHour = hourOfDay },
                                        startHour,
                                        0,
                                        true
                                    ).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("Jam Mulai", fontSize = 10.sp, color = Color.Gray)
                                    Text(String.format("%02d:00 WIB", startHour), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, _ -> endHour = hourOfDay },
                                        endHour,
                                        0,
                                        true
                                    ).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("Jam Selesai", fontSize = 10.sp, color = Color.Gray)
                                    Text(String.format("%02d:00 WIB", endHour), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    "TIERED" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Aturan Promo Bertingkat (Berdasarkan Belanja):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    tierItems = listOf(
                                        PromoTierItem("50000", "10"),
                                        PromoTierItem("100000", "20"),
                                        PromoTierItem("150000", "30")
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Preset Contoh (50rb:10%..)", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text(
                            text = "Makin tinggi total belanja pelanggan, makin besar diskon yang otomatis didapatkan di kasir.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tierItems.forEachIndexed { index, tierItem ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = tierItem.minSpendText,
                                        onValueChange = { newVal ->
                                            tierItems = tierItems.toMutableList().also {
                                                it[index] = it[index].copy(minSpendText = newVal)
                                            }
                                        },
                                        label = { Text("Belanja ≥ (Rp)", fontSize = 10.sp) },
                                        placeholder = { Text("50000", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1.3f)
                                    )

                                    OutlinedTextField(
                                        value = tierItem.discountText,
                                        onValueChange = { newVal ->
                                            tierItems = tierItems.toMutableList().also {
                                                it[index] = it[index].copy(discountText = newVal)
                                            }
                                        },
                                        label = { Text("Diskon (%)", fontSize = 10.sp) },
                                        placeholder = { Text("10", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(0.9f)
                                    )

                                    if (tierItems.size > 1) {
                                        IconButton(
                                            onClick = {
                                                tierItems = tierItems.toMutableList().also { it.removeAt(index) }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val lastSpend = tierItems.lastOrNull()?.minSpendText?.toLongOrNull() ?: 50000L
                                    val lastDisc = tierItems.lastOrNull()?.discountText?.toLongOrNull() ?: 10L
                                    tierItems = tierItems + PromoTierItem(
                                        minSpendText = (lastSpend + 50000L).toString(),
                                        discountText = (lastDisc + 10L).coerceAtMost(100L).toString()
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tambah Tingkatan Belanja Baru", fontSize = 11.sp)
                            }
                        }
                    }

                    "PWP" -> {
                        Text(
                            text = "Syarat Minimal Belanja untuk Tebus Murah:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        OutlinedTextField(
                            value = pwpMinSpendText,
                            onValueChange = { pwpMinSpendText = it },
                            label = { Text("Belanja Minimal di Atas (Rp)") },
                            placeholder = { Text("50000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick chips for min spend - clean and evenly distributed without double "Rp"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("30000" to "30rb", "50000" to "50rb", "100000" to "100rb", "150000" to "150rb").forEach { (amount, label) ->
                                FilterChip(
                                    selected = pwpMinSpendText == amount,
                                    onClick = { pwpMinSpendText = amount },
                                    label = { Text("≥ $label", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Text(
                            text = "Pilih Produk Tebus Murah (Bisa Lebih Dari Satu):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        val currentDiscountPct = promoValueText.toDoubleOrNull() ?: 10.0

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (pwpRewardProductIds.isEmpty()) {
                                Text("Belum ada produk tebus murah dipilih.", fontSize = 11.sp, color = Color.Gray)
                            }
                            pwpRewardProductIds.forEach { pId ->
                                val p = products.find { it.id == pId }
                                val tebusPrice = if (p != null) (p.sellPrice * (1.0 - currentDiscountPct / 100.0)).coerceAtLeast(0.0) else 0.0
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p?.name ?: "Produk #$pId", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (p != null) {
                                            Text(
                                                text = "Normal: ${CurrencyFormatter.formatRupiah(p.sellPrice)} → Tebus: ${CurrencyFormatter.formatRupiah(tebusPrice)} (-${currentDiscountPct.toInt()}%)",
                                                fontSize = 10.sp,
                                                color = Color(0xFF16A34A),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { pwpRewardProductIds = pwpRewardProductIds - pId },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Hapus", tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            val unselectedRewardProducts = products.filter { it.id !in pwpRewardProductIds && it.id !in usedInOtherPromosMap.keys }
                            if (unselectedRewardProducts.isNotEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { addPwpRewardDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Tambah Produk Tebus Murah", fontSize = 11.sp)
                                    }

                                    DropdownMenu(
                                        expanded = addPwpRewardDropdownExpanded,
                                        onDismissRequest = { addPwpRewardDropdownExpanded = false },
                                        modifier = Modifier
                                            .heightIn(max = 280.dp)
                                            .background(Color.White)
                                    ) {
                                        unselectedRewardProducts.forEach { prod ->
                                            DropdownMenuItem(
                                                text = { Text("${prod.name} (${CurrencyFormatter.formatRupiah(prod.sellPrice)})", fontSize = 12.sp) },
                                                onClick = {
                                                    pwpRewardProductIds = pwpRewardProductIds + prod.id
                                                    addPwpRewardDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Besaran Diskon Tebus Murah:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("10", "20", "30", "40", "50").forEach { pct ->
                                FilterChip(
                                    selected = promoValueText == pct,
                                    onClick = { promoValueText = pct },
                                    label = { Text("$pct%", fontSize = 10.sp, fontWeight = if (promoValueText == pct) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = promoValueText,
                            onValueChange = { promoValueText = it },
                            label = { Text("Atau Masukkan Diskon Custom (%)") },
                            placeholder = { Text("Contoh: 10, 20, 30, 40, 50") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    "BUNDLING" -> {
                        Text(
                            text = "Pilih Produk Paket Bundling (Bisa 2 atau Lebih):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Pelanggan harus membeli seluruh produk paket berikut untuk mendapatkan diskon bundling.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (bundleProductIds.isEmpty()) {
                                Text("Belum ada produk dipilih dalam paket.", fontSize = 11.sp, color = Color.Gray)
                            }
                            bundleProductIds.forEachIndexed { index, pId ->
                                val p = products.find { it.id == pId }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(p?.name ?: "Produk #$pId", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(if (p != null) CurrencyFormatter.formatRupiah(p.sellPrice) else "", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                    IconButton(
                                        onClick = { bundleProductIds = bundleProductIds - pId },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Hapus", tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            // Bundle package price calculation preview
                            val totalBundleOriginal = bundleProductIds.mapNotNull { id -> products.find { it.id == id }?.sellPrice }.sum()
                            val discVal = promoValueText.toDoubleOrNull() ?: 10.0
                            val totalBundleDiscounted = if (promoType == "PERCENTAGE") {
                                (totalBundleOriginal * (1.0 - discVal / 100.0)).coerceAtLeast(0.0)
                            } else {
                                (totalBundleOriginal - discVal).coerceAtLeast(0.0)
                            }
                            if (bundleProductIds.isNotEmpty()) {
                                Surface(
                                    color = Color(0xFFECFEFF),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCFFAFE)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("${bundleProductIds.size} Produk dalam Paket", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0E7490))
                                            Text("Total Normal: ${CurrencyFormatter.formatRupiah(totalBundleOriginal)}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Harga Paket Promo:", fontSize = 9.sp, color = Color.Gray)
                                            Text(CurrencyFormatter.formatRupiah(totalBundleDiscounted), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0891B2))
                                        }
                                    }
                                }
                            }

                            val unselectedProducts = products.filter { it.id !in bundleProductIds && it.id !in usedInOtherPromosMap.keys }
                            if (unselectedProducts.isNotEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { addBundleDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Tambah Produk ke Paket", fontSize = 11.sp)
                                    }

                                    DropdownMenu(
                                        expanded = addBundleDropdownExpanded,
                                        onDismissRequest = { addBundleDropdownExpanded = false },
                                        modifier = Modifier
                                            .heightIn(max = 280.dp)
                                            .background(Color.White)
                                    ) {
                                        unselectedProducts.forEach { prod ->
                                            DropdownMenuItem(
                                                text = { Text("${prod.name} (${CurrencyFormatter.formatRupiah(prod.sellPrice)})", fontSize = 12.sp) },
                                                onClick = {
                                                    bundleProductIds = bundleProductIds + prod.id
                                                    addBundleDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "DAY_SPECIFIC" -> {
                        Text(text = "Pilih Hari Aktif Promo:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        val days = listOf("SENIN", "SELASA", "RABU", "KAMIS", "JUMAT", "SABTU", "MINGGU")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(days) { day ->
                                val isSelected = selectedDays.contains(day)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                                    },
                                    label = { Text(day, fontSize = 10.sp) }
                                )
                            }
                        }

                        // Day presets
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedDays == setOf("JUMAT"),
                                onClick = { selectedDays = setOf("JUMAT") },
                                label = { Text("Jumat Berkah", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = selectedDays == setOf("SABTU", "MINGGU"),
                                onClick = { selectedDays = setOf("SABTU", "MINGGU") },
                                label = { Text("Weekend", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = selectedDays.size == 7,
                                onClick = { selectedDays = days.toSet() },
                                label = { Text("Semua Hari", fontSize = 10.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Target Produk Hari Tertentu (Bisa Lebih Dari Satu):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = daySpecificTargetMode == "MULTI_PRODUCTS",
                                onClick = { daySpecificTargetMode = "MULTI_PRODUCTS" },
                                label = { Text("Pilih Produk Tertentu", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = daySpecificTargetMode == "ALL_PRODUCTS",
                                onClick = { daySpecificTargetMode = "ALL_PRODUCTS" },
                                label = { Text("Semua Produk", fontSize = 11.sp) }
                            )
                        }

                        if (daySpecificTargetMode == "MULTI_PRODUCTS") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (daySpecificProductIds.isEmpty()) {
                                    Text("Belum ada produk dipilih.", fontSize = 11.sp, color = Color.Gray)
                                }
                                daySpecificProductIds.forEach { pId ->
                                    val p = products.find { it.id == pId }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White, RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(p?.name ?: "Produk #$pId", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(if (p != null) CurrencyFormatter.formatRupiah(p.sellPrice) else "", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        IconButton(
                                            onClick = { daySpecificProductIds = daySpecificProductIds - pId },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Hapus", tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }

                                val unselectedDayProducts = products.filter { it.id !in daySpecificProductIds && it.id !in usedInOtherPromosMap.keys }
                                if (unselectedDayProducts.isNotEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { addDaySpecificDropdownExpanded = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Tambah Produk Target", fontSize = 11.sp)
                                        }

                                        DropdownMenu(
                                            expanded = addDaySpecificDropdownExpanded,
                                            onDismissRequest = { addDaySpecificDropdownExpanded = false },
                                            modifier = Modifier
                                                .heightIn(max = 280.dp)
                                                .background(Color.White)
                                        ) {
                                            unselectedDayProducts.forEach { prod ->
                                                DropdownMenuItem(
                                                    text = { Text("${prod.name} (${CurrencyFormatter.formatRupiah(prod.sellPrice)})", fontSize = 12.sp) },
                                                    onClick = {
                                                        daySpecificProductIds = daySpecificProductIds + prod.id
                                                        addDaySpecificDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "CATEGORY" -> {
                        Text(text = "Pilih Kategori Produk:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        ExposedDropdownMenuBox(
                            expanded = categoryDropdownExpanded,
                            onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val currentCat = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Pilih Kategori"
                            OutlinedTextField(
                                value = currentCat,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kategori Produk Diskon") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false }
                            ) {
                                categories.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.name) },
                                        onClick = {
                                            selectedCategoryId = c.id
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // If FLASH_SALE, allow target product or all products
                if (promoScheme == "FLASH_SALE") {
                    Text(text = "Target Produk Flash Sale:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = targetType == "SPECIFIC_PRODUCT",
                            onClick = { targetType = "SPECIFIC_PRODUCT" },
                            label = { Text("Produk Tertentu") }
                        )
                        FilterChip(
                            selected = targetType == "ALL_PRODUCTS",
                            onClick = { targetType = "ALL_PRODUCTS" },
                            label = { Text("Semua Produk") }
                        )
                    }

                    if (targetType == "SPECIFIC_PRODUCT") {
                        ExposedDropdownMenuBox(
                            expanded = productDropdownExpanded,
                            onExpandedChange = { productDropdownExpanded = !productDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val currentProdName = products.firstOrNull { it.id == selectedProductId }?.name ?: "Pilih Produk"
                            OutlinedTextField(
                                value = currentProdName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Produk Sasaran") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = productDropdownExpanded,
                                onDismissRequest = { productDropdownExpanded = false }
                            ) {
                                products.forEach { prod ->
                                    val isUsedInOther = usedInOtherPromosMap.containsKey(prod.id) && prod.id != (promo?.targetId ?: 0L)
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = prod.name,
                                                    color = if (isUsedInOther) Color.Gray else Color.Black
                                                )
                                                if (isUsedInOther) {
                                                    Text(
                                                        text = "Sudah dipakai di promo '${usedInOtherPromosMap[prod.id]}'",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFFDC2626)
                                                    )
                                                }
                                            }
                                        },
                                        enabled = !isUsedInOther,
                                        onClick = {
                                            selectedProductId = prod.id
                                            productDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Discount Type and Value (for all except TIERED and PWP where discount is set above)
                if (promoScheme !in listOf("TIERED", "PWP")) {
                    Text(text = "Tipe & Besaran Diskon:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = promoType == "PERCENTAGE",
                            onClick = { promoType = "PERCENTAGE" },
                            label = { Text("Persentase (%)") }
                        )
                        FilterChip(
                            selected = promoType == "NOMINAL",
                            onClick = { promoType = "NOMINAL" },
                            label = { Text("Potongan (Rp)") }
                        )
                    }

                    OutlinedTextField(
                        value = promoValueText,
                        onValueChange = { promoValueText = it },
                        label = { Text(if (promoType == "PERCENTAGE") "Besar Diskon (%)" else "Potongan Harga (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan Promosi (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTargetType = when (promoScheme) {
                        "TIERED" -> "ALL_PRODUCTS"
                        "CATEGORY" -> "CATEGORY"
                        "DAY_SPECIFIC" -> daySpecificTargetMode
                        "BUNDLING" -> "MULTI_PRODUCTS"
                        "PWP" -> "MULTI_PRODUCTS"
                        else -> targetType
                    }

                    val finalTargetId = when {
                        promoScheme == "TIERED" -> 0L
                        promoScheme == "CATEGORY" -> selectedCategoryId
                        targetType == "SPECIFIC_PRODUCT" -> selectedProductId
                        else -> 0L
                    }

                    val finalTargetName = when {
                        promoScheme == "TIERED" -> "Total Belanja"
                        promoScheme == "CATEGORY" -> categories.firstOrNull { it.id == selectedCategoryId }?.name ?: ""
                        promoScheme == "BUNDLING" -> bundleProductIds.mapNotNull { id -> products.find { it.id == id }?.name }.joinToString(" + ")
                        promoScheme == "PWP" -> pwpRewardProductIds.mapNotNull { id -> products.find { it.id == id }?.name }.joinToString(", ")
                        promoScheme == "DAY_SPECIFIC" -> if (daySpecificTargetMode == "ALL_PRODUCTS") "Semua Produk" else daySpecificProductIds.mapNotNull { id -> products.find { it.id == id }?.name }.joinToString(", ")
                        targetType == "SPECIFIC_PRODUCT" -> products.firstOrNull { it.id == selectedProductId }?.name ?: ""
                        else -> "Semua Produk"
                    }

                    val finalMultiIds = when (promoScheme) {
                        "BUNDLING" -> bundleProductIds.joinToString(",")
                        "PWP" -> pwpRewardProductIds.joinToString(",")
                        "DAY_SPECIFIC" -> if (daySpecificTargetMode == "MULTI_PRODUCTS") daySpecificProductIds.joinToString(",") else ""
                        else -> ""
                    }

                    val finalMultiNames = when (promoScheme) {
                        "BUNDLING" -> bundleProductIds.mapNotNull { id -> products.find { it.id == id }?.name }.joinToString(" + ")
                        "PWP" -> pwpRewardProductIds.mapNotNull { id -> products.find { it.id == id }?.name }.joinToString(", ")
                        "DAY_SPECIFIC" -> if (daySpecificTargetMode == "MULTI_PRODUCTS") daySpecificProductIds.mapNotNull { id -> products.find { it.id == id }?.name }.joinToString(", ") else "Semua Produk"
                        else -> ""
                    }

                    val finalTierRules = when (promoScheme) {
                        "TIERED" -> tierItems.filter { it.minSpendText.isNotBlank() && it.discountText.isNotBlank() }
                            .joinToString(",") { "${it.minSpendText.trim()}:${it.discountText.trim()}" }
                        else -> ""
                    }

                    val finalPromo = PromotionEntity(
                        id = promo?.id ?: 0L,
                        name = name.ifBlank { "Promosi Baru" },
                        promoScheme = promoScheme,
                        type = if (promoScheme in listOf("TIERED", "PWP")) "PERCENTAGE" else promoType,
                        value = promoValueText.toDoubleOrNull() ?: 10.0,
                        targetType = finalTargetType,
                        targetId = finalTargetId,
                        targetName = finalTargetName,
                        minPurchaseAmount = when (promoScheme) {
                            "PWP" -> pwpMinSpendText.toDoubleOrNull() ?: 50000.0
                            else -> 0.0
                        },
                        tierRules = finalTierRules,
                        multiProductIds = finalMultiIds,
                        multiProductNames = finalMultiNames,
                        startHour = if (promoScheme == "FLASH_SALE") startHour else 0,
                        endHour = if (promoScheme == "FLASH_SALE") endHour else 24,
                        minQuantity = 1,
                        activeDays = if (promoScheme == "DAY_SPECIFIC") selectedDays.joinToString(",") else "",
                        triggerProductId = if (promoScheme == "BUNDLING") (bundleProductIds.firstOrNull() ?: 0L) else 0L,
                        triggerProductName = if (promoScheme == "BUNDLING") (bundleProductIds.firstOrNull()?.let { id -> products.find { it.id == id }?.name } ?: "") else "",
                        triggerMinQty = 1,
                        rewardProductId = if (promoScheme == "BUNDLING") (bundleProductIds.getOrNull(1) ?: 0L) else (if (promoScheme == "PWP") (pwpRewardProductIds.firstOrNull() ?: 0L) else 0L),
                        rewardProductName = if (promoScheme == "BUNDLING") (bundleProductIds.getOrNull(1)?.let { id -> products.find { it.id == id }?.name } ?: "") else (if (promoScheme == "PWP") (pwpRewardProductIds.firstOrNull()?.let { id -> products.find { it.id == id }?.name } ?: "") else ""),
                        rewardSpecialPrice = 0.0,
                        isActive = promo?.isActive ?: true,
                        note = note
                    )
                    onSave(finalPromo)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Simpan Promosi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
