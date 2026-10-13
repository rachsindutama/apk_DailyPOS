package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.sync.SyncState
import com.example.ui.components.AppNotificationBanner
import com.example.ui.KasirViewModel
import com.example.ui.screens.BarcodeScannerScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.PromotionsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StockMovementScreen
import com.example.ui.screens.StockOpnameScreen
import com.example.ui.theme.KasirTokoKelontongTheme
import kotlinx.coroutines.launch

enum class MainDestination(val label: String, val fullTitle: String, val icon: ImageVector) {
    POS("Kasir", "Kasir Penjualan", Icons.Default.PointOfSale),
    SCANNER("Scan Barcode", "Pindai Barcode & QR", Icons.Default.QrCodeScanner),
    INVENTORY("Inventaris", "Inventaris Produk", Icons.Default.Inventory2),
    MUTASI("Mutasi Stok", "Riwayat Mutasi Stok", Icons.Default.History),
    NSB("NSB", "Nota Selisih Barang", Icons.Default.ReceiptLong),
    STOCK_OPNAME("Stok Opname", "Stok Opname Fisik", Icons.Default.FactCheck),
    CATEGORIES("Kategori", "Kategori Produk", Icons.Default.Category),
    PROMOS("Promosi", "Promosi & Skema Diskon", Icons.Default.Discount),
    REPORTS("Laporan", "Laporan Penjualan & Laba", Icons.Default.Assessment),
    SETTINGS("Pengaturan", "Pengaturan Toko", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: KasirViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KasirTokoKelontongTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: KasirViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val lowStockCount by viewModel.lowStockCount.collectAsState()
    val isOnline by viewModel.cloudSyncManager.isOnline.collectAsState()
    val syncState by viewModel.cloudSyncManager.syncState.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val notificationMsg by viewModel.notificationMessage.collectAsState()

    var showSplash by remember { mutableStateOf(true) }
    var currentDestination by remember { mutableStateOf(MainDestination.POS) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (showSplash) {
        SplashScreen(
            onSplashFinished = { showSplash = false }
        )
        return
    }

    val isSyncing = syncState is SyncState.Syncing
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    if (currentUser == null) {
        LoginScreen(
            viewModel = viewModel,
            onLoginSuccess = { currentDestination = MainDestination.POS }
        )
        return
    }

    // BackHandler: if drawer is open, close it; otherwise return to POS if on another tab
    BackHandler(enabled = drawerState.isOpen || currentDestination != MainDestination.POS) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            currentDestination = MainDestination.POS
        }
    }

    val destinations = MainDestination.values().toList()
    val isSuperadmin = storeSettings.userType.equals("superadmin", ignoreCase = true)

    // Redirection guard: if current destination is SETTINGS but user is not superadmin, redirect to POS
    LaunchedEffect(isSuperadmin, currentDestination) {
        if (currentDestination == MainDestination.SETTINGS && !isSuperadmin) {
            currentDestination = MainDestination.POS
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.width(320.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Sidebar Header (Clean Store Brand Header)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_cash_register),
                                contentDescription = "Logo POS",
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = storeSettings.storeName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (storeSettings.slogan.isNotBlank()) storeSettings.slogan else "Kasir Modern Berbasis Cloud",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // 2. Status Sinkronisasi Container (Wrapped in grey theme box with border)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.cloudSyncManager.triggerAutoSync() },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Status Sinkronisasi",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = when {
                                        !isOnline -> Color.Gray
                                        syncState is SyncState.Syncing -> Color(0xFFD97706)
                                        unsyncedCount > 0 -> Color(0xFFD97706)
                                        else -> Color(0xFF10B981)
                                    },
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when {
                                        !isOnline -> "Mode Offline"
                                        syncState is SyncState.Syncing -> "Menyinkronkan..."
                                        unsyncedCount > 0 -> "$unsyncedCount Belum Sync"
                                        else -> "Cloud Online"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        !isOnline -> Color.Gray
                                        syncState is SyncState.Syncing -> Color(0xFFD97706)
                                        unsyncedCount > 0 -> Color(0xFFD97706)
                                        else -> Color(0xFF047857)
                                    }
                                )
                            }
                        }
                    }

                    // 3. MENU UTAMA Section Header
                    Text(
                        text = "MENU UTAMA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 8.dp)
                    )

                    // 4. Custom Menu Cards
                    val menuList = buildList {
                        add(Triple(MainDestination.POS, "Kasir", "Transaksi penjualan, barcode & QRIS"))
                        add(Triple(MainDestination.INVENTORY, "Inventaris Produk", "Kelola harga jual, stok, mutasi & NSB"))
                        add(Triple(MainDestination.PROMOS, "Promosi & Diskon", "Atur diskon & skema harga grosir"))
                        add(Triple(MainDestination.REPORTS, "Laporan Penjualan", "Omzet, laba bersih, HPP & riwayat struk"))
                        if (isSuperadmin) {
                            add(Triple(MainDestination.SETTINGS, "Pengaturan Toko", "Profil toko, kop struk & NMID QRIS"))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(menuList) { (dest, title, subtitle) ->
                            val isSelected = currentDestination == dest

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        currentDestination = dest
                                        scope.launch { drawerState.close() }
                                    }
                                    .testTag("drawer_item_${dest.name.lowercase()}"),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFFECFDF5) else Color.Transparent,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Icon container
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color(0xFF006C4C) else Color(0xFFF1F5F9),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = dest.icon,
                                                contentDescription = title,
                                                tint = if (isSelected) Color.White else Color(0xFF64748B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color(0xFF064E3B) else Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = subtitle,
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Badge count for inventory low stock
                                    if (dest == MainDestination.INVENTORY && lowStockCount > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFFEF3C7),
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "$lowStockCount",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFD97706)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF006C4C) else Color(0xFFCBD5E1),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 5. Sidebar Footer (Clean User & Logout Bar)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Kasir Aktif",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (storeSettings.userType.equals("superadmin", ignoreCase = true)) Color(0xFFEDE9FE) else Color(0xFFDCFCE7)
                                    ) {
                                        Text(
                                            text = storeSettings.userType.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (storeSettings.userType.equals("superadmin", ignoreCase = true)) Color(0xFF6D28D9) else Color(0xFF15803D),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentUser?.email ?: "admin@gmail.com",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    viewModel.logout()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Keluar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_cash_register),
                                contentDescription = "Logo Kasir",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = storeSettings.storeName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A),
                                    lineHeight = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (storeSettings.slogan.isNotBlank()) storeSettings.slogan else "Kasir Modern Berbasis Cloud",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B),
                                    lineHeight = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("hamburger_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Buka Sidebar Menu"
                            )
                        }
                    },
                    actions = {
                        // Full 2-Way Sync SQLite <-> Firestore
                        IconButton(
                            onClick = {
                                scope.launch {
                                    Toast.makeText(context, "Menyinkronkan...", Toast.LENGTH_SHORT).show()
                                    viewModel.cloudSyncManager.triggerAutoSync()
                                    viewModel.cloudSyncManager.pullDataFromCloud()
                                    Toast.makeText(context, "Sinkronisasi Berhasil!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("top_bar_sync_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sinkronkan Data",
                                tint = when {
                                    !isOnline -> Color.Gray
                                    isSyncing -> Color(0xFFD97706)
                                    unsyncedCount > 0 -> Color(0xFFD97706)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = if (isSyncing) Modifier.rotate(angle) else Modifier
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                ScreenContent(
                    destination = currentDestination,
                    viewModel = viewModel,
                    onNavigateToDestination = { currentDestination = it }
                )

                AppNotificationBanner(
                    message = notificationMsg,
                    onDismiss = { viewModel.clearNotification() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
fun ScreenContent(
    destination: MainDestination,
    viewModel: KasirViewModel,
    onNavigateToDestination: (MainDestination) -> Unit
) {
    Crossfade(targetState = destination, label = "screen_transition") { dest ->
        when (dest) {
            MainDestination.POS -> PosScreen(
                viewModel = viewModel,
                onNavigateToInventory = { onNavigateToDestination(MainDestination.INVENTORY) },
                onNavigateToScanner = { onNavigateToDestination(MainDestination.SCANNER) }
            )
            MainDestination.SCANNER -> BarcodeScannerScreen(
                viewModel = viewModel,
                onNavigateToCheckout = { onNavigateToDestination(MainDestination.POS) }
            )
            MainDestination.INVENTORY -> InventoryScreen(
                viewModel = viewModel,
                onNavigateToCategories = { onNavigateToDestination(MainDestination.CATEGORIES) }
            )
            MainDestination.STOCK_OPNAME -> StockOpnameScreen(viewModel = viewModel)
            MainDestination.REPORTS -> ReportsScreen(viewModel = viewModel)
            MainDestination.MUTASI -> InventoryScreen(
                viewModel = viewModel,
                onNavigateToCategories = { onNavigateToDestination(MainDestination.CATEGORIES) },
                initialTab = 1
            )
            MainDestination.NSB -> InventoryScreen(
                viewModel = viewModel,
                onNavigateToCategories = { onNavigateToDestination(MainDestination.CATEGORIES) },
                initialTab = 2
            )
            MainDestination.PROMOS -> PromotionsScreen(viewModel = viewModel)
            MainDestination.CATEGORIES -> InventoryScreen(viewModel = viewModel, onNavigateToCategories = {})
            MainDestination.SETTINGS -> {
                val isSuper = viewModel.storeSettings.collectAsState().value.userType.equals("superadmin", ignoreCase = true)
                if (isSuper) {
                    SettingsScreen(viewModel = viewModel)
                } else {
                    PosScreen(
                        viewModel = viewModel,
                        onNavigateToInventory = { onNavigateToDestination(MainDestination.INVENTORY) },
                        onNavigateToScanner = { onNavigateToDestination(MainDestination.SCANNER) }
                    )
                }
            }
        }
    }
}
