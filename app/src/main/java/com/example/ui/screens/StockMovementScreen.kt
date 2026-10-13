package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.ui.KasirViewModel
import com.example.ui.components.PaginationControlBar
import com.example.ui.components.paginate
import com.example.util.CurrencyFormatter
import java.util.Calendar

data class BulkItem(
    val product: ProductEntity,
    var qty: Int,
    var isNsb: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockMovementScreen(
    viewModel: KasirViewModel,
    showHeader: Boolean = true
) {
    val context = LocalContext.current
    val movements by viewModel.stockMovements.collectAsState()
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf("SEMUA") } // "SEMUA", "HARIAN", "BULANAN", "TAHUNAN", "RENTANG"
    var selectedType by remember { mutableStateOf("SEMUA") }
    var filterDropdownExpanded by remember { mutableStateOf(false) }

    var customStartDate by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis)
    }
    var customEndDate by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis)
    }

    fun showStartDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = customStartDate }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val newStart = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                }.timeInMillis
                customStartDate = newStart
                selectedPeriod = "RENTANG"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showEndDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = customEndDate }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val newEnd = Calendar.getInstance().apply {
                    set(year, month, day, 23, 59, 59)
                }.timeInMillis
                customEndDate = newEnd
                selectedPeriod = "RENTANG"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Modern 2-Step Adjustment Flow
    var activeAdjustmentMode by remember { mutableStateOf<Boolean?>(null) } // true = in, false = out, null = closed
    var movementToDelete by remember { mutableStateOf<StockMovementEntity?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var movementPage by remember { mutableIntStateOf(1) }
    var movementPageSize by remember { mutableIntStateOf(10) }

    val filteredMovements = movements.filter { movement ->
        val matchesType = when (selectedType) {
            "SEMUA" -> true
            "NSB" -> movement.isNsb
            else -> movement.type.equals(selectedType, ignoreCase = true)
        }
        val matchesSearch = searchQuery.isBlank() ||
                movement.productName.contains(searchQuery, ignoreCase = true) ||
                movement.notes.contains(searchQuery, ignoreCase = true) ||
                movement.reason.contains(searchQuery, ignoreCase = true)

        val matchesPeriod = when (selectedPeriod) {
            "HARIAN" -> {
                val cal = Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis() }
                val startDay = Calendar.getInstance().apply {
                    set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                }.timeInMillis
                val endDay = Calendar.getInstance().apply {
                    set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
                }.timeInMillis
                movement.timestamp in startDay..endDay
            }
            "BULANAN" -> {
                val cal = Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis() }
                val startMonth = Calendar.getInstance().apply {
                    set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1, 0, 0, 0)
                }.timeInMillis
                val endMonth = Calendar.getInstance().apply {
                    set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
                }.timeInMillis
                movement.timestamp in startMonth..endMonth
            }
            "TAHUNAN" -> {
                val cal = Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis() }
                val startYear = Calendar.getInstance().apply {
                    set(cal.get(Calendar.YEAR), 0, 1, 0, 0, 0)
                }.timeInMillis
                val endYear = Calendar.getInstance().apply {
                    set(cal.get(Calendar.YEAR), 11, 31, 23, 59, 59)
                }.timeInMillis
                movement.timestamp in startYear..endYear
            }
            "RENTANG" -> movement.timestamp in customStartDate..customEndDate
            else -> true
        }

        matchesType && matchesSearch && matchesPeriod
    }

    val typeOptions = listOf(
        "SEMUA" to "Semua",
        "MASUK" to "Stok Masuk (+)",
        "KELUAR" to "Stok Keluar (-)",
        "NSB" to "Nota Selisih (NSB)",
        "PENJUALAN" to "Penjualan Kasir",
        "PENYESUAIAN" to "Penyesuaian Opname"
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (showHeader) {
            // Screen Header Title & Subtitle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Riwayat Mutasi Stok",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Pencatatan mutasi stok masuk & stok keluar",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

            // Period Filter Bar (Semua, Hari Ini, Bulan Ini, Tahun Ini, Rentang Tanggal)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    listOf(
                        "SEMUA" to "Semua",
                        "HARIAN" to "Hari Ini",
                        "BULANAN" to "Bulan Ini",
                        "TAHUNAN" to "Tahun Ini",
                        "RENTANG" to "Rentang Tanggal"
                    )
                ) { (key, label) ->
                    val isSelected = selectedPeriod == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPeriod = key },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Custom Date Range Selector (Shown when Rentang is chosen)
            if (selectedPeriod == "RENTANG") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showStartDatePicker() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = CurrencyFormatter.formatDateShort(customStartDate),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text("s/d", fontSize = 11.sp, color = Color.Gray)

                    OutlinedButton(
                        onClick = { showEndDatePicker() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = CurrencyFormatter.formatDateShort(customEndDate),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Quick Action Bar: Tambah Stok Masuk & Kurangi Stok Keluar + Search & Filter Dropdown
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top row: Action Buttons (+ Tambah Stok & - Kurangi Stok)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { activeAdjustmentMode = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_quick_stock_in"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stok Masuk", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { activeAdjustmentMode = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_quick_stock_out"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stok Keluar", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Search and Filter Type in 2 Columns
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search box on the left
                        Surface(
                            modifier = Modifier
                                .weight(1.15f)
                                .height(50.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Cari",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Cari barang/catatan...",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 11.sp,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp), tint = Color.Gray)
                                    }
                                }
                            }
                        }

                        // Type dropdown on the right
                        Box(modifier = Modifier.weight(0.95f)) {
                            val selectedTypeLabel = typeOptions.find { it.first == selectedType }?.second ?: "Semua Riwayat"
                            Surface(
                                onClick = { filterDropdownExpanded = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selectedType != "SEMUA") MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedTypeLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedType != "SEMUA") FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedType != "SEMUA") MaterialTheme.colorScheme.primary else Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.Gray
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = filterDropdownExpanded,
                                onDismissRequest = { filterDropdownExpanded = false },
                                modifier = Modifier
                                    .heightIn(max = 260.dp)
                                    .background(Color.White)
                            ) {
                                typeOptions.forEach { (typeKey, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = label,
                                                fontWeight = if (selectedType == typeKey) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedType == typeKey) Color(0xFF047857) else Color(0xFF0F172A),
                                                fontSize = 12.sp
                                            )
                                        },
                                        onClick = {
                                            selectedType = typeKey
                                            filterDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

        // Movement List
        if (filteredMovements.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Belum Ada Riwayat Mutasi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Gunakan tombol '+ Stok Masuk' atau '- Stok Keluar' di atas untuk mencatat penambahan / pengurangan stok produk",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            val totalPages = kotlin.math.max(1, kotlin.math.ceil(filteredMovements.size.toDouble() / movementPageSize).toInt())
            val clampedPage = movementPage.coerceIn(1, totalPages)
            val paginatedMovements = filteredMovements.paginate(clampedPage, movementPageSize)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(paginatedMovements, key = { it.id }) { movement ->
                    MovementItemCard(
                        movement = movement
                    )
                }

                item {
                    PaginationControlBar(
                        currentPage = clampedPage,
                        totalPages = totalPages,
                        totalItems = filteredMovements.size,
                        pageSize = movementPageSize,
                        onPageChange = { movementPage = it },
                        onPageSizeChange = {
                            movementPageSize = it
                            movementPage = 1
                        },
                        itemName = "mutasi",
                        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                    )
                }
            }
        }
    }

    // Bulk Penerimaan / Pengeluaran Barang Modal Flow
    if (activeAdjustmentMode != null) {
        val isStockIn = activeAdjustmentMode!!
        BulkStockAdjustmentModal(
            isStockIn = isStockIn,
            products = products,
            categories = categories,
            onDismiss = { activeAdjustmentMode = null },
            onConfirmBulkStockChange = { bulkItems, extraInfo, notes ->
                activeAdjustmentMode = null
                if (bulkItems.isEmpty()) {
                    Toast.makeText(context, "Daftar barang tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                    return@BulkStockAdjustmentModal
                }

                for (item in bulkItems) {
                    if (isStockIn) {
                        viewModel.recordStockIn(
                            product = item.product,
                            qty = item.qty,
                            supplier = extraInfo.ifBlank { "Supplier Umum" },
                            newBuyPrice = null,
                            notes = notes
                        )
                    } else {
                        val reasonText = extraInfo.ifBlank { "Rusak/Kadaluarsa" }
                        val unitLoss = if (item.product.buyPrice > 0.0) item.product.buyPrice else item.product.sellPrice
                        val totalLoss = unitLoss * item.qty
                        val finalIsNsb = true
                        val finalLoss = totalLoss
                        viewModel.recordStockOut(
                            product = item.product,
                            qty = item.qty,
                            reason = reasonText,
                            notes = notes,
                            isNsb = finalIsNsb,
                            lossAmount = finalLoss
                        )
                    }
                }

                val typeLabel = if (isStockIn) "Stok Masuk" else "Stok Keluar"
                Toast.makeText(context, "Berhasil memproses $typeLabel (${bulkItems.size} barang)", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete single movement item confirmation
    if (movementToDelete != null) {
        val move = movementToDelete!!
        AlertDialog(
            onDismissRequest = { movementToDelete = null },
            title = { Text("Hapus Catatan Mutasi?") },
            text = { Text("Hapus riwayat mutasi ${move.type} untuk '${move.productName}'? Catatan ini akan dihapus dari riwayat lokal dan Firestore.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStockMovement(move)
                        movementToDelete = null
                        Toast.makeText(context, "Riwayat mutasi dihapus", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { movementToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Clear all movements dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = Color.White,
            title = { Text("Bersihkan Semua Riwayat?") },
            text = { Text("Apakah Anda yakin ingin mengosongkan seluruh riwayat mutasi stok?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearStockMovements()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "Riwayat mutasi dibersihkan", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Bersihkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun MovementItemCard(
    movement: StockMovementEntity
) {
    val isPositive = movement.quantity > 0
    val typeBgColor = when (movement.type) {
        "MASUK" -> Color(0xFFECFDF5)
        "KELUAR" -> Color(0xFFFEF2F2)
        "PENJUALAN" -> Color(0xFFF0F9FF)
        else -> Color(0xFFFEF3C7)
    }
    val typeBorderColor = when (movement.type) {
        "MASUK" -> Color(0xFFA7F3D0)
        "KELUAR" -> Color(0xFFFECACA)
        "PENJUALAN" -> Color(0xFFBAE6FD)
        else -> Color(0xFFFDE68A)
    }
    val typeColor = when (movement.type) {
        "MASUK" -> Color(0xFF047857)
        "KELUAR" -> Color(0xFFDC2626)
        "PENJUALAN" -> Color(0xFF0284C7)
        else -> Color(0xFFD97706)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = typeBgColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, typeBorderColor),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (movement.type) {
                            "MASUK" -> Icons.Default.ArrowDownward
                            "KELUAR" -> Icons.Default.ArrowUpward
                            "PENJUALAN" -> Icons.Default.PointOfSale
                            else -> Icons.Default.History
                        },
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movement.productName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = movement.reason.ifBlank { movement.type },
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (movement.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ket: ${movement.notes}",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = CurrencyFormatter.formatDate(movement.timestamp),
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )

                if (movement.isNsb && movement.lossAmount > 0.0) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFEF2F2)
                    ) {
                        Text(
                            text = if (movement.costPrice > 0.0) {
                                "NSB: -${CurrencyFormatter.formatRupiah(movement.lossAmount)} (Modal: ${CurrencyFormatter.formatRupiah(movement.costPrice)}/item)"
                            } else {
                                "NSB: -${CurrencyFormatter.formatRupiah(movement.lossAmount)}"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = typeBgColor
                ) {
                    Text(
                        text = if (isPositive) "+${movement.quantity}" else "${movement.quantity}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = typeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${movement.previousStock} → ${movement.currentStock}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkStockAdjustmentModal(
    isStockIn: Boolean,
    products: List<ProductEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirmBulkStockChange: (bulkItems: List<BulkItem>, extraInfo: String, notes: String) -> Unit
) {
    val context = LocalContext.current
    val bulkItems = remember { mutableStateListOf<BulkItem>() }

    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var productDropdownExpanded by remember { mutableStateOf(false) }
    var productSearchQuery by remember { mutableStateOf("") }
    var qtyInput by remember { mutableStateOf("1") }

    var globalExtraInfo by remember { mutableStateOf("") }
    var globalNotes by remember { mutableStateOf("") }
    val reasonOptions = listOf("Rusak/Kadaluarsa", "Hilang", "Penyesuaian")
    var selectedReason by remember { mutableStateOf(reasonOptions[0]) }
    var reasonDropdownExpanded by remember { mutableStateOf(false) }
    var isNsbActive by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, productSearchQuery, isStockIn) {
        val base = if (isStockIn) products else products.filter { it.stock > 0 }
        if (productSearchQuery.isBlank()) {
            base
        } else {
            base.filter {
                it.name.contains(productSearchQuery, ignoreCase = true) ||
                        it.barcode.contains(productSearchQuery, ignoreCase = true) ||
                        it.sku.contains(productSearchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 10.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isStockIn) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isStockIn) Color(0xFFA7F3D0) else Color(0xFFFECACA)
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isStockIn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isStockIn) Color(0xFF047857) else Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isStockIn) "Stok Masuk" else "Stok Keluar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tambah barang ke daftar lalu klik proses",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // SECTION A: Form Pilih Produk & QTY
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Pilih Produk & QTY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )

                            // Dropdown Produk
                            ExposedDropdownMenuBox(
                                expanded = productDropdownExpanded,
                                onExpandedChange = { productDropdownExpanded = !productDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = if (selectedProduct != null) selectedProduct!!.name else productSearchQuery,
                                    onValueChange = {
                                        productSearchQuery = it
                                        selectedProduct = null
                                        productDropdownExpanded = true
                                    },
                                    placeholder = { Text("Cari / pilih produk...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                    },
                                    trailingIcon = {
                                        if (selectedProduct != null || productSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = {
                                                selectedProduct = null
                                                productSearchQuery = ""
                                            }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                            }
                                        } else {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded)
                                        }
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedBorderColor = if (isStockIn) Color(0xFF047857) else Color(0xFFDC2626),
                                        unfocusedContainerColor = Color.White,
                                        focusedContainerColor = Color.White
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = productDropdownExpanded,
                                    onDismissRequest = { productDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(Color.White)
                                        .heightIn(max = 220.dp)
                                ) {
                                    if (filteredProducts.isEmpty()) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (!isStockIn) "Semua produk berstok habis (0) / tidak ditemukan" else "Produk tidak ditemukan",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            },
                                            onClick = { productDropdownExpanded = false }
                                        )
                                    } else {
                                        filteredProducts.forEach { product ->
                                            val catName = categories.find { it.id == product.categoryId }?.name ?: "Umum"
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = product.name,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = Color(0xFF0F172A),
                                                            modifier = Modifier.weight(1f),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "Stok: ${product.stock} ${product.unit}",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF64748B)
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectedProduct = product
                                                    productSearchQuery = product.name
                                                    productDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Selected product info bar
                            if (selectedProduct != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFE0F2FE),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedProduct!!.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0284C7),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Stok Saat Ini: ${selectedProduct!!.stock} ${selectedProduct!!.unit}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0369A1)
                                        )
                                    }
                                }
                            }

                            // Integrated Stepper & Add Button Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Integrated Stepper Control
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.height(42.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val cur = qtyInput.toIntOrNull() ?: 1
                                                if (cur > 1) qtyInput = (cur - 1).toString()
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Kurang", tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .width(48.dp)
                                                .height(30.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isStockIn) Color(0xFF047857) else Color(0xFFDC2626))
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                BasicTextField(
                                                    value = qtyInput,
                                                    onValueChange = { input ->
                                                        val digits = input.filter { c -> c.isDigit() }
                                                        val num = digits.toIntOrNull() ?: 0
                                                        if (!isStockIn && selectedProduct != null && num > selectedProduct!!.stock) {
                                                            qtyInput = selectedProduct!!.stock.toString()
                                                            Toast.makeText(context, "Jumlah maksimal stok '${selectedProduct!!.name}' adalah ${selectedProduct!!.stock} ${selectedProduct!!.unit}!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            qtyInput = digits
                                                        }
                                                    },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    textStyle = androidx.compose.ui.text.TextStyle(
                                                        textAlign = TextAlign.Center,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 14.sp,
                                                        color = if (isStockIn) Color(0xFF047857) else Color(0xFFDC2626)
                                                    ),
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                val cur = qtyInput.toIntOrNull() ?: 0
                                                if (isStockIn || selectedProduct == null || cur < selectedProduct!!.stock) {
                                                    qtyInput = (cur + 1).toString()
                                                } else {
                                                    Toast.makeText(context, "Maksimal stok '${selectedProduct!!.name}' adalah ${selectedProduct!!.stock} ${selectedProduct!!.unit}!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Tambah", tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                // Button Tambah ke Daftar
                                Button(
                                    onClick = {
                                        val prod = selectedProduct
                                        val qtyNum = qtyInput.toIntOrNull() ?: 0
                                        if (prod == null) {
                                            Toast.makeText(context, "Silakan pilih produk terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (!isStockIn) {
                                            if (prod.stock <= 0) {
                                                Toast.makeText(context, "Stok '${prod.name}' sudah habis (0)! Tidak dapat memproses stok keluar / NSB.", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            if (qtyNum > prod.stock) {
                                                Toast.makeText(context, "Jumlah QTY ($qtyNum) melebihi stok yang tersedia (${prod.stock} ${prod.unit})!", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                        }
                                        if (qtyNum <= 0) {
                                            Toast.makeText(context, "Jumlah QTY harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        val existingIndex = bulkItems.indexOfFirst { it.product.id == prod.id }
                                        if (existingIndex >= 0) {
                                            val existing = bulkItems[existingIndex]
                                            val totalQty = existing.qty + qtyNum
                                            if (!isStockIn && totalQty > prod.stock) {
                                                Toast.makeText(context, "Total QTY ($totalQty) untuk '${prod.name}' melebihi stok yang tersedia (${prod.stock} ${prod.unit})!", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            bulkItems[existingIndex] = existing.copy(qty = totalQty)
                                        } else {
                                            bulkItems.add(
                                                BulkItem(
                                                    product = prod,
                                                    qty = qtyNum,
                                                    isNsb = isNsbActive
                                                )
                                            )
                                        }

                                        selectedProduct = null
                                        productSearchQuery = ""
                                        qtyInput = "1"
                                    },
                                    enabled = selectedProduct != null && (qtyInput.toIntOrNull() ?: 0) > 0 && (isStockIn || (qtyInput.toIntOrNull() ?: 0) <= (selectedProduct?.stock ?: 0)),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isStockIn) Color(0xFF047857) else Color(0xFFDC2626),
                                        disabledContainerColor = Color(0xFFE2E8F0),
                                        disabledContentColor = Color(0xFF94A3B8)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tambah ke Daftar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // SECTION B: Daftar Barang (Bulk List)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isStockIn) "Daftar Barang Masuk (${bulkItems.size})" else "Daftar Barang Keluar (${bulkItems.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                            if (bulkItems.isNotEmpty()) {
                                TextButton(
                                    onClick = { bulkItems.clear() },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Kosongkan", fontSize = 12.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (bulkItems.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFFBEB),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Daftar barang masih kosong!",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Pilih produk di atas lalu klik 'Tambah ke Daftar'",
                                            fontSize = 11.sp,
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                bulkItems.forEachIndexed { index, item ->
                                    val newStock = if (isStockIn) item.product.stock + item.qty else (item.product.stock - item.qty).coerceAtLeast(0)
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Row 1: Product Title & Delete Button
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${index + 1}. ${item.product.name}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = { bulkItems.removeAt(index) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                                }
                                            }

                                            // Row 2: Stock calculation on left, Compact Stepper on right
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Stok: ${item.product.stock} → Baru: $newStock ${item.product.unit}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isStockIn) Color(0xFF047857) else Color(0xFFDC2626)
                                                )

                                                // Stepper Pill
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    modifier = Modifier
                                                        .background(Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            if (item.qty > 1) {
                                                                bulkItems[index] = item.copy(qty = item.qty - 1)
                                                            } else {
                                                                bulkItems.removeAt(index)
                                                            }
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.Remove, contentDescription = "Kurang", tint = Color(0xFF0F172A), modifier = Modifier.size(12.dp))
                                                    }

                                                    Text(
                                                        text = if (isStockIn) "+${item.qty}" else "-${item.qty}",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 11.sp,
                                                        color = if (isStockIn) Color(0xFF047857) else Color(0xFFDC2626),
                                                        modifier = Modifier.padding(horizontal = 6.dp)
                                                    )

                                                    IconButton(
                                                        onClick = {
                                                            if (isStockIn || item.qty < item.product.stock) {
                                                                bulkItems[index] = item.copy(qty = item.qty + 1)
                                                            } else {
                                                                Toast.makeText(context, "Maksimal stok '${item.product.name}' hanya ${item.product.stock} ${item.product.unit}!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = "Tambah", tint = Color(0xFF0F172A), modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECTION C: Keterangan / Alasan
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Informasi Transaksi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF1E293B)
                        )

                        if (isStockIn) {
                            OutlinedTextField(
                                value = globalExtraInfo,
                                onValueChange = { globalExtraInfo = it },
                                label = { Text("Nama Supplier / Vendor (Opsional)", fontSize = 12.sp) },
                                placeholder = { Text("Contoh: PT Distributor Jaya", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedBorderColor = Color(0xFF047857)
                                )
                            )
                        } else {
                            // Dropdown Alasan
                            ExposedDropdownMenuBox(
                                expanded = reasonDropdownExpanded,
                                onExpandedChange = { reasonDropdownExpanded = !reasonDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedReason,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Alasan Pengeluaran Barang", fontSize = 12.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedBorderColor = Color(0xFFDC2626)
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = reasonDropdownExpanded,
                                    onDismissRequest = { reasonDropdownExpanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    reasonOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt, fontWeight = if (selectedReason == opt) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) },
                                            onClick = {
                                                selectedReason = opt
                                                reasonDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isNsbActive) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isNsbActive) Color(0xFFFECACA) else Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Catat Kerugian NSB (Nota Selisih Barang)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF991B1B))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Bukukan estimasi kerugian ke Laporan Penjualan", fontSize = 10.sp, color = Color(0xFFB91C1C))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = isNsbActive,
                                        onCheckedChange = { isNsbActive = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFFDC2626)
                                        )
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = globalNotes,
                            onValueChange = { globalNotes = it },
                            label = { Text("Catatan / No. Surat Jalan (Opsional)", fontSize = 12.sp) },
                            placeholder = { Text("Contoh: SJ-2026/10/04-001", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedBorderColor = if (isStockIn) Color(0xFF047857) else Color(0xFFDC2626)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(10.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Batal", fontWeight = FontWeight.Bold, color = Color(0xFF475569), fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (isSubmitting) return@Button
                            if (bulkItems.isEmpty()) {
                                Toast.makeText(context, "Daftar barang tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!isStockIn) {
                                for (item in bulkItems) {
                                    if (item.product.stock <= 0) {
                                        Toast.makeText(context, "Stok '${item.product.name}' sudah habis (0)! Tidak dapat diproses.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (item.qty > item.product.stock) {
                                        Toast.makeText(context, "Jumlah '${item.product.name}' (${item.qty}) melebihi stok tersedia (${item.product.stock} ${item.product.unit})!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                }
                            }

                            isSubmitting = true
                            val finalItems = bulkItems.map { it.copy(isNsb = isNsbActive) }
                            val finalExtra = if (isStockIn) globalExtraInfo else selectedReason
                            onConfirmBulkStockChange(finalItems, finalExtra, globalNotes)
                        },
                        enabled = !isSubmitting && bulkItems.isNotEmpty(),
                        modifier = Modifier
                            .weight(1.8f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isStockIn) Color(0xFF047857) else Color(0xFFDC2626),
                            disabledContainerColor = Color(0xFFE2E8F0),
                            disabledContentColor = Color(0xFF94A3B8)
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSubmitting) "Memproses..." else "Proses (${bulkItems.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
