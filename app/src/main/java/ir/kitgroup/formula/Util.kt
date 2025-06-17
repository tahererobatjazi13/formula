package ir.kitgroup.formula

import saman.zamani.persiandate.PersianDate

object Util {
    fun calculatePrice(weightInGram: Double, pricePerKg: Double): Double {
        return (weightInGram / 1000) * pricePerKg
    }

    fun calculatePricePerKg(weightInGrams: Double, priceForWeight: Double): Double {
        val pricePerGram = priceForWeight / weightInGrams
        return pricePerGram * 1000  // قیمت برای 1 کیلوگرم
    }


    fun formatQuantity(quantity: Double): String {
        return if (quantity % 1 == 0.0) {
            quantity.toInt().toString()
        } else {
            // فرمت با ۴ رقم اعشار و حذف صفرهای انتهایی
            String.format("%.4f", quantity).replace(Regex("0+$"), "").replace(Regex("\\.$"), "")
        }
    }

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