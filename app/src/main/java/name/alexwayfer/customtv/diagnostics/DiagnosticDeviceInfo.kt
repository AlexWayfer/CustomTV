package name.alexwayfer.customtv.diagnostics

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.telephony.TelephonyManager
import name.alexwayfer.customtv.BuildConfig
import java.io.File

/** The build and the device a report comes from. */
internal fun diagnosticDevice(context: Context) = DiagnosticDevice(
    appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
    premium = BuildConfig.PREMIUM,
    androidRelease = Build.VERSION.RELEASE,
    sdk = Build.VERSION.SDK_INT,
    manufacturer = Build.MANUFACTURER,
    model = Build.MODEL,
    // Without the regional preferences, such as the first day of the week, that ride along as extensions.
    languages = Resources.getSystem().configuration.locales.let { locales ->
        (0 until locales.size()).map { locales[it].stripExtensions().toLanguageTag() }.distinct()
    },
    simCountry = context.getSystemService(TelephonyManager::class.java)?.simCountryIso,
    debug = BuildConfig.DEBUG,
    installedAtMillis = diagnosticInstalledAt(
        packageUpdatedMillis = packageUpdatedMillis(context),
        // Android Studio deploys a changed debug build into this overlay without reinstalling the APK.
        studioOverlayMillis = File(context.codeCacheDir, ".overlay/id").lastModified(),
    ),
)

private fun packageUpdatedMillis(context: Context): Long? = try {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    info.lastUpdateTime
} catch (_: PackageManager.NameNotFoundException) {
    null
}
