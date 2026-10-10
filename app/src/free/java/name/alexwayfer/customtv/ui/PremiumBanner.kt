package name.alexwayfer.customtv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.PremiumBannerStore
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.ui.settings.PremiumFeatureList
import name.alexwayfer.customtv.ui.settings.goldButtonColors
import name.alexwayfer.customtv.ui.settings.premiumUpgradeOpener
import name.alexwayfer.customtv.ui.theme.PremiumGold
import name.alexwayfer.customtv.ui.theme.TwitchBg

/**
 * Offers Premium on a full screen from time to time: when a stream or a recording closes back to the app, or when
 * the user comes back to the home screen, after leaving a player by the recents screen or picture-in-picture.
 * [startSettled] tells that the app has decided whether it reopens a channel on start.
 */
@Composable
internal fun PremiumBanner(
    playerOpen: Boolean,
    inPictureInPicture: Boolean,
    onHome: Boolean,
    startSettled: Boolean,
) {
    val context = LocalContext.current
    val store = remember(context) { PremiumBannerStore(context) }
    val scope = rememberCoroutineScope()
    var wasPlayerOpen by remember { mutableStateOf(playerOpen) }
    var returned by remember { mutableStateOf(false) }
    var visible by rememberSaveable { mutableStateOf(false) }
    suspend fun showIfDue(moment: String) {
        if (!premiumBannerDue(System.currentTimeMillis(), store.shownAtMillis(), store.playerOpens())) return
        AppLog.i(PREMIUM_BANNER_LOG_TAG, "shown $moment")
        visible = true
    }
    LifecycleStartEffect(Unit) {
        returned = true
        onStopOrDispose { }
    }
    LaunchedEffect(playerOpen, inPictureInPicture) {
        val opened = premiumBannerPlayerOpened(wasPlayerOpen, playerOpen)
        val moment = premiumBannerMoment(wasPlayerOpen, playerOpen, inPictureInPicture)
        wasPlayerOpen = playerOpen
        if (opened) scope.launch { store.countPlayerOpen(PREMIUM_BANNER_PLAYER_OPENS) }
        // The banner window takes a few frames to come up, and a tap on the list in them opens a stream under it:
        // the stream wins, and the banner waits for its close.
        if (playerOpen && visible) {
            AppLog.i(PREMIUM_BANNER_LOG_TAG, "dropped for a player")
            visible = false
        }
        if (!moment) return@LaunchedEffect
        // A stream opened from the list right after the close cancels this wait, so the banner never comes up
        // over it only to go away.
        delay(PREMIUM_BANNER_CLOSE_DELAY)
        showIfDue("after the player closed")
    }
    LaunchedEffect(returned, startSettled, playerOpen, inPictureInPicture, onHome) {
        if (!returned || !startSettled) return@LaunchedEffect
        returned = false
        if (premiumBannerReturnMoment(playerOpen, inPictureInPicture, onHome)) showIfDue("on coming back to the app")
    }
    if (visible) {
        PremiumBannerDialog(
            // Only a banner the user has closed counts as shown, not one a stream took over.
            onDismiss = {
                visible = false
                val now = System.currentTimeMillis()
                scope.launch { store.markShown(now) }
            },
        )
    }
}

@Composable
private fun PremiumBannerDialog(onDismiss: () -> Unit) {
    val openUpgrade = premiumUpgradeOpener()
    val screenCorners = rememberScreenCorners()
    val glowAngle by rememberPremiumGlowAngle()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .premiumBannerBackground()
                .premiumGlow(screenCorners, PremiumGlowEdgeStyle, angle = { glowAngle }),
            color = Color.Transparent,
            contentColor = contentColorFor(TwitchBg),
        ) {
            Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                BoxWithConstraints(Modifier.weight(1f)) {
                    // The title on top, the list in between: the room left splits between the gap under the title and
                    // the gap above the buttons, which the last child keeps.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = maxHeight)
                            .padding(top = 58.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Less than the gap below the list: the large title line keeps room under its letters, so the
                        // two gaps look even.
                        PremiumTitle(Modifier.padding(bottom = 12.dp))
                        PremiumFeatureList(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 23.sp),
                            lineSpacing = 6.dp,
                            featureIndent = 0.dp,
                        )
                        // Taller than nothing, so the list sits a bit closer to the title than to the buttons.
                        Spacer(Modifier.height(12.dp))
                    }
                }
                Column(
                    // No top padding: the list above already keeps its gap from the buttons.
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button(
                        onClick = {
                            openUpgrade()
                            onDismiss()
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = goldButtonColors(),
                        contentPadding = PaddingValues(horizontal = 40.dp),
                    ) {
                        Text(stringResource(R.string.premium_upgrade), style = MaterialTheme.typography.titleMedium)
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.premium_not_now))
                    }
                }
            }
        }
    }
}

private const val PREMIUM_BANNER_LOG_TAG = "PremiumBanner"

/** The gold Premium logo and the edition name, where the home screen shows the app name. */
@Composable
private fun PremiumTitle(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_premium_logo),
            contentDescription = null,
            modifier = Modifier.size(38.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.settings_section_premium),
            color = PremiumGold,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
        )
    }
}
