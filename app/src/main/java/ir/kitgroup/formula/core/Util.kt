package ir.kitgroup.formula.core

import android.annotation.SuppressLint
import ir.kitgroup.formula.database.entity.PackagingDetail
import ir.kitgroup.formula.database.entity.ProductDetail
import saman.zamani.persiandate.PersianDate
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object Util {

    val priceFormatter: DecimalFormat = DecimalFormat("#,###,###,###")

    fun calculatePrice(weightInGram: Double, pricePerKg: Double): Double {
        return (weightInGram / 1000) * pricePerKg
    }

    fun calculatePackagingPrice(quantity: Double, price: Double): Double {
        return quantity * price
    }

    fun calculatePricePerKg(weightInGrams: Double, priceForWeight: Double): Double {
        val pricePerGram = priceForWeight / weightInGrams
        return pricePerGram * 1000  // قیمت برای 1 کیلوگرم
    }

    fun formatQuantity(quantity: Double): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }

        return if (quantity % 1 == 0.0) {
            val df = DecimalFormat("#,###", symbols)
            df.format(quantity)
        } else {
            val df = DecimalFormat("#,###.####", symbols)
            df.format(quantity)
        }
    }


    @SuppressLint("DefaultLocale")
    fun formatDateShamsi(timeInMillis: Long): String {
        val persianDate = PersianDate(timeInMillis)
        return "${persianDate.shYear}/${persianDate.shMonth}/${persianDate.shDay} - ${persianDate.hour}:${
            String.format(
                "%02d",
                persianDate.minute
            )
        }"
    }

    fun getTotalPriceForProduct(productDetails: List<ProductDetail>): Double {
        var totalPrice = 0.0
        productDetails.forEach { detail ->
            totalPrice += calculatePrice(detail.materialPrice, detail.quantity)
        }
        return totalPrice
    }

    fun getTotalQuantityForProduct(productDetails: List<ProductDetail>): Double {
        var totalQuantity = 0.0
        productDetails.forEach { detail ->
            totalQuantity += detail.quantity
        }
        return totalQuantity
    }

    fun getTotalPriceForPackaging(productDetails: List<PackagingDetail>): Double {
        var totalPrice = 0.0
        productDetails.forEach { detail ->
            totalPrice += detail.materialPrice * detail.quantity
        }
        return totalPrice
    }

}