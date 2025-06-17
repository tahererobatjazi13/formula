package ir.kitgroup.formula.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import ir.kitgroup.formula.database.entity.Material
import ir.kitgroup.formula.database.entity.ProductDetail
import ir.kitgroup.formula.database.entity.Product


@Dao
interface ProductDao {

    @Insert
    suspend fun insertProduct(product: Product): Long

    @Insert
    suspend fun insertProductDetails(productDetails: List<ProductDetail>)

    @Update
    suspend fun updateProductDetails(productDetails: List<ProductDetail>)

    @Insert
    suspend fun insertRawMaterial(material: Material)

    @Query("SELECT * FROM materials")
    suspend fun getAllRawMaterials(): List<Material>


    @Query("SELECT * FROM product")
    fun getAllProducts(): LiveData<List<Product>>

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query(
        """
       SELECT pd.id, pd.productId, pd.materialId, pd.quantity, pd.price, 
       pd.materialName AS materialName, pd.materialPrice AS materialPrice, pd.type
       FROM product_details pd
       LEFT JOIN materials m ON pd.materialId = m.materialId
       WHERE pd.productId = :productId
    """
    )
    fun getProductDetails(productId: Int): LiveData<List<ProductDetail>>

    @Query("DELETE FROM product_details WHERE productId = :productId")
    suspend fun deleteProductDetailsByProductId(productId: Int)

    @Query("SELECT * FROM PRODUCT WHERE productId = :productId LIMIT 1")
    fun getProductById(productId: Int): LiveData<Product>

    @Query("UPDATE product_details SET materialPrice = :newPrice, price = quantity * :newPrice WHERE materialId = :materialId")
    suspend fun updatePriceByMaterialId(materialId: Int, newPrice: Double)

    @Query(
        """
    UPDATE product_details 
    SET materialName = :newName, 
        materialPrice = :newPrice, 
        price = quantity * :newPrice 
    WHERE materialId = :materialId AND type = 0
"""
    )
    suspend fun updatePriceNameByMaterialId(
        materialId: Int,
        newName: String,
        newPrice: Double
    )

    @Query(
        """
    UPDATE product_details 
    SET materialName = :newName 
    WHERE materialId = :materialId AND type = 1
"""
    )
    suspend fun updateNameByMProductId(
        materialId: Int,
        newName: String
    )

    @Query("SELECT * FROM product_details WHERE productId = :productId")
    suspend fun getProductDetailsSuspend(productId: Int): List<ProductDetail>

    // متدی برای دریافت محصولات مرتبط با یک ماده اولیه خاص
    @Transaction
    @Query(
        """
        SELECT * FROM product 
        WHERE productId IN (SELECT productId FROM product_details WHERE materialId = :materialId)
    """
    )
    suspend fun getProductsByMaterialId(materialId: Int): List<Product>

    @Query("SELECT * FROM product_details WHERE productId = :productId")
    suspend fun getDetailsForProduct(productId: Int): List<ProductDetail>


}



