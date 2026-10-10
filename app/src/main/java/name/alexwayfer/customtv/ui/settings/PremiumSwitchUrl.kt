package name.alexwayfer.customtv.ui.settings

import android.content.Context
import android.telephony.TelephonyManager
import androidx.core.content.getSystemService

internal const val PREMIUM_SWITCH_URL_RUB = "https://t.me/tribute/app?startapp=s17hV"
internal const val PREMIUM_SWITCH_URL_USD = "https://t.me/tribute/app?startapp=s18rS"

/** SIM countries that see the Premium price in rubles; everyone else, and a device without a SIM, sees dollars. */
private val rubleCountries = setOf("ru", "by")

/** The Premium purchase in the currency for the SIM's country; a dual-SIM value such as `ru,` counts by its first SIM. */
internal fun premiumSwitchUrl(simCountryIso: String?): String {
    val country = simCountryIso?.substringBefore(',')?.trim()?.lowercase()
    return if (country in rubleCountries) PREMIUM_SWITCH_URL_RUB else PREMIUM_SWITCH_URL_USD
}

internal fun premiumSwitchUrl(context: Context): String =
    premiumSwitchUrl(context.getSystemService<TelephonyManager>()?.simCountryIso)
