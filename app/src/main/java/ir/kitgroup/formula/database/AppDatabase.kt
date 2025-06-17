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

@Database(
    entities = [Material::class, Product::class, ProductDetail::class, MaterialChangeLog::class],
    version = 3,
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
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                database.execSQL("ALTER TABLE materials ADD COLUMN createdDate INTEGER NOT NULL DEFAULT $now")
                database.execSQL("ALTER TABLE materials ADD COLUMN updatedDate INTEGER NOT NULL DEFAULT $now")
                database.execSQL("ALTER TABLE product ADD COLUMN createdDate INTEGER NOT NULL DEFAULT $now")
                database.execSQL("ALTER TABLE product ADD COLUMN updatedDate INTEGER NOT NULL DEFAULT $now")
                // دستورات برای ایجاد جدول جدید
                database.execSQL(
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //  isFinalProduct اضافه کردن ستون جدید با مقدار پیش‌فرض
                database.execSQL("ALTER TABLE product ADD COLUMN isFinalProduct INTEGER NOT NULL DEFAULT 1")
            }
        }
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
