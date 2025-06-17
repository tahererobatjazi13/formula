package ir.kitgroup.formula.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_details",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["productId"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ProductDetail(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productId: Int,
    val materialId: Int,
    val quantity: Double,
    val price: Double,
    val materialName: String,
    val materialPrice: Double,
    val type: Int,
)

