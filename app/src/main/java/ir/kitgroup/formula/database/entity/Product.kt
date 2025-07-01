package ir.kitgroup.formula.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val productId: Int = 0,
    val productName: String,
    val date: String = "",
    val description: String = "",
    var quantity: Double = 0.0,
    var price: Double = 0.0,
    val createdDate: Long = System.currentTimeMillis(), // زمان ایجاد
    var updatedDate: Long = System.currentTimeMillis(),// آخرین آپدیت
    val isFinalProduct: Boolean = true // true: نهایی، false: میانی
)

