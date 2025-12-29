package ir.kitgroup.formulaNew.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ir.kitgroup.formulaNew.database.dao.MaterialDao
import ir.kitgroup.formulaNew.database.dao.PackagingDao
import ir.kitgroup.formulaNew.database.dao.ProductDao
import ir.kitgroup.formulaNew.database.entity.Material
import ir.kitgroup.formulaNew.database.entity.MaterialChangeLog
import ir.kitgroup.formulaNew.database.entity.Packaging
import ir.kitgroup.formulaNew.database.entity.PackagingDetail
import ir.kitgroup.formulaNew.database.entity.PackagingUsage
import ir.kitgroup.formulaNew.database.entity.Product
import ir.kitgroup.formulaNew.database.entity.ProductDetail
import ir.kitgroup.formulaNew.database.entity.ProductHistory


@Database(
    entities = [Material::class, Product::class, ProductDetail::class, MaterialChangeLog::class, ProductHistory::class, Packaging::class, PackagingDetail::class, PackagingUsage::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun materialDao(): MaterialDao
    abstract fun productDao(): ProductDao
    abstract fun packagingDao(): PackagingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database")
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .addMigrations(MIGRATION_5_6)
                    .addMigrations(MIGRATION_6_7)
                    .addMigrations(MIGRATION_7_8)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                db.execSQL("ALTER TABLE materials ADD COLUMN createdDate INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE materials ADD COLUMN updatedDate INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE product ADD COLUMN createdDate INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE product ADD COLUMN updatedDate INTEGER NOT NULL DEFAULT $now")
                // دستورات برای ایجاد جدول جدید
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `material_change_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `materialId` INTEGER NOT NULL,
                `materialName` TEXT NOT NULL,
                `changeDate` INTEGER NOT NULL,
                `changeType` INTEGER NOT NULL,
                `oldValue` REAL NOT NULL,
                `newValue` REAL NOT NULL
            )
        """
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                //  isFinalProduct اضافه کردن ستون جدید با مقدار پیش‌فرض
                db.execSQL("ALTER TABLE product ADD COLUMN isFinalProduct INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS product_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                productId INTEGER NOT NULL,
                quantity REAL NOT NULL,
                unitPrice REAL NOT NULL,
                totalPrice REAL NOT NULL,
                date INTEGER NOT NULL,
                FOREIGN KEY(productId) REFERENCES product(productId) ON DELETE CASCADE
            )
            """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_product_history_productId ON product_history(productId)")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // افزودن ستون جدید با مقدار پیش‌فرض "material"
                db.execSQL("ALTER TABLE materials ADD COLUMN type TEXT NOT NULL DEFAULT 'material'")
                db.execSQL("ALTER TABLE materials ADD COLUMN nature TEXT NOT NULL DEFAULT 'physical'")

            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS packaging (
                packagingId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                packagingName TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                quantity REAL NOT NULL DEFAULT 0.0,
                weight REAL NOT NULL DEFAULT 0.0,
                price REAL NOT NULL DEFAULT 0.0,
                createdDate INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000),
                updatedDate INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)
            )
            """.trimIndent()
                )
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS packaging_details (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                packagingId INTEGER NOT NULL,
                materialId INTEGER NOT NULL,
                quantity REAL NOT NULL,
                price REAL NOT NULL,
                materialName TEXT NOT NULL,
                materialPrice REAL NOT NULL,
                FOREIGN KEY(packagingId) REFERENCES packaging(packagingId) ON DELETE CASCADE
            )
            """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS packaging_usage (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                productId INTEGER NOT NULL,
                packagingId INTEGER NOT NULL,
                productUsageId INTEGER NOT NULL,
                usedWeight REAL NOT NULL,
                packagingWeight REAL NOT NULL
            )
            """.trimIndent()
                )
            }
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
