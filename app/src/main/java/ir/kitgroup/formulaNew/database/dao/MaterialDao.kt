package ir.kitgroup.formulaNew.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import ir.kitgroup.formulaNew.database.entity.Material
import ir.kitgroup.formulaNew.database.entity.MaterialChangeLog

@Dao
interface MaterialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(material: Material)

    @Update
    suspend fun update(material: Material)

    @Delete
    suspend fun delete(material: Material)

    @Query("SELECT * FROM materials")
    fun getAllMaterials(): LiveData<List<Material>>

    @Query("SELECT * FROM materials WHERE materialId = :id")
    suspend fun getMaterialById(id: Int): Material?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: MaterialChangeLog)

    @Query(
        """
    SELECT * FROM material_change_logs 
    WHERE materialId = :materialId AND changeType = :changeType 
    ORDER BY changeDate DESC
"""
    )
    fun getChangeLogsForMaterialByType(
        materialId: Int,
        changeType: Int
    ): LiveData<List<MaterialChangeLog>>


    @Query("SELECT COUNT(*) FROM product_details WHERE materialId = :materialId")
    suspend fun getMaterialUsageCount(materialId: Int): Int

    @Query("SELECT COUNT(*) FROM packaging_details WHERE materialId = :packagingId")
    suspend fun getPackagingUsageCount(packagingId: Int): Int

}



