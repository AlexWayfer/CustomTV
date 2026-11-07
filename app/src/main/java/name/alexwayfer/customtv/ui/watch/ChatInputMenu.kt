package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.chat.chatMentionName
import name.alexwayfer.customtv.ui.settings.animatedControlValue

/** The live chat's settings menu and its shortcuts; a selected moderator hands focus back after it closes. */
@Composable
internal fun ChatInputMenuSheet(input: ChatInput, composer: ChatComposerState, field: ChatFieldState) {
    val menu = input.menu
    val sheetSettings = animatedControlValue(menu.settings)
    val channel = input.channel
    val modesPage = rememberChatModesSubpage(channel.roomModes, channel.modeControl)
    if (!field.menuOpen || sheetSettings == null) return
    ChatSettingsSheet(
        channelLogin = input.channel.login,
        channelId = channel.broadcasterId,
        browserSheetHeightPx = input.channel.browserSheetHeightPx,
        readableColors = sheetSettings.readableColors,
        timestamps = sheetSettings.timestamps,
        textSize = sheetSettings.textSize,
        smoothChatScroll = sheetSettings.smoothChatScroll,
        onReadableColorsChange = menu.onReadableColorsChange,
        onTimestampsChange = menu.onTimestampsChange,
        onTextSizeChange = menu.onTextSizeChange,
        onSmoothChatScrollChange = menu.onSmoothChatScrollChange,
        onRefreshEmotes = menu.onRefreshEmotes,
        onRefreshLabels = menu.onRefreshLabels,
        onOpenOwnProfile = menu.onOpenOwnProfile?.let { open ->
            {
                field.menuOpen = false
                open()
            }
        },
        onViewChatRules = menuRulesOpener(input, composer, field),
        onMentionModerator = {
            field.menuOpen = false
            composer.mentionAfterMenu = chatMentionName(it.login, it.displayName)
        },
        onDismiss = {
            field.menuOpen = false
            input.sheetKeyboard.onDismiss()
        },
        atModerators = field.menuStart == ChatMenuStart.Moderators,
        streamHide = menu.streamHide,
        initialSubpage = modesPage?.takeIf { field.menuStart == ChatMenuStart.Modes },
        firstRow = { openSubpage ->
            ChannelPanelRows(channel.broadcasterId, channel.browserSheetHeightPx, openSubpage)
        },
        modesRow = { openSubpage ->
            ChatModesRow(channel.roomModes, channel.modeControl, modesPage, openSubpage)
        },
        recapRow = { hideThen ->
            menu.recap?.let { source ->
                ChatRecapRow(
                    source = source,
                    onOpen = {
                        hideThen {
                            field.menuOpen = false
                            composer.recapOpen = true
                        }
                    },
                )
            }
        },
    )
}
