package name.alexwayfer.customtv.ui.settings

import androidx.annotation.StringRes
import name.alexwayfer.customtv.R

/** A license a part of the app comes under. */
internal enum class License(val title: String) {
    Apache2("Apache License 2.0"),
    Gpl3("GNU General Public License v3.0"),
}

/**
 * Something the app is built with, credited on the licenses page: its [name] linking to [url], what the app takes
 * from it ([role]), and its [license].
 */
internal class Credit(val name: String, val url: String, @param:StringRes val role: Int, val license: License)

/** The libraries and icons both editions carry. */
internal val sharedCredits = listOf(
    Credit("Android Jetpack", "https://developer.android.com/jetpack", R.string.credit_jetpack, License.Apache2),
    Credit("Kotlin", "https://kotlinlang.org/", R.string.credit_kotlin, License.Apache2),
    Credit(
        "Material Components for Android",
        "https://github.com/material-components/material-components-android",
        R.string.credit_material_components,
        License.Apache2,
    ),
    Credit("OkHttp", "https://square.github.io/okhttp/", R.string.credit_okhttp, License.Apache2),
    Credit("Coil", "https://coil-kt.github.io/coil/", R.string.credit_coil, License.Apache2),
    Credit(
        "Multiplatform Markdown Renderer",
        "https://github.com/mikepenz/multiplatform-markdown-renderer",
        R.string.credit_markdown_renderer,
        License.Apache2,
    ),
    Credit("Reorderable", "https://github.com/Calvin-LL/Reorderable", R.string.credit_reorderable, License.Apache2),
    Credit("Material Icons", "https://fonts.google.com/icons", R.string.credit_material_icons, License.Apache2),
)

/** Every credit of this edition: the app's own code if it is open, the shared ones, then this edition's own. */
internal val softwareCredits: List<Credit> get() = listOfNotNull(appCredit) + sharedCredits + editionCredits
