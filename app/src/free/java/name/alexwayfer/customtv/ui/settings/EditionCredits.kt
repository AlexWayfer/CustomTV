package name.alexwayfer.customtv.ui.settings

import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.update.GITHUB_REPOSITORY_URL

/** The free edition is open source: its own license and code come first on the page. */
internal val appCredit: Credit? = Credit("CustomTV", GITHUB_REPOSITORY_URL, R.string.credit_app, License.Gpl3)

/** The free edition carries only the shared libraries. */
internal val editionCredits = emptyList<Credit>()
