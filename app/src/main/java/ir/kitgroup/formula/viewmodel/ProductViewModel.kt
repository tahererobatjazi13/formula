package ir.kitgroup.formula.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ir.kitgroup.formula.database.AppDatabase
import ir.kitgroup.formula.database.entity.Material
import ir.kitgroup.formula.database.entity.ProductDetail
import ir.kitgroup.formula.database.entity.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val productDao = AppDatabase.getDatabase(application).productDao()
    val allProducts: LiveData<List<Product>> = productDao.getAllProducts()


    fun delete(product: Product) = viewModelScope.launch {
        productDao.deleteProduct(product)
    }

    fun getAllRawMaterials(): LiveData<List<Material>> {
        val result = MutableLiveData<List<Material>>()
        viewModelScope.launch(Dispatchers.IO) {
            val rawMaterials = productDao.getAllRawMaterials()
            withContext(Dispatchers.Main) {
                result.value = rawMaterials
            }
        }
        return result
    }

    fun getProductDetails(productId: Int): LiveData<List<ProductDetail>> {
        return productDao.getProductDetails(productId)
    }

    fun insertProductWithDetails(
        product: Product,
        totalPrice: Double,
        selectedMaterials: List<Material>,
        selectedProducts: List<Product>
    ) {
        viewModelScope.launch {
            val productId = productDao.insertProduct(product)
            val productDetails = mutableListOf<ProductDetail>()

            selectedMaterials.forEach { material ->
                productDetails.add(
                    ProductDetail(
                        productId = productId.toInt(),
                        materialId = material.materialId,
                        quantity = material.quantity,
                        price = material.price,
                        materialName = material.materialName,
                        materialPrice = material.price,
                        type = 0
                    )
                )
            }

            // ذخیره جزئیات محصولات انتخاب‌شده
            selectedProducts.forEach { selectedProduct ->
                productDetails.add(
                    ProductDetail(
                        productId = productId.toInt(),
                        materialId = selectedProduct.productId,
                        quantity = selectedProduct.quantity,
                        price = totalPrice,
                        materialName = selectedProduct.productName,
                        materialPrice = totalPrice, type = 1
                    )
                )
            }
            productDao.insertProductDetails(productDetails)
        }
    }

    fun updateProductWithDetails(
        product: Product,
        selectedMaterials: List<Material>,
        selectedProducts: List<Product>
    ) {
        viewModelScope.launch {
            productDao.updateProduct(product) // آپدیت اطلاعات محصول اصلی
            productDao.updateNameByMProductId(product.productId, product.productName)

            // ابتدا جزئیات قبلی را حذف کن
            productDao.deleteProductDetailsByProductId(product.productId)

            val productDetails = mutableListOf<ProductDetail>()

            selectedMaterials.forEach { material ->
                productDetails.add(
                    ProductDetail(
                        productId = product.productId,
                        materialId = material.materialId,
                        quantity = material.quantity,
                        price = material.price,
                        materialName = material.materialName,
                        materialPrice = material.price,
                        type = 0
                    )
                )
            }

            selectedProducts.forEach { selectedProduct ->
                productDetails.add(
                    ProductDetail(
                        productId = product.productId,
                        materialId = selectedProduct.productId,
                        quantity = selectedProduct.quantity,
                        price = selectedProduct.price,
                        materialName = selectedProduct.productName,
                        materialPrice = selectedProduct.price,
                        type = 1
                    )
                )
            }

            productDao.insertProductDetails(productDetails)
        }
    }

    suspend fun getProductDetailsSuspend(productId: Int): List<ProductDetail> {
        return productDao.getProductDetailsSuspend(productId)
    }
}
