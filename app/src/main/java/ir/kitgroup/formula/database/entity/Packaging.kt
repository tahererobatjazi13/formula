package ir.kitgroup.formula.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "packaging")
data class Packaging(
    @PrimaryKey(autoGenerate = true)
    val packagingId: Int = 0,
    val packagingName: String,
    val description: String = "",
    var quantity: Double = 0.0,
    var weight: Double = 0.0,
    var price: Double = 0.0,
    val createdDate: Long = System.currentTimeMillis(), // زمان ایجاد
    var updatedDate: Long = System.currentTimeMillis()// آخرین آپدیت
)

