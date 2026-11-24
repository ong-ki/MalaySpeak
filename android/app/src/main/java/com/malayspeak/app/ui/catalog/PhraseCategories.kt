package com.malayspeak.app.ui.catalog

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.malayspeak.app.R

/** A single catalogue filter shared by every phrase surface in the app. */
@Immutable
data class PhraseCategory(
    val key: String,
    val labelMm: String,
    val labelEn: String
)

/** The ten shipped categories plus the "everything" filter. */
val PhraseCategories = listOf(
    PhraseCategory("ALL", "အားလုံး", "All phrases"),
    PhraseCategory("GREETINGS", "နှုတ်ဆက်ခြင်း", "Greetings"),
    PhraseCategory("MARKET", "ဈေးဝယ်ခြင်း", "Market"),
    PhraseCategory("DINING", "စားသောက်ဆိုင်", "Dining"),
    PhraseCategory("TRANSPORT", "သွားလာရေး", "Transport"),
    PhraseCategory("WORK", "လုပ်ငန်းခွင်", "Work"),
    PhraseCategory("HOUSING", "အိမ်ငှားနှင့် နေထိုင်ရေး", "Housing"),
    PhraseCategory("NUMBERS_TIME", "ကိန်းဂဏန်းနှင့် အချိန်", "Numbers & time"),
    PhraseCategory("HEALTH_EMERGENCY", "ကျန်းမာရေးနှင့် အရေးပေါ်", "Health & emergency"),
    PhraseCategory("DAILY_LIFE", "နေ့စဉ်လူနေမှု", "Daily life"),
    PhraseCategory("SLANG_PARTICLES", "ကာရန်အသုံးများ", "Slang & particles")
)

/**
 * Interface labels are always English resources. Burmese labels remain only in
 * the catalogue data set for lesson content and search.
 */
@StringRes
fun String.categoryLabelRes(): Int = when (this) {
    "ALL" -> R.string.category_all
    "GREETINGS" -> R.string.category_greetings
    "MARKET" -> R.string.category_market
    "DINING" -> R.string.category_dining
    "TRANSPORT" -> R.string.category_transport
    "WORK" -> R.string.category_work
    "HOUSING" -> R.string.category_housing
    "NUMBERS_TIME" -> R.string.category_numbers_time
    "HEALTH_EMERGENCY" -> R.string.category_health_emergency
    "DAILY_LIFE" -> R.string.category_daily_life
    "SLANG_PARTICLES" -> R.string.category_slang_particles
    else -> R.string.category_all
}

@StringRes
fun PhraseCategory.categoryLabelRes(): Int = key.categoryLabelRes()
