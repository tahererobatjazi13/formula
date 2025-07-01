package ir.kitgroup.formula.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_history",
    foreignKeys = [ForeignKey(
        entity = Product::class,
        parentColumns = ["productId"],
        childColumns = ["productId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("productId")]
)
data class ProductHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Int,
    val quantity: Double,
    val unitPrice: Double,
    val totalPrice: Double,
    val date: Long = System.currentTimeMillis()
)
