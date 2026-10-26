package name.alexwayfer.customtv.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.components.NoSheetWindowDim
import name.alexwayfer.customtv.ui.components.TextButton
import name.alexwayfer.customtv.ui.GlowCorners
import name.alexwayfer.customtv.ui.PremiumGlowEdgeStyle
import name.alexwayfer.customtv.ui.PremiumGlowStyle
import name.alexwayfer.customtv.ui.premiumBannerBackground
import name.alexwayfer.customtv.ui.premiumGlow
import name.alexwayfer.customtv.ui.rememberPremiumGlowAngle
import name.alexwayfer.customtv.ui.rememberScreenCorners
import name.alexwayfer.customtv.ui.theme.PremiumGold
import name.alexwayfer.customtv.ui.watch.rememberStopAtScrollTop

/** A group of the full Premium list under its [title]. */
private class PremiumFeatureSection(@param:StringRes val title: Int, val features: List<Int>)

/**
 * Everything Premium adds, which the short list only samples. Add a line here with every new Premium feature, and raise
 * the count in `PremiumAllFeaturesTouchTest` with it.
 */
private val premiumFeatureSections = listOf(
    PremiumFeatureSection(
        R.string.premium_section_watching,
        listOf(
            R.string.premium_all_past_broadcasts,
            R.string.premium_all_clips,
            R.string.premium_all_chat_activity,
            R.string.premium_all_explore,
            R.string.premium_all_hidden_categories,
        ),
    ),
    PremiumFeatureSection(
        R.string.premium_section_chat,
        listOf(
            R.string.premium_all_link_previews,
            R.string.premium_all_word_alerts,
            R.string.premium_all_hidden_words,
            R.string.premium_all_highlight_pins,
            R.string.premium_all_chatter_portraits,
            R.string.premium_all_chat_recaps,
            R.string.premium_all_top_chatter_badges,
            R.string.premium_all_donations,
        ),
    ),
    PremiumFeatureSection(
        R.string.premium_section_notifications,
        listOf(
            R.string.premium_all_live_alerts,
            R.string.premium_all_title_alerts,
            R.string.premium_all_alerts_per_channel,
            R.string.premium_all_home_widget,
        ),
    ),
    PremiumFeatureSection(
        R.string.premium_section_moderation,
        listOf(
            R.string.premium_all_bans,
            R.string.premium_all_message_deletion,
            R.string.premium_all_warnings,
            R.string.premium_all_message_menu_moderation,
            R.string.premium_all_chat_modes,
            R.string.premium_all_shoutouts,
            R.string.premium_all_automod,
            R.string.premium_all_mod_logs,
            R.string.premium_all_suspicious_users,
            R.string.premium_all_follow_status,
        ),
    ),
    PremiumFeatureSection(
        R.string.premium_section_experimental,
        listOf(
            R.string.premium_all_whispers,
            R.string.premium_all_watch_streaks,
            R.string.premium_all_channel_points,
            R.string.premium_all_recommendations,
            R.string.premium_all_predictions_and_polls,
            R.string.premium_all_voting,
            R.string.premium_all_following_buttons,
            R.string.premium_all_raider_marks,
            R.string.premium_all_reports,
        ),
    ),
)

/** Closes the short list: opens the full one, and says how many features it holds. */
@Composable
internal fun PremiumAllFeaturesLink(onClick: () -> Unit, style: TextStyle = MaterialTheme.typography.bodyLarge) {
    val count = premiumFeatureSections.sumOf { it.features.size }
    TextButton(
        onClick = onClick,
        // The button's own padding brings its text in line with the heading and the features above; the gap on top
        // sets it apart from the last of them.
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    ) {
        Text(pluralStringResource(R.plurals.premium_all_features, count, count), style = style)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

/** The full Premium list in a sheet, with Upgrade under it in sight however far the list scrolls. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PremiumAllFeaturesSheet(onUpgrade: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Short of the whole screen, so what the sheet opened over stays in sight above it.
    val maxHeight = with(LocalDensity.current) {
        (LocalWindowInfo.current.containerSize.height * PREMIUM_ALL_FEATURES_HEIGHT_SHARE).toDp()
    }
    // The sheet's own rounded top, and the screen's corners at the bottom, where the sheet meets them.
    val sheetCorner = with(LocalDensity.current) { SHEET_CORNER.toPx() }
    val screenCorners = rememberScreenCorners()
    val edgeCorners = GlowCorners(sheetCorner, sheetCorner, screenCorners.bottomRight, screenCorners.bottomLeft)
    val edgeAngle by rememberPremiumGlowAngle()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // The banner's background fills the whole sheet, under its handle and the navigation bar too.
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        NoSheetWindowDim()
        Column(
            Modifier
                .fillMaxWidth()
                .premiumBannerBackground(strength = SHEET_BACKGROUND_STRENGTH)
                .premiumGlow(edgeCorners, SheetEdgeStyle, angle = { edgeAngle })
                .windowInsetsPadding(BottomSheetDefaults.windowInsets),
        ) {
            BottomSheetDefaults.DragHandle(Modifier.align(Alignment.CenterHorizontally))
            PremiumAllFeaturesBody(
                onUpgrade = {
                    onUpgrade()
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                modifier = Modifier.heightIn(max = maxHeight),
            )
        }
    }
}

/** The radius of the sheet's top corners: Material's extra large shape, which the sheet takes by default. */
private val SHEET_CORNER = 28.dp

/** How strong the banner's background is on the sheet: calmer, under a list that has a lot to read. */
private const val SHEET_BACKGROUND_STRENGTH = 0.4f

/** The line along the banner's screen edge, without its glow, so the sheet reads as a piece of the banner. */
private val SheetEdgeStyle = PremiumGlowStyle(
    line = PremiumGlowEdgeStyle.line,
    lineAlpha = 0.55f,
    glow = 0.dp,
    outward = false,
)

/** The most of the window's height the full list's sheet takes. */
private const val PREMIUM_ALL_FEATURES_HEIGHT_SHARE = 0.7f

@Composable
internal fun PremiumAllFeaturesBody(onUpgrade: () -> Unit, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    // A drag back up through the list stops at the top; closing the sheet takes a new drag.
    val overscroll = rememberOverscrollEffect()
    val stopAtTop = rememberStopAtScrollTop(scrollState, overscroll)
    Column(modifier) {
        Text(
            text = stringResource(R.string.premium_all_features_title),
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .nestedScroll(stopAtTop)
                .verticalScroll(scrollState, overscrollEffect = overscroll)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            premiumFeatureSections.forEach { PremiumFeatureSectionLines(it) }
        }
        Button(
            onClick = onUpgrade,
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp)
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .goldButtonFill(),
            colors = goldButtonColors(),
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) {
            Text(stringResource(R.string.premium_upgrade), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun PremiumFeatureSectionLines(section: PremiumFeatureSection) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(section.title),
            modifier = Modifier
                .padding(bottom = 4.dp)
                .semantics { heading() },
            color = PremiumGold,
            style = MaterialTheme.typography.titleMedium,
        )
        section.features.forEach { feature ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val style = MaterialTheme.typography.bodyLarge
                Text(text = "•", modifier = Modifier.clearAndSetSemantics {}, color = PremiumGold, style = style)
                Text(text = stringResource(feature), style = style)
            }
        }
    }
}
