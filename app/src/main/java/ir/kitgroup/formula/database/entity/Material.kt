package ir.kitgroup.formula.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "materials")
data class Material(
    @PrimaryKey(autoGenerate = true)
    val materialId: Int = 0,
    val materialName: String,
    var price: Double,
    var quantity: Double = 0.0,
    var createdDate: Long = System.currentTimeMillis(),  // زمان ایجاد
    var updatedDate: Long = System.currentTimeMillis()   // زمان آخرین ویرایش
)
