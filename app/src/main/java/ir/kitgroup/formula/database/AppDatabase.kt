package ir.kitgroup.formula.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ir.kitgroup.formula.database.dao.MaterialDao
import ir.kitgroup.formula.database.dao.ProductDao
import ir.kitgroup.formula.database.entity.Material
import ir.kitgroup.formula.database.entity.MaterialChangeLog
import ir.kitgroup.formula.database.entity.Product
import ir.kitgroup.formula.database.entity.ProductDetail
import ir.kitgroup.formula.database.entity.ProductHistory

@Database(
    entities = [Material::class, Product::class, ProductDetail::class, MaterialChangeLog::class, ProductHistory::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun materialDao(): MaterialDao
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
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

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
