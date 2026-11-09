package name.alexwayfer.customtv.diagnostics

import java.lang.ref.WeakReference
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatViewModel
import name.alexwayfer.customtv.chat.ChatterHistory
import name.alexwayfer.customtv.chat.PersonalEmotes
import name.alexwayfer.customtv.chat.SevenTvCosmetics

/** Samples immutable flow values without keeping a closed chat or its messages alive. */
internal object LiveChatMemory {
    private val chats = mutableListOf<WeakReference<ChatViewModel>>()

    @Synchronized
    fun track(chat: ChatViewModel) {
        chats.removeAll { it.get() == null }
        if (chats.none { it.get() === chat }) chats.add(WeakReference(chat))
    }

    fun line(): String {
        val retained = synchronized(this) {
            chats.removeAll { it.get() == null }
            chats.mapNotNull { it.get() }
        }
        return "liveChats=${retained.size}" + retained.mapIndexed { index, chat ->
            val counts = liveChatMemoryCounts(
                chat.messages.value,
                chat.chatterHistory.value,
                chat.personalEmotes.value,
                chat.sevenTvCosmetics.value,
            )
            " chat[$index]={chatters=${chat.sessionChatters.value.size} $counts}"
        }.joinToString("")
    }
}

/** Counts references in each collection, not unique messages or the bytes they occupy. Never includes their data. */
internal fun liveChatMemoryCounts(
    messages: List<ChatMessage>,
    history: ChatterHistory,
    personal: PersonalEmotes,
    cosmetics: SevenTvCosmetics,
): String =
    "rows=${messages.size} authors=${history.messages.size} " +
        "authorRows=${history.messages.values.sumOf { it.size }} " +
        "mentionTargets=${history.mentions.size} mentionRows=${history.mentions.values.sumOf { it.size }} " +
        "personalSets=${personal.sevenTvSets.size} personalEmotes=${personal.sevenTvSets.values.sumOf { it.size }} " +
        "personalUsers=${personal.sevenTvUsers.size} bttvUsers=${personal.bttv.size} " +
        "bttvEmotes=${personal.bttv.values.sumOf { it.size }} " +
        "paints=${cosmetics.paints.size} badges=${cosmetics.badges.size} " +
        "paintUsers=${cosmetics.userPaints.size} badgeUsers=${cosmetics.userBadges.size}"
