package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.PromotionDao
import com.example.data.local.dao.StockMovementDao
import com.example.data.local.dao.StoreSettingDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.UnitDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PromotionEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.StoreSettingEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.entity.UnitEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        StockMovementEntity::class,
        PromotionEntity::class,
        StoreSettingEntity::class,
        UnitEntity::class,
        UserEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class KasirDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun promotionDao(): PromotionDao
    abstract fun storeSettingDao(): StoreSettingDao
    abstract fun unitDao(): UnitDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: KasirDatabase? = null

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE promotions ADD COLUMN tierRules TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE promotions ADD COLUMN multiProductIds TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE promotions ADD COLUMN multiProductNames TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stock_movements ADD COLUMN isNsb INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE stock_movements ADD COLUMN lossAmount REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE store_settings ADD COLUMN userType TEXT NOT NULL DEFAULT 'admin'")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN tenantType TEXT NOT NULL DEFAULT 'admin'")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN role TEXT NOT NULL DEFAULT 'admin'")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        uid TEXT NOT NULL PRIMARY KEY,
                        email TEXT NOT NULL DEFAULT '',
                        name TEXT NOT NULL DEFAULT '',
                        userType TEXT NOT NULL DEFAULT 'admin',
                        tenantType TEXT NOT NULL DEFAULT 'admin',
                        role TEXT NOT NULL DEFAULT 'admin',
                        storeName TEXT NOT NULL DEFAULT '',
                        isActive INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): KasirDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KasirDatabase::class.java,
                    "kasir_kelontong.db"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        // Ensure clean default settings exist without any dummy data
                        database.storeSettingDao().insertOrUpdate(
                            StoreSettingEntity(
                                id = 1,
                                storeName = "Toko Saya",
                                slogan = "",
                                address = "",
                                phoneNumber = "",
                                receiptHeader = "STRUK PEMBELIAN",
                                receiptFooter = "Terima kasih atas kunjungan Anda!",
                                printerPaperSize = "58mm",
                                taxRatePercent = 11.0,
                                isTaxEnabledByDefault = false,
                                qrisMerchantName = "",
                                qrisNmid = "",
                                userType = "admin",
                                tenantType = "admin",
                                role = "admin"
                            )
                        )
                        // Seed default retail units
                        val defaultUnits = listOf(
                            "pcs", "kg", "gr", "pack", "bungkus",
                            "renceng", "dus", "botol", "kaleng", "sachet",
                            "liter", "meter", "porsi", "ikat", "butir", "roll"
                        )
                        val unitEntities = defaultUnits.map { UnitEntity(name = it, isDefault = true) }
                        database.unitDao().insertUnits(unitEntities)
                    }
                }
            }
        }
    }
}
