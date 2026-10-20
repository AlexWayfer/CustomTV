package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.PremiumBannerRequest

/** Under Vote in browser, the free poll card names Premium as the way to vote in the app. */
@Composable
internal fun pollBrowserNote(): String = stringResource(R.string.chat_poll_vote_premium_hint)

/** A choice of an open poll, which the free build cannot vote for, shows the Premium banner. */
internal fun pollLockedPick(): (() -> Unit)? = { PremiumBannerRequest.send("a poll choice") }
