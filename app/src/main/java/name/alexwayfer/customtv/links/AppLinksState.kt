package name.alexwayfer.customtv.links

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import name.alexwayfer.customtv.ui.account.openLinkSettings

/**
 * Android 12 and later hand twitch.tv links to the app only once the user adds them in its Open by default settings.
 * Older versions ask which app opens each link, so there is nothing to set up.
 */
internal fun appLinksNeedSetup(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.S

/**
 * The app opens twitch.tv links once supported links are allowed and every host the manifest claims, [claimedHosts],
 * is added.
 */
internal fun appLinksApproved(
    linkHandlingAllowed: Boolean,
    approvedHosts: Set<String>,
    claimedHosts: Set<String>,
): Boolean = linkHandlingAllowed && approvedHosts.containsAll(claimedHosts)

/** Whether Android hands twitch.tv links to the app, read again on every return to the app. */
@Composable
internal fun rememberAppLinksEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember(context) { mutableStateOf(appLinksEnabled(context)) }
    LifecycleResumeEffect(context) {
        enabled = appLinksEnabled(context)
        onPauseOrDispose { }
    }
    return enabled
}

private fun appLinksEnabled(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    val manager = context.getSystemService(DomainVerificationManager::class.java) ?: return false
    val state = try {
        manager.getDomainVerificationUserState(context.packageName)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    } ?: return false
    val approvedHosts = state.hostToStateMap.filterValues { it != DomainVerificationUserState.DOMAIN_STATE_NONE }.keys
    return appLinksApproved(state.isLinkHandlingAllowed, approvedHosts, CLAIMED_LINK_HOSTS)
}

/** Android's Open by default page of the app, where the user adds the twitch.tv links. */
internal fun openAppLinkSettings(context: Context) = openLinkSettings(context, context.packageName)
