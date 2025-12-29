package ir.kitgroup.formulaNew.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "packaging_usage")
data class PackagingUsage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val packagingId: Int,
    val productUsageId: Int,
    val usedWeight: Double,
    val packagingWeight: Double,
)


