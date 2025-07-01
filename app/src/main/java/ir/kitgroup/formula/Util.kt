package ir.kitgroup.formula

import android.annotation.SuppressLint
import saman.zamani.persiandate.PersianDate
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object Util {
    fun calculatePrice(weightInGram: Double, pricePerKg: Double): Double {
        return (weightInGram / 1000) * pricePerKg
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

}