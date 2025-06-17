package ir.kitgroup.formula.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import ir.kitgroup.formula.Util.calculatePricePerKg
import ir.kitgroup.formula.adapter.getTotalPriceForProduct
import ir.kitgroup.formula.adapter.getTotalQuantityForProduct
import ir.kitgroup.formula.database.AppDatabase
import ir.kitgroup.formula.database.entity.Material
import ir.kitgroup.formula.database.entity.MaterialChangeLog
import kotlinx.coroutines.launch

class MaterialViewModel(application: Application) : AndroidViewModel(application) {
    private val materialDao = AppDatabase.getDatabase(application).materialDao()
    private val productDetailDao = AppDatabase.getDatabase(application).productDao()
    val allMaterials: LiveData<List<Material>> = materialDao.getAllMaterials()

    fun updateMaterialAndProductDetails(material: Material) {
        viewModelScope.launch {
            val currentMaterial = materialDao.getMaterialById(material.materialId)
            if (currentMaterial != null) {
                // دریافت محصولات مرتبط و قیمت فعلی‌شان قبل از تغییر ماده
                val relatedProducts = productDetailDao.getProductsByMaterialId(material.materialId)
                val productOldPrices = mutableMapOf<Int, Double>()

                for (product in relatedProducts) {
                    val details = productDetailDao.getDetailsForProduct(product.productId)
                    val oldProductPrice = calculatePricePerKg(
                        getTotalQuantityForProduct(details),
                        getTotalPriceForProduct(details)
                    )
                    productOldPrices[product.productId] = oldProductPrice
                }

                // ثبت لاگ تغییر قیمت ماده
                if (material.price != currentMaterial.price) {
                    material.updatedDate = System.currentTimeMillis()
                    material.createdDate = currentMaterial.createdDate

                    materialDao.insert(
                        MaterialChangeLog(
                            materialId = material.materialId,
                            materialName = material.materialName,
                            changeDate = System.currentTimeMillis(),
                            changeType = 1, // تغییر قیمت ماده
                            oldValue = currentMaterial.price,
                            newValue = material.price
                        )
                    )
                } else {
                    material.updatedDate = currentMaterial.updatedDate
                    material.createdDate = currentMaterial.createdDate
                }

                // آپدیت ماده و جزئیات محصولات
                materialDao.update(material)

                productDetailDao.updatePriceNameByMaterialId(
                    material.materialId,
                    material.materialName,
                    material.price
                )

                // مقایسه و ثبت لاگ تغییرات قیمت محصولات
                for (product in relatedProducts) {
                    val details = productDetailDao.getDetailsForProduct(product.productId)

                    val newProductPrice = calculatePricePerKg(
                        getTotalQuantityForProduct(details),
                        getTotalPriceForProduct(details)
                    )

                    val oldProductPrice = productOldPrices[product.productId] ?: continue

                    if (newProductPrice != oldProductPrice) {
                        product.price = newProductPrice
                        product.updatedDate = System.currentTimeMillis()
                        productDetailDao.updateProduct(product)

                        // ثبت لاگ تغییر قیمت محصول
                        materialDao.insert(
                            MaterialChangeLog(
                                materialId = product.productId,
                                materialName = product.productName,
                                changeDate = System.currentTimeMillis(),
                                changeType = 3, // تغییر قیمت محصول
                                oldValue = oldProductPrice,
                                newValue = newProductPrice
                            )
                        )
                    }
                }
            }
        }
    }

    fun insert(material: Material) = viewModelScope.launch {
        materialDao.insert(material)
    }

    fun delete(material: Material) = viewModelScope.launch {
        materialDao.delete(material)
    }

    fun getChangeLogsForMaterialByType(materialId: Int, changeType: Int): LiveData<List<MaterialChangeLog>> {
        return materialDao.getChangeLogsForMaterialByType(materialId, changeType)
    }
}
