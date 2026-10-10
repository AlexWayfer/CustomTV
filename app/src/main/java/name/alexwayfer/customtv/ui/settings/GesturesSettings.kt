package name.alexwayfer.customtv.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R

/** The row in the settings that opens the gestures page. */
@Composable
internal fun GesturesSettingsEntry(onOpen: () -> Unit) {
    SettingsSection(stringResource(R.string.settings_section_help))
    SettingsPageEntry(
        icon = painterResource(R.drawable.ic_swipe),
        title = stringResource(R.string.gestures),
        hint = stringResource(R.string.gestures_hint),
        onOpen = onOpen,
    )
}

private class Gesture(@param:StringRes val title: Int, @param:StringRes val result: Int)

private class GestureGroup(@param:StringRes val title: Int, val gestures: List<Gesture>)

private val GestureGroups = listOf(
    GestureGroup(
        R.string.gestures_section_home,
        listOf(Gesture(R.string.gesture_recent_swipe, R.string.gesture_recent_swipe_result)),
    ),
    GestureGroup(
        R.string.gestures_section_chat,
        listOf(
            Gesture(R.string.gesture_message_swipe, R.string.gesture_message_swipe_result),
            Gesture(R.string.gesture_message_hold, R.string.gesture_message_hold_result),
            Gesture(R.string.gesture_thread_swipe, R.string.gesture_thread_swipe_result),
            Gesture(R.string.gesture_chatter_card_hold, R.string.gesture_chatter_card_hold_result),
        ),
    ),
    GestureGroup(
        R.string.gestures_section_player,
        listOf(
            Gesture(R.string.gesture_player_down, R.string.gesture_player_down_result),
            Gesture(R.string.gesture_player_up, R.string.gesture_player_up_result),
            Gesture(R.string.gesture_mini_player_up, R.string.gesture_mini_player_up_result),
            Gesture(R.string.gesture_mini_player_sideways, R.string.gesture_mini_player_sideways_result),
        ),
    ),
    GestureGroup(
        R.string.gestures_section_fullscreen,
        listOf(
            Gesture(R.string.gesture_fullscreen_down, R.string.gesture_fullscreen_down_result),
            Gesture(R.string.gesture_chat_to_edge, R.string.gesture_chat_to_edge_result),
            Gesture(R.string.gesture_chat_across, R.string.gesture_chat_across_result),
            Gesture(R.string.gesture_video_sideways, R.string.gesture_video_sideways_result),
            Gesture(R.string.gesture_overlay_chat_tap, R.string.gesture_overlay_chat_tap_result),
        ),
    ),
)

/** What each gesture in the app does, by the place it works in. */
@Composable
internal fun GesturesSettingsPage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
        GestureGroups.forEachIndexed { index, group ->
            SettingsSection(stringResource(group.title), first = index == 0)
            group.gestures.forEach { gesture ->
                ListItem(
                    headlineContent = { Text(stringResource(gesture.title)) },
                    supportingContent = { Text(stringResource(gesture.result)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                )
            }
        }
    }
}
