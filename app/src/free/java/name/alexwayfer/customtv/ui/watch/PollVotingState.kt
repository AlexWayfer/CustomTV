package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.chat.ChannelPoll

/** Voting needs the experimental login, a Premium feature: the free poll card offers the browser. */
@Suppress("unused")
@Composable
internal fun rememberPollVoting(poll: ChannelPoll, channelId: String?, expanded: Boolean): PollVoting? = null
