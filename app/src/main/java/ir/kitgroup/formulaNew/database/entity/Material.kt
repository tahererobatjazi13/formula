package ir.kitgroup.formulaNew.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "materials")
data class Material(
    @PrimaryKey(autoGenerate = true)
    val materialId: Int = 0,

    val materialName: String,
    var price: Double,
    var quantity: Double = 0.0,

    var createdDate: Long = System.currentTimeMillis(),
    var updatedDate: Long = System.currentTimeMillis(),

    var type: String = "material", // "material" یا "packaging"
    var nature: String = "physical" // "physical" یا "virtual"
)

