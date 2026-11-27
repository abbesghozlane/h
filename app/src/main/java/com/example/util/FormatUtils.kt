package com.example.util

import androidx.annotation.DrawableRes
import com.example.R
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val latinSymbols = DecimalFormatSymbols(Locale.US)
private val currencyFormatter = DecimalFormat("#,##0.##", latinSymbols)
private val qtyFormatter = DecimalFormat("0.##", latinSymbols)

fun formatMoney(amount: Double, currencySymbol: String = "د.ج"): String {
    val clean = if (abs(amount) < 0.005) 0.0 else amount
    return "${currencyFormatter.format(clean)} $currencySymbol"
}

fun formatNumberPlain(value: Double): String {
    val clean = if (abs(value) < 0.005) 0.0 else value
    return currencyFormatter.format(clean)
}

fun formatDecimalQty(value: Double): String {
    val clean = if (abs(value) < 0.0001) 0.0 else value
    return qtyFormatter.format(clean)
}

fun formatDateTimeArabic(millis: Long): String {
    val fmt = SimpleDateFormat("dd/MM • HH:mm", Locale.US)
    return fmt.format(Date(millis))
}

fun formatShortArabicDay(millis: Long): String {
    val fmt = SimpleDateFormat("EEE", Locale("ar"))
    return fmt.format(Date(millis))
}

@DrawableRes
fun resolvePerfumeDrawable(imageKey: String): Int {
    return when (imageKey) {
        "royal_oud" -> R.drawable.img_perfume_royal_oud
        "black_afgano" -> R.drawable.img_perfume_black_afgano
        "prince_spray" -> R.drawable.img_perfume_prince_spray
        "tobacco_vanille" -> R.drawable.img_perfume_tobacco_vanille
        else -> R.drawable.img_perfume_royal_oud
    }
}

data class PerfumeArtworkOption(
    val key: String,
    val label: String,
    @DrawableRes val resId: Int
)

val PerfumeArtworkOptions = listOf(
    PerfumeArtworkOption("royal_oud", "عود ملكي", R.drawable.img_perfume_royal_oud),
    PerfumeArtworkOption("black_afgano", "زيت خام", R.drawable.img_perfume_black_afgano),
    PerfumeArtworkOption("prince_spray", "كريستال ذهبي", R.drawable.img_perfume_prince_spray),
    PerfumeArtworkOption("tobacco_vanille", "عنبر وفانيلا", R.drawable.img_perfume_tobacco_vanille)
)

fun generateNextLoyaltyCode(existingCodes: Set<String>): String {
    var num = 1040 + existingCodes.size * 7
    while ("PRF-$num" in existingCodes) {
        num += 1
    }
    return "PRF-$num"
}
