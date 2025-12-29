package ir.kitgroup.formulaNew.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import ir.kitgroup.formulaNew.database.entity.Material
import ir.kitgroup.formulaNew.database.entity.Packaging
import ir.kitgroup.formulaNew.database.entity.PackagingDetail
import ir.kitgroup.formulaNew.database.entity.PackagingUsage

@Dao
interface PackagingDao {

    @Query("SELECT * FROM packaging")
    fun getAllPackaging(): LiveData<List<Packaging>>

    @Delete
    suspend fun deletePackaging(packaging: Packaging)

    @Insert
    suspend fun insertPackagingDetails(productDetails: List<PackagingDetail>)

    @Insert
    suspend fun insertAll(usages: List<PackagingUsage>)

    @Insert
    suspend fun insertPackagingUsage(usage: PackagingUsage)

    @Update
    suspend fun updatePackagingUsage(usage: PackagingUsage)

    @Query("SELECT * FROM packaging_usage WHERE productUsageId = :productUsageId AND packagingId = :packagingId")
    suspend fun getPackagingUsage(productUsageId: Int, packagingId: Int): PackagingUsage?

    @Query("SELECT * FROM packaging_usage WHERE productUsageId = :productUsageId")
    suspend fun getUsagesForProduct(productUsageId: Long): List<PackagingUsage>

    @Insert
    suspend fun insertPackagingDetail(packagingDetail: List<PackagingDetail>)

    @Insert
    suspend fun insertPackaging(packaging: Packaging): Long

    @Query(
        """
       SELECT pd.id, pd.packagingId, pd.materialId, pd.quantity, pd.price, 
       pd.materialName AS materialName, pd.materialPrice AS materialPrice
       FROM packaging_details pd
       LEFT JOIN materials m ON pd.materialId = m.materialId
       WHERE pd.packagingId = :packagingId
    """
    )
    fun getPackagingDetails(packagingId: Int): LiveData<List<PackagingDetail>>

    @Query("DELETE FROM packaging_details WHERE packagingId = :packagingId")
    suspend fun deletePackagingDetailsByPackagingId(packagingId: Int)

    @Query("DELETE FROM packaging_usage WHERE id = :id")
    suspend fun deletePackagingUsageById(id: Int)

    @Update
    suspend fun updatePackaging(packaging: Packaging)

    @Query("SELECT * FROM materials WHERE type = :type")
    fun getMaterialsByType(type: String): LiveData<List<Material>>

    @Query("SELECT * FROM packaging_details WHERE packagingId = :packagingId")
    suspend fun getPackagingDetailsSuspend(packagingId: Int): List<PackagingDetail>

    @Transaction
    @Query(
        """
        SELECT * FROM packaging 
        WHERE packagingId IN (SELECT packagingId FROM packaging_details WHERE materialId = :materialId)
    """
    )
    suspend fun getPackagingsByMaterialId(materialId: Int): List<Packaging>

    @Query("SELECT * FROM packaging_details WHERE packagingId = :packagingId")
    suspend fun getDetailsForPackaging(packagingId: Int): List<PackagingDetail>
}
