package ir.kitgroup.formula.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "material_change_logs")
data class MaterialChangeLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // کلید اصلی لاگ، نه شناسه‌ی ماده
    val materialId: Int,
    val materialName: String,
    val changeDate: Long,    // تاریخ تغییر (مثلاً System.currentTimeMillis())
    val changeType: Int,     // نوع تغییر (مثلاً 1 برای تغییر قیمت، 2 برای تغییر نام و غیره)
    val oldValue: Double,    // مقدار قبلی
    val newValue: Double     // مقدار جدید
)
