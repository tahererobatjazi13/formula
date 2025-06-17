package ir.kitgroup.formula.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import ir.kitgroup.formula.database.entity.Material
import ir.kitgroup.formula.database.entity.MaterialChangeLog

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

}



