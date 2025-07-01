package ir.kitgroup.formula.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ir.kitgroup.formula.Util.calculatePricePerKg
import ir.kitgroup.formula.adapter.getTotalPriceForProduct
import ir.kitgroup.formula.adapter.getTotalQuantityForProduct
import ir.kitgroup.formula.database.AppDatabase
import ir.kitgroup.formula.database.entity.Material
import ir.kitgroup.formula.database.entity.ProductDetail
import ir.kitgroup.formula.database.entity.Product
import ir.kitgroup.formula.database.entity.ProductHistory
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


    private val _price = MutableLiveData<Double>()
    val price: LiveData<Double> = _price

    private val _history = MutableLiveData<List<ProductHistory>>()
    val history: LiveData<List<ProductHistory>> = _history

    private val _lastInsertedId = MutableLiveData<Long?>()
    val lastInsertedId: LiveData<Long?> get() = _lastInsertedId

    @SuppressLint("DefaultLocale")
    fun calculateAndSave(productId: Int, quantity: Double, priceKg: Double) {
        viewModelScope.launch {
            val calculatedPrice = String.format("%.2f", priceKg * quantity).toDouble()
            val history = ProductHistory(
                productId = productId,
                quantity = quantity,
                unitPrice = priceKg,
                totalPrice = calculatedPrice
            )
            val id =
                productDao.insertHistory(history)
            _price.postValue(calculatedPrice)
            _history.postValue(productDao.getHistoryForProduct(productId))
            _lastInsertedId.postValue(id) // ذخیره آخرین ID
        }
    }

    fun resetLastInsertedId() {
        _lastInsertedId.postValue(null)
    }

    fun loadHistory(productId: Int) {
        viewModelScope.launch {
            _history.postValue(productDao.getHistoryForProduct(productId))
        }
    }

    private val _productHistory = MutableLiveData<ProductHistory?>()
    val productHistory: LiveData<ProductHistory?> get() = _productHistory

    fun loadProductHistoryById(id: Long) {
        viewModelScope.launch {
            val result = productDao.getProductHistoryById(id)
            _productHistory.postValue(result)
        }
    }


    private val _processedDetails = MutableLiveData<List<ProductDetail>>()
    val processedDetails: LiveData<List<ProductDetail>> get() = _processedDetails

    fun loadProcessedDetails(type: Int, productId: Int, formattedQty: Double) {
        viewModelScope.launch {
            val rawDetails = productDao.getDetailsForProduct(productId)
            val processedList = mutableListOf<ProductDetail>()

            for (detail in rawDetails) {
                val subDetails = productDao.getDetailsForProduct(detail.productId)
                val totalQty = subDetails.sumOf { it.quantity }
                if (detail.type == 1) {

                    val subDetailsProduct = productDao.getDetailsForProduct(detail.materialId)
                    val pricePerKg = calculatePricePerKg(
                        getTotalQuantityForProduct(subDetailsProduct),
                        getTotalPriceForProduct(subDetailsProduct)
                    )

                    val quantity = detail.quantity
                    val result = quantity * (formattedQty * 1000) / totalQty
                    val resultPrice = (result * pricePerKg) / 1000
                    processedList.add(
                        ProductDetail(
                            id = detail.id,
                            productId = detail.productId,
                            materialId = detail.materialId,
                            quantity = result,
                            price = resultPrice,
                            materialName = detail.materialName,
                            materialPrice = resultPrice,
                            type = 1
                        )
                    )

                } else {

                    if (type == 2) {
                        val quantity = detail.quantity
                        val result = quantity * (formattedQty) / totalQty
                        val resultPrice = (result * detail.materialPrice) / 1000
                        processedList.add(
                            ProductDetail(
                                id = detail.id,
                                productId = detail.productId,
                                materialId = detail.materialId,
                                quantity = result,
                                price = resultPrice,
                                materialName = detail.materialName,
                                materialPrice = resultPrice,
                                type = 0
                            )
                        )

                    } else {
                        val quantity = detail.quantity
                        val result = quantity * (formattedQty * 1000) / totalQty
                        val resultPrice = (result * detail.materialPrice) / 1000

                        processedList.add(
                            ProductDetail(
                                id = detail.id,
                                productId = detail.productId,
                                materialId = detail.materialId,
                                quantity = result,
                                price = resultPrice,
                                materialName = detail.materialName,
                                materialPrice = resultPrice,
                                type = 0
                            )
                        )
                    }
                }
            }
            _processedDetails.postValue(processedList)
        }
    }
}
