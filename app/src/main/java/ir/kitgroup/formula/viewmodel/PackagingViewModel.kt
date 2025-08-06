package ir.kitgroup.formula.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ir.kitgroup.formula.database.AppDatabase
import ir.kitgroup.formula.database.entity.Material
import ir.kitgroup.formula.database.entity.Packaging
import ir.kitgroup.formula.database.entity.PackagingDetail
import ir.kitgroup.formula.database.entity.PackagingUsage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class PackagingViewModel(application: Application) : AndroidViewModel(application) {

    private val packagingDao = AppDatabase.getDatabase(application).packagingDao()

    val allPackagings: LiveData<List<Packaging>> = packagingDao.getAllPackaging()

    private val _totalUsedWeight = MutableLiveData(0.0)
    val totalUsedWeight: LiveData<Double> = _totalUsedWeight

    private val usedWeights = mutableMapOf<Int, Double>()
    private var totalProductWeight: Double = 0.0

    private val _currentUsages = mutableMapOf<Int, Double>() // productUsageId -> usedWeight
    val _packagingUsages = MutableStateFlow<List<PackagingUsage>>(emptyList())

    private val _isEditMode = MutableLiveData<Boolean>()
    val isEditMode: LiveData<Boolean> = _isEditMode


    fun setTotalProductWeight(weight: Double) {
        totalProductWeight = weight
        _totalUsedWeight.value = 0.0
        usedWeights.clear()
    }

    fun updateUsedWeightForPackaging(id: Int, quantity: Double, unitWeight: Double) {
        usedWeights[id] = quantity * unitWeight
        _totalUsedWeight.value = usedWeights.values.sum()
    }

    fun canUseWeight(quantity: Double, unitWeight: Double, id: Int): Boolean {
        val currentSum = usedWeights.values.sum()
        val previous = usedWeights[id] ?: 0.0
        val newUsedWeight = quantity * unitWeight
        val newSum = currentSum - previous + newUsedWeight
        return newSum <= totalProductWeight
    }

    fun loadUsagesForProduct(productUsageId: Long) {
        viewModelScope.launch {
            val usages = packagingDao.getUsagesForProduct(productUsageId)
            _packagingUsages.value = usages

            _currentUsages.clear()
            usedWeights.clear()

            usages.forEach { usage ->
                _currentUsages[usage.packagingId] = usage.usedWeight

                //  مقدار مورد استفاده از هر بسته‌بندی = usedWeight * packagingWeight
                usedWeights[usage.packagingId] = usage.usedWeight * usage.packagingWeight
            }

            calculateTotalUsedWeight(usages)
            _isEditMode.value = usages.isNotEmpty()
        }
    }

    private fun calculateTotalUsedWeight(usages: List<PackagingUsage>) {
        val total = usages.sumOf { it.usedWeight * it.packagingWeight }
        _totalUsedWeight.value = total
    }

    suspend fun saveOrUpdateUsages(
        productId: Int,
        productUsageId: Int,
        selectedPackagings: List<Packaging>,
        isEditMode: Boolean
    ) {
        try {
            if (!isEditMode) {
                val list = selectedPackagings
                    .filter { it.quantity > 0.0 }
                    .map { packaging ->
                        PackagingUsage(
                            productId = productId,
                            packagingId = packaging.packagingId,
                            productUsageId = productUsageId,
                            usedWeight = packaging.quantity,
                            packagingWeight = packaging.weight
                        )
                    }
                if (list.isNotEmpty()) {
                    packagingDao.insertAll(list)
                }
            } else {
                for (packaging in selectedPackagings) {
                    val existing =
                        packagingDao.getPackagingUsage(productUsageId, packaging.packagingId)

                    when {
                        packaging.quantity <= 0.0 -> {
                            if (existing != null) {
                                packagingDao.deletePackagingUsageById(existing.id)
                            }
                        }

                        else -> {
                            val newUsage = PackagingUsage(
                                productId = productId,
                                packagingId = packaging.packagingId,
                                productUsageId = productUsageId,
                                usedWeight = packaging.quantity,
                                packagingWeight = packaging.weight
                            )

                            if (existing != null) {
                                packagingDao.updatePackagingUsage(newUsage.copy(id = existing.id))
                            } else {
                                packagingDao.insertPackagingUsage(newUsage)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    fun getUsedWeightForPackaging(productUsageId: Int): Double {
        return _currentUsages[productUsageId] ?: 0.0
    }

    fun insertPackagingWithDetails(
        packaging: Packaging,
        selectedMaterials: List<Material>
    ) {
        viewModelScope.launch {
            val packagingId = packagingDao.insertPackaging(packaging)
            val packagingDetail = mutableListOf<PackagingDetail>()

            selectedMaterials.forEach { material ->
                packagingDetail.add(
                    PackagingDetail(
                        packagingId = packagingId.toInt(),
                        materialId = material.materialId,
                        quantity = material.quantity,
                        price = material.price,
                        materialName = material.materialName,
                        materialPrice = material.price
                    )
                )
            }
            packagingDao.insertPackagingDetail(packagingDetail)
        }
    }

    fun updatePackagingWithDetails(
        packaging: Packaging,
        selectedMaterials: List<Material>,
    ) {
        viewModelScope.launch {
            packagingDao.updatePackaging(packaging)

            // ابتدا جزئیات قبلی را حذف کن
            packagingDao.deletePackagingDetailsByPackagingId(packaging.packagingId)

            val packagingDetail = mutableListOf<PackagingDetail>()

            selectedMaterials.forEach { material ->
                packagingDetail.add(
                    PackagingDetail(
                        packagingId = packaging.packagingId,
                        materialId = material.materialId,
                        quantity = material.quantity,
                        price = material.price,
                        materialName = material.materialName,
                        materialPrice = material.price
                    )
                )
            }

            packagingDao.insertPackagingDetails(packagingDetail)
        }
    }

    fun deletePackaging(packaging: Packaging) = viewModelScope.launch {
        packagingDao.deletePackaging(packaging)
    }

    fun getPackageDetails(packagingId: Int): LiveData<List<PackagingDetail>> {
        return packagingDao.getPackagingDetails(packagingId)
    }

    fun getAllRawMaterialsByType(type: String): LiveData<List<Material>> {
        return packagingDao.getMaterialsByType(type)
    }

    suspend fun getPackageDetailsSuspend(packagingId: Int): List<PackagingDetail> {
        return packagingDao.getPackagingDetailsSuspend(packagingId)
    }
}