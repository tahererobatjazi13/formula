package ir.kitgroup.formulaNew.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "packaging_details",
    foreignKeys = [
        ForeignKey(
            entity = Packaging::class,
            parentColumns = ["packagingId"],
            childColumns = ["packagingId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PackagingDetail(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val packagingId: Int,
    val materialId: Int,
    val quantity: Double,
    val price: Double,
    val materialName: String,
    val materialPrice: Double)

