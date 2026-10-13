package com.example.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.dao.TopProductSummary
import com.example.data.local.dao.TransactionWithItems
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.ui.KasirViewModel
import com.example.ui.ReportPeriod
import com.example.ui.components.PaginationControlBar
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.paginate
import com.example.util.CurrencyFormatter
import com.example.util.ReceiptPrinterHelper
import java.util.Calendar

@Composable
fun ReportsScreen(viewModel: KasirViewModel) {
    val context = LocalContext.current
    val storeSettings by viewModel.storeSettings.collectAsState()
    val reportPeriod by viewModel.reportPeriod.collectAsState()
    val customStart by viewModel.customStartDate.collectAsState()
    val customEnd by viewModel.customEndDate.collectAsState()
    val summary by viewModel.reportSummary.collectAsState()
    val reportTransactions by viewModel.reportTransactions.collectAsState()
    val reportNsbMovements by viewModel.reportNsbMovements.collectAsState()
    val topProducts by viewModel.reportTopProducts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Ringkasan & Grafik, 1: Riwayat Transaksi, 2: Produk Terlaris
    var transactionForReceipt by remember { mutableStateOf<TransactionWithItems?>(null) }

    fun showStartDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = customStart }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val newStart = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                }.timeInMillis
                viewModel.setCustomReportDateRange(newStart, customEnd)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showEndDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = customEnd }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val newEnd = Calendar.getInstance().apply {
                    set(year, month, day, 23, 59, 59)
                }.timeInMillis
                viewModel.setCustomReportDateRange(customStart, newEnd)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Screen Header Title & Subtitle with Share Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Laporan Penjualan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Omzet, laba bersih, HPP & riwayat transaksi",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            // Share report button
            Button(
                onClick = {
                    val periodLabel = when (reportPeriod) {
                        ReportPeriod.SEMUA -> "Semua Riwayat Penjualan"
                        ReportPeriod.HARIAN -> "Harian (${CurrencyFormatter.formatDateShort(System.currentTimeMillis())})"
                        ReportPeriod.BULANAN -> "Bulanan"
                        ReportPeriod.TAHUNAN -> "Tahunan"
                        ReportPeriod.RENTANG_TANGGAL -> "${CurrencyFormatter.formatDateShort(customStart)} - ${CurrencyFormatter.formatDateShort(customEnd)}"
                    }
                    val text = """
                    *LAPORAN PENJUALAN - ${storeSettings.storeName.uppercase()}*
                    Periode: $periodLabel
                    --------------------------------
                    Total Transaksi: ${summary.totalTransactions} nota
                    Produk Terjual : ${summary.totalItemsSold} item
                    
                    Omset Bersih  : ${CurrencyFormatter.formatRupiah(summary.netSales)}
                    Diskon Diberi : ${CurrencyFormatter.formatRupiah(summary.totalDiscount)}
                    Pajak PPN     : ${CurrencyFormatter.formatRupiah(summary.totalTax)}
                    Estimasi Modal: ${CurrencyFormatter.formatRupiah(summary.totalCostHpp)}
                    Laba Operasi  : ${CurrencyFormatter.formatRupiah(summary.grossOperationalProfit)}
                    Kerugian NSB  : -${CurrencyFormatter.formatRupiah(summary.totalNsbLoss)} (${summary.totalNsbItemsCount} item)
                    --------------------------------
                    *LABA BERSIH* : ${CurrencyFormatter.formatRupiah(summary.estimatedProfit)}
                    --------------------------------
                    Metode Pembayaran:
                    - Tunai : ${CurrencyFormatter.formatRupiah(summary.cashSales)}
                    - QRIS  : ${CurrencyFormatter.formatRupiah(summary.qrisSales)}
                    - Lain  : ${CurrencyFormatter.formatRupiah(summary.transferSales)}
                    """.trimIndent()
                    ReceiptPrinterHelper.shareReceipt(context, text)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bagikan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Period filter chips: Semua, Harian, Bulanan, Tahunan, Rentang Tanggal (Swipeable LazyRow)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = reportPeriod == ReportPeriod.SEMUA,
                    onClick = { viewModel.setReportPeriod(ReportPeriod.SEMUA) },
                    label = { Text("Semua", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                FilterChip(
                    selected = reportPeriod == ReportPeriod.HARIAN,
                    onClick = { viewModel.setReportPeriod(ReportPeriod.HARIAN) },
                    label = { Text("Hari Ini", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                FilterChip(
                    selected = reportPeriod == ReportPeriod.BULANAN,
                    onClick = { viewModel.setReportPeriod(ReportPeriod.BULANAN) },
                    label = { Text("Bulan Ini", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                FilterChip(
                    selected = reportPeriod == ReportPeriod.TAHUNAN,
                    onClick = { viewModel.setReportPeriod(ReportPeriod.TAHUNAN) },
                    label = { Text("Tahun Ini", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                FilterChip(
                    selected = reportPeriod == ReportPeriod.RENTANG_TANGGAL,
                    onClick = { viewModel.setReportPeriod(ReportPeriod.RENTANG_TANGGAL) },
                    label = { Text("Rentang Tanggal", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Custom Date Pickers when RENTANG_TANGGAL is chosen
        if (reportPeriod == ReportPeriod.RENTANG_TANGGAL) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showStartDatePicker() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Start", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dari: ${CurrencyFormatter.formatDateShort(customStart)}", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { showEndDatePicker() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "End", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sampai: ${CurrencyFormatter.formatDateShort(customEnd)}", fontSize = 11.sp)
                }
            }
        }

        // Tabs (Ringkasan, Transaksi, Produk Terlaris)
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Ringkasan", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Transaksi (${reportTransactions.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Produk Terlaris", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        // Content
        when (selectedTab) {
            0 -> SummaryMetricsTab(
                summary = summary,
                nsbMovements = reportNsbMovements
            )
            1 -> TransactionsHistoryTab(
                transactions = reportTransactions,
                onSelectTransaction = { transactionForReceipt = it }
            )
            2 -> TopProductsTab(topProducts)
        }
    }
}

    if (transactionForReceipt != null) {
        ReceiptDialog(
            transactionWithItems = transactionForReceipt!!,
            storeSettings = storeSettings,
            onDismiss = { transactionForReceipt = null }
        )
    }
}

@Composable
fun SummaryMetricsTab(
    summary: com.example.ui.SalesReportSummary,
    nsbMovements: List<StockMovementEntity> = emptyList(),
    onNavigateToNsb: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main Profit Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (summary.totalNsbLoss > 0) "Estimasi Laba Bersih (Setelah NSB)" else "Estimasi Laba Bersih",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46)
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Profit",
                                tint = Color(0xFF006C4C),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = CurrencyFormatter.formatRupiah(summary.estimatedProfit),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF006C4C)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (summary.totalNsbLoss > 0) {
                        "Laba Operasional (${CurrencyFormatter.formatRupiah(summary.grossOperationalProfit)}) - Kerugian NSB (-${CurrencyFormatter.formatRupiah(summary.totalNsbLoss)})"
                    } else {
                        "Omset Bersih dikurangi Total Modal Barang (HPP)"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF047857)
                )
            }
        }

        // Metric 2x2 Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Total Omset Bersih",
                value = CurrencyFormatter.formatRupiah(summary.netSales),
                subtitle = "${summary.totalTransactions} Transaksi",
                color = Color(0xFF0284C7),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Modal Barang (HPP)",
                value = CurrencyFormatter.formatRupiah(summary.totalCostHpp),
                subtitle = "${summary.totalItemsSold} Item Terjual",
                color = Color(0xFF64748B),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Total Diskon Diberikan",
                value = CurrencyFormatter.formatRupiah(summary.totalDiscount),
                subtitle = "Potongan Promo Kasir",
                color = Color(0xFFDC2626),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Kerugian NSB (Selisih)",
                value = CurrencyFormatter.formatRupiah(summary.totalNsbLoss),
                subtitle = "${summary.totalNsbItemsCount} Item Hilang / Rusak",
                color = Color(0xFFDC2626),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Total Pajak PPN",
                value = CurrencyFormatter.formatRupiah(summary.totalTax),
                subtitle = "Pajak Terkumpul",
                color = Color(0xFFD97706),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Pencatatan NSB",
                value = "${summary.nsbTransactionCount} Nota Mutasi",
                subtitle = "Barang Rusak / Hilang",
                color = Color(0xFF7C3AED),
                modifier = Modifier.weight(1f)
            )
        }

        // Accounting Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Rincian Perhitungan Laba & NSB",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Omset Penjualan Bersih", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(CurrencyFormatter.formatRupiah(summary.netSales), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0284C7))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Modal Barang (HPP)", fontSize = 12.sp, color = Color(0xFF475569))
                    Text("- ${CurrencyFormatter.formatRupiah(summary.totalCostHpp)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Laba Kotor Operasional Kasir", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(CurrencyFormatter.formatRupiah(summary.grossOperationalProfit), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF16A34A))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Kerugian NSB (Hilang/Rusak)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFDC2626))
                        if (summary.totalNsbItemsCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("(${summary.totalNsbItemsCount} item)", fontSize = 10.sp, color = Color(0xFFDC2626))
                        }
                    }
                    Text("- ${CurrencyFormatter.formatRupiah(summary.totalNsbLoss)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                }

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Laba Bersih Akhir Toko", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    Text(CurrencyFormatter.formatRupiah(summary.estimatedProfit), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF006C4C))
                }
            }
        }

        // UI Komponen Baru: Ringkasan Akumulatif NSB berdasarkan Kategori Alasan
        NsbCategoryLossSummaryCard(
            summary = summary,
            onCategoryClick = onNavigateToNsb?.let { nav -> { nav() } }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Payment Methods Breakdown
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Breakdown Metode Pembayaran",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                PaymentRow(label = "Tunai (Cash)", amount = summary.cashSales, total = summary.netSales, color = Color(0xFF16A34A))
                Spacer(modifier = Modifier.height(6.dp))
                PaymentRow(label = "QRIS Dinamis / Statis", amount = summary.qrisSales, total = summary.netSales, color = Color(0xFF0284C7))
                Spacer(modifier = Modifier.height(6.dp))
                PaymentRow(label = "Transfer Bank / Lainnya", amount = summary.transferSales, total = summary.netSales, color = Color(0xFF9333EA))
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B), maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 10.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
fun PaymentRow(label: String, amount: Double, total: Double, color: Color) {
    val percent = if (total > 0) (amount / total) * 100 else 0.0
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(
                text = "${CurrencyFormatter.formatRupiah(amount)} (${String.format("%.1f", percent)}%)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (percent / 100f).toFloat().coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun TransactionsHistoryTab(
    transactions: List<TransactionWithItems>,
    onSelectTransaction: (TransactionWithItems) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPaymentFilter by remember { mutableStateOf("SEMUA") }
    var paymentDropdownExpanded by remember { mutableStateOf(false) }
    var txPage by remember { mutableIntStateOf(1) }
    var txPageSize by remember { mutableIntStateOf(10) }

    val filteredTransactions = transactions.filter { tx ->
        val t = tx.transaction
        val matchesQuery = searchQuery.isBlank() ||
                t.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                t.customerName.contains(searchQuery, ignoreCase = true) ||
                t.cashierName.contains(searchQuery, ignoreCase = true) ||
                tx.items.any { it.productName.contains(searchQuery, ignoreCase = true) }

        val matchesPayment = when (selectedPaymentFilter) {
            "TUNAI" -> t.paymentMethod.equals("TUNAI", ignoreCase = true)
            "QRIS" -> t.paymentMethod.equals("QRIS", ignoreCase = true)
            else -> true
        }

        matchesQuery && matchesPayment
    }

    val paymentOptions = listOf(
        "SEMUA" to "Semua",
        "TUNAI" to "Tunai",
        "QRIS" to "QRIS"
    )
    val selectedPaymentLabel = paymentOptions.find { it.first == selectedPaymentFilter }?.second ?: "Semua Pembayaran"

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Box on Left & Payment Method Dropdown on Right (Single Clean Row)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search box on the left
            Surface(
                modifier = Modifier
                    .weight(1.15f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
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
                        contentDescription = "Cari Transaksi",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Cari nota, pelanggan, barang...",
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
                            Icon(Icons.Default.Close, contentDescription = "Hapus", modifier = Modifier.size(14.dp), tint = Color.Gray)
                        }
                    }
                }
            }

            // Payment Method dropdown on the right
            Box(modifier = Modifier.weight(0.95f)) {
                Surface(
                    onClick = { paymentDropdownExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedPaymentFilter != "SEMUA") MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1)
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
                            text = selectedPaymentLabel,
                            fontSize = 11.sp,
                            fontWeight = if (selectedPaymentFilter != "SEMUA") FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedPaymentFilter != "SEMUA") MaterialTheme.colorScheme.primary else Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Pilih Metode Pembayaran",
                            modifier = Modifier.size(20.dp),
                            tint = Color.Gray
                        )
                    }
                }

                DropdownMenu(
                    expanded = paymentDropdownExpanded,
                    onDismissRequest = { paymentDropdownExpanded = false },
                    modifier = Modifier
                        .heightIn(max = 240.dp)
                        .background(Color.White)
                ) {
                    paymentOptions.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedPaymentFilter == key) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedPaymentFilter == key) MaterialTheme.colorScheme.primary else Color.Black
                                )
                            },
                            onClick = {
                                selectedPaymentFilter = key
                                paymentDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedPaymentFilter != "SEMUA")
                            "Tidak ada transaksi yang cocok dengan filter pencarian"
                        else
                            "Tidak ada transaksi penjualan pada periode ini",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            val totalPages = kotlin.math.max(1, kotlin.math.ceil(filteredTransactions.size.toDouble() / txPageSize).toInt())
            val clampedPage = txPage.coerceIn(1, totalPages)
            val paginatedTransactions = filteredTransactions.paginate(clampedPage, txPageSize)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(paginatedTransactions, key = { it.transaction.id }) { tx ->
                    val t = tx.transaction
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTransaction(tx) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = t.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (t.paymentMethod == "QRIS") Color(0xFFE0F2FE) else Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = t.paymentMethod,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (t.paymentMethod == "QRIS") Color(0xFF0284C7) else Color(0xFF16A34A),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${CurrencyFormatter.formatDate(t.timestamp)} - ${t.customerName}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${tx.items.sumOf { it.quantity }} barang (${tx.items.size} jenis)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatRupiah(t.grandTotal),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "Lihat & Cetak Struk",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                item {
                    PaginationControlBar(
                        currentPage = clampedPage,
                        totalPages = totalPages,
                        totalItems = filteredTransactions.size,
                        pageSize = txPageSize,
                        onPageChange = { txPage = it },
                        onPageSizeChange = {
                            txPageSize = it
                            txPage = 1
                        },
                        itemName = "transaksi",
                        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TopProductsTab(topProducts: List<TopProductSummary>) {
    var topPage by remember { mutableIntStateOf(1) }
    var topPageSize by remember { mutableIntStateOf(10) }

    if (topProducts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Belum ada penjualan produk pada periode ini", color = Color.Gray)
        }
    } else {
        val totalPages = kotlin.math.max(1, kotlin.math.ceil(topProducts.size.toDouble() / topPageSize).toInt())
        val clampedPage = topPage.coerceIn(1, totalPages)
        val paginatedProducts = topProducts.paginate(clampedPage, topPageSize)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(paginatedProducts) { itemIndex, item ->
                val globalRank = ((clampedPage - 1) * topPageSize) + itemIndex
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rank Badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when (globalRank) {
                                        0 -> Color(0xFFF59E0B)
                                        1 -> Color(0xFF94A3B8)
                                        2 -> Color(0xFFB45309)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${globalRank + 1}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (globalRank <= 2) Color.White else Color.DarkGray
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.productName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Terjual: ${item.totalQty} unit",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyFormatter.formatRupiah(item.totalSales),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Laba: +${CurrencyFormatter.formatRupiah(item.totalProfit)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF16A34A)
                            )
                        }
                    }
                }
            }

            item {
                PaginationControlBar(
                    currentPage = clampedPage,
                    totalPages = totalPages,
                    totalItems = topProducts.size,
                    pageSize = topPageSize,
                    onPageChange = { topPage = it },
                    onPageSizeChange = {
                        topPageSize = it
                        topPage = 1
                    },
                    itemName = "produk terlaris",
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                )
            }
        }
    }
}

/**
 * UI Komponen Baru: Ringkasan Akumulatif Nota Selisih Barang (NSB)
 * Menampilkan total kerugian akumulatif per kategori alasan:
 * - Hilang (merah)
 * - Rusak / Kadaluarsa (oranye/amber)
 * - Penyesuaian (biru)
 */
@Composable
fun NsbCategoryLossSummaryCard(
    summary: com.example.ui.SalesReportSummary,
    selectedCategoryFilter: String? = null,
    onCategoryClick: ((String) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Total Kerugian + Items Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Total Kerugian:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = CurrencyFormatter.formatRupiah(summary.totalNsbLoss),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFDC2626)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Text(
                        text = "${summary.totalNsbItemsCount} pcs • ${summary.nsbTransactionCount} nota",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF991B1B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Row 2: Slim Proportional Bar (height 4dp)
            if (summary.totalNsbLoss > 0) {
                val total = summary.totalNsbLoss.toFloat()
                val hilangWeight = (summary.nsbHilangLoss.toFloat() / total).coerceIn(0f, 1f)
                val rusakWeight = (summary.nsbRusakLoss.toFloat() / total).coerceIn(0f, 1f)
                val penyesuaianWeight = (summary.nsbPenyesuaianLoss.toFloat() / total).coerceIn(0f, 1f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFE2E8F0))
                ) {
                    if (hilangWeight > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(hilangWeight)
                                .background(Color(0xFFDC2626))
                        )
                    }
                    if (rusakWeight > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(rusakWeight)
                                .background(Color(0xFFD97706))
                        )
                    }
                    if (penyesuaianWeight > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(penyesuaianWeight)
                                .background(Color(0xFF2563EB))
                        )
                    }
                }
            }

            // Row 3: 3 Compact Category Cards (Hilang, Rusak, Penyesuaian)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NsbCategoryItemCard(
                    title = "Hilang",
                    lossAmount = summary.nsbHilangLoss,
                    itemsCount = summary.nsbHilangItems,
                    totalLoss = summary.totalNsbLoss,
                    themeColor = Color(0xFFDC2626),
                    bgColor = Color(0xFFFEF2F2),
                    borderColor = Color(0xFFFECACA),
                    isSelected = selectedCategoryFilter == "Hilang",
                    onClick = onCategoryClick?.let { { it("Hilang") } },
                    modifier = Modifier.weight(1f)
                )

                NsbCategoryItemCard(
                    title = "Rusak",
                    lossAmount = summary.nsbRusakLoss,
                    itemsCount = summary.nsbRusakItems,
                    totalLoss = summary.totalNsbLoss,
                    themeColor = Color(0xFFD97706),
                    bgColor = Color(0xFFFFFBEB),
                    borderColor = Color(0xFFFDE68A),
                    isSelected = selectedCategoryFilter == "Rusak" || selectedCategoryFilter == "Rusak/Kadaluarsa",
                    onClick = onCategoryClick?.let { { it("Rusak/Kadaluarsa") } },
                    modifier = Modifier.weight(1f)
                )

                NsbCategoryItemCard(
                    title = "Penyesuaian",
                    lossAmount = summary.nsbPenyesuaianLoss,
                    itemsCount = summary.nsbPenyesuaianItems,
                    totalLoss = summary.totalNsbLoss,
                    themeColor = Color(0xFF2563EB),
                    bgColor = Color(0xFFEFF6FF),
                    borderColor = Color(0xFFBFDBFE),
                    isSelected = selectedCategoryFilter == "Penyesuaian",
                    onClick = onCategoryClick?.let { { it("Penyesuaian") } },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun NsbCategoryItemCard(
    title: String,
    lossAmount: Double,
    itemsCount: Int,
    totalLoss: Double,
    themeColor: Color,
    bgColor: Color,
    borderColor: Color,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sharePct = if (totalLoss > 0) (lossAmount / totalLoss * 100).toInt() else 0
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) bgColor else Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) themeColor else borderColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColor
                )
                Text(
                    text = "$sharePct%",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColor
                )
            }

            Text(
                text = CurrencyFormatter.formatRupiah(lossAmount),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (lossAmount > 0) themeColor else Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "$itemsCount pcs",
                fontSize = 9.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
fun NsbHistoryTab(
    nsbMovements: List<StockMovementEntity>,
    summary: com.example.ui.SalesReportSummary,
    onDeleteMovement: ((StockMovementEntity) -> Unit)? = null,
    onSeedSampleData: (() -> Unit)? = null,
    onAddNsbClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var categoryFilter by remember { mutableStateOf("Semua") } // "Semua", "Hilang", "Rusak", "Penyesuaian"
    var periodFilter by remember { mutableStateOf("Semua") } // "Semua", "Hari Ini", "Bulan Ini", "Tahun Ini", "Rentang"
    var movementToDelete by remember { mutableStateOf<StockMovementEntity?>(null) }
    var nsbPage by remember { mutableIntStateOf(1) }
    var nsbPageSize by remember { mutableIntStateOf(10) }

    var customStart by remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        mutableLongStateOf(cal.timeInMillis)
    }
    var customEnd by remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        mutableLongStateOf(cal.timeInMillis)
    }

    fun showStartDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = customStart }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val newStart = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                }.timeInMillis
                customStart = newStart
                periodFilter = "Rentang"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showEndDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = customEnd }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val newEnd = Calendar.getInstance().apply {
                    set(year, month, day, 23, 59, 59)
                }.timeInMillis
                customEnd = newEnd
                periodFilter = "Rentang"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val now = System.currentTimeMillis()
    val periodFilteredMovements = remember(nsbMovements, periodFilter, customStart, customEnd) {
        val cal = Calendar.getInstance()
        when (periodFilter) {
            "Hari Ini" -> {
                cal.timeInMillis = now
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis
                nsbMovements.filter { it.timestamp in start..end }
            }
            "Bulan Ini" -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)); cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis
                nsbMovements.filter { it.timestamp in start..end }
            }
            "Tahun Ini" -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_YEAR, 1); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR)); cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis
                nsbMovements.filter { it.timestamp in start..end }
            }
            "Rentang" -> {
                nsbMovements.filter { it.timestamp in customStart..customEnd }
            }
            else -> nsbMovements
        }
    }

    val periodSummary = remember(periodFilteredMovements) {
        var totalLoss = 0.0
        var totalItems = 0
        var hilangLoss = 0.0
        var hilangItems = 0
        var hilangCount = 0
        var rusakLoss = 0.0
        var rusakItems = 0
        var rusakCount = 0
        var penyesuaianLoss = 0.0
        var penyesuaianItems = 0
        var penyesuaianCount = 0

        for (m in periodFilteredMovements) {
            val loss = if (m.lossAmount > 0.0) m.lossAmount else (Math.abs(m.quantity) * m.costPrice)
            val qty = Math.abs(m.quantity)
            totalLoss += loss
            totalItems += qty

            val r = m.reason.lowercase()
            when {
                r.contains("hilang") -> {
                    hilangLoss += loss
                    hilangItems += qty
                    hilangCount++
                }
                r.contains("rusak") || r.contains("kadaluarsa") || r.contains("expired") || r.contains("pecah") || r.contains("basi") -> {
                    rusakLoss += loss
                    rusakItems += qty
                    rusakCount++
                }
                else -> {
                    penyesuaianLoss += loss
                    penyesuaianItems += qty
                    penyesuaianCount++
                }
            }
        }

        com.example.ui.SalesReportSummary(
            totalNsbLoss = totalLoss,
            totalNsbItemsCount = totalItems,
            nsbTransactionCount = periodFilteredMovements.size,
            nsbHilangLoss = hilangLoss,
            nsbHilangItems = hilangItems,
            nsbHilangCount = hilangCount,
            nsbRusakLoss = rusakLoss,
            nsbRusakItems = rusakItems,
            nsbRusakCount = rusakCount,
            nsbPenyesuaianLoss = penyesuaianLoss,
            nsbPenyesuaianItems = penyesuaianItems,
            nsbPenyesuaianCount = penyesuaianCount
        )
    }

    val filteredMovements = remember(periodFilteredMovements, categoryFilter) {
        when (categoryFilter) {
            "Hilang" -> periodFilteredMovements.filter { it.reason.contains("Hilang", ignoreCase = true) }
            "Rusak/Kadaluarsa", "Rusak" -> periodFilteredMovements.filter {
                it.reason.contains("Rusak", ignoreCase = true) ||
                it.reason.contains("Kadaluarsa", ignoreCase = true) ||
                it.reason.contains("Expired", ignoreCase = true) ||
                it.reason.contains("Pecah", ignoreCase = true) ||
                it.reason.contains("Basi", ignoreCase = true)
            }
            "Penyesuaian" -> periodFilteredMovements.filter {
                it.reason.contains("Penyesuaian", ignoreCase = true) ||
                it.reason.contains("Selisih", ignoreCase = true) ||
                it.reason.contains("Opname", ignoreCase = true) ||
                (!it.reason.contains("Hilang", ignoreCase = true) && !it.reason.contains("Rusak", ignoreCase = true) && !it.reason.contains("Kadaluarsa", ignoreCase = true))
            }
            else -> periodFilteredMovements
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Ringkasan Akumulatif NSB per Kategori (Ultra Compact)
        NsbCategoryLossSummaryCard(
            summary = periodSummary,
            selectedCategoryFilter = categoryFilter,
            onCategoryClick = { clickedCat ->
                categoryFilter = if (categoryFilter == clickedCat) "Semua" else clickedCat
            }
        )

        val primaryColor = MaterialTheme.colorScheme.primary
        var showPeriodMenu by remember { mutableStateOf(false) }
        var showCategoryMenu by remember { mutableStateOf(false) }

        // Dua Kolom Filter Berdampingan (Kiri: Filter Periode, Kanan: Filter Kategori)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kolom Kiri: Filter Periode (Dropdown Selector)
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showPeriodMenu = true },
                    shape = RoundedCornerShape(8.dp),
                    color = if (periodFilter != "Semua") primaryColor.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (periodFilter != "Semua") primaryColor else Color(0xFFCBD5E1)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = if (periodFilter != "Semua") primaryColor else Color(0xFF64748B),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (periodFilter == "Rentang") "Rentang" else periodFilter,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (periodFilter != "Semua") primaryColor else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = if (periodFilter != "Semua") primaryColor else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showPeriodMenu,
                    onDismissRequest = { showPeriodMenu = false }
                ) {
                    val periodOptions = listOf("Semua", "Hari Ini", "Bulan Ini", "Tahun Ini", "Rentang")
                    periodOptions.forEach { p ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (p == "Rentang") "Rentang Tanggal" else p,
                                    fontWeight = if (periodFilter == p) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (periodFilter == p) primaryColor else Color(0xFF1E293B)
                                )
                            },
                            onClick = {
                                periodFilter = p
                                showPeriodMenu = false
                                if (p == "Rentang") {
                                    showStartDatePicker()
                                }
                            },
                            leadingIcon = {
                                if (periodFilter == p) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = primaryColor, modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                    }
                }
            }

            // Kolom Kanan: Filter Kategori Alasan (Dropdown Selector)
            Box(modifier = Modifier.weight(1f)) {
                val catColor = when (categoryFilter) {
                    "Hilang" -> Color(0xFFDC2626)
                    "Rusak/Kadaluarsa", "Rusak" -> Color(0xFFD97706)
                    "Penyesuaian" -> Color(0xFF2563EB)
                    else -> primaryColor
                }
                val countText = when (categoryFilter) {
                    "Hilang" -> periodSummary.nsbHilangCount
                    "Rusak/Kadaluarsa", "Rusak" -> periodSummary.nsbRusakCount
                    "Penyesuaian" -> periodSummary.nsbPenyesuaianCount
                    else -> periodFilteredMovements.size
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showCategoryMenu = true },
                    shape = RoundedCornerShape(8.dp),
                    color = if (categoryFilter != "Semua") catColor.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (categoryFilter != "Semua") catColor else Color(0xFFCBD5E1)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = null,
                                tint = if (categoryFilter != "Semua") catColor else Color(0xFF64748B),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (categoryFilter == "Semua") "Semua ($countText)" else "$categoryFilter ($countText)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (categoryFilter != "Semua") catColor else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = if (categoryFilter != "Semua") catColor else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false }
                ) {
                    val catOptions = listOf(
                        Triple("Semua", periodFilteredMovements.size, primaryColor),
                        Triple("Rusak/Kadaluarsa", periodSummary.nsbRusakCount, Color(0xFFD97706)),
                        Triple("Hilang", periodSummary.nsbHilangCount, Color(0xFFDC2626)),
                        Triple("Penyesuaian", periodSummary.nsbPenyesuaianCount, Color(0xFF2563EB))
                    )
                    catOptions.forEach { (cat, count, col) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "$cat ($count)",
                                    fontWeight = if (categoryFilter == cat) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (categoryFilter == cat) col else Color(0xFF1E293B)
                                )
                            },
                            onClick = {
                                categoryFilter = cat
                                showCategoryMenu = false
                            },
                            leadingIcon = {
                                if (categoryFilter == cat) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = col, modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                    }
                }
            }
        }

        // Custom Date Pickers when "Rentang" is chosen (Ultra Compact Row)
        if (periodFilter == "Rentang") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showStartDatePicker() },
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = "Mulai", tint = primaryColor, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = CurrencyFormatter.formatDateShort(customStart),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }

                Text("s/d", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showEndDatePicker() },
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = "Sampai", tint = primaryColor, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = CurrencyFormatter.formatDateShort(customEnd),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }
        }

        // List of items
        if (filteredMovements.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (categoryFilter == "Semua") "Tidak Ada Kerugian NSB" else "Tidak Ada Catatan Kategori $categoryFilter",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tidak ditemukan pencatatan barang pada kategori ini untuk periode yang dipilih.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            val totalPages = kotlin.math.max(1, kotlin.math.ceil(filteredMovements.size.toDouble() / nsbPageSize).toInt())
            val clampedPage = nsbPage.coerceIn(1, totalPages)
            val paginatedMovements = filteredMovements.paginate(clampedPage, nsbPageSize)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(paginatedMovements) { item ->
                    val r = item.reason.lowercase()
                    val isHilang = r.contains("hilang")
                    val isRusak = r.contains("rusak") || r.contains("kadaluarsa") || r.contains("expired") || r.contains("pecah") || r.contains("basi")
                    val loss = if (item.lossAmount > 0.0) item.lossAmount else Math.abs(item.quantity) * item.costPrice

                    val tagColor = when {
                        isHilang -> Color(0xFFDC2626)
                        isRusak -> Color(0xFFD97706)
                        else -> Color(0xFF2563EB)
                    }
                    val tagBg = when {
                        isHilang -> Color(0xFFFEF2F2)
                        isRusak -> Color(0xFFFFFBEB)
                        else -> Color(0xFFEFF6FF)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = item.productName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = tagBg
                                    ) {
                                        Text(
                                            text = item.reason.ifBlank { "NSB" },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = tagColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Jumlah: ${Math.abs(item.quantity)} pcs  •  Modal: ${CurrencyFormatter.formatRupiah(item.costPrice)}/item",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569)
                                )

                                if (item.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Ket: ${item.notes}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = CurrencyFormatter.formatDate(item.timestamp, "EEEE, dd MMM yyyy • HH:mm"),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Kerugian",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "-${CurrencyFormatter.formatRupiah(loss)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFDC2626)
                                )
                                if (onDeleteMovement != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    IconButton(
                                        onClick = { movementToDelete = item },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Hapus Catatan",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    PaginationControlBar(
                        currentPage = clampedPage,
                        totalPages = totalPages,
                        totalItems = filteredMovements.size,
                        pageSize = nsbPageSize,
                        onPageChange = { nsbPage = it },
                        onPageSizeChange = {
                            nsbPageSize = it
                            nsbPage = 1
                        },
                        itemName = "catatan NSB",
                        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                    )
                }
            }
        }
    }

    if (movementToDelete != null) {
        val move = movementToDelete!!
        AlertDialog(
            onDismissRequest = { movementToDelete = null },
            title = { Text("Hapus Catatan NSB?") },
            text = { Text("Hapus riwayat NSB '${move.productName}' senilai ${CurrencyFormatter.formatRupiah(if (move.lossAmount > 0.0) move.lossAmount else Math.abs(move.quantity) * move.costPrice)}? Catatan ini akan dihapus dari mutasi dan perhitungan kerugian.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMovement?.invoke(move)
                        movementToDelete = null
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
}
