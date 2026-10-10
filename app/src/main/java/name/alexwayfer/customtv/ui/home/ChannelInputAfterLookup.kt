package name.alexwayfer.customtv.ui.home

import name.alexwayfer.customtv.data.ChannelLookup

internal fun channelInputAfterLookup(input: String, result: ChannelLookup): String =
    if (result is ChannelLookup.Found) "" else input
