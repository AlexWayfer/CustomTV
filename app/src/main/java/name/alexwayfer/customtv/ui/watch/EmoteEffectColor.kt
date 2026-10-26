package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.EmoteEffects

/**
 * Whether the color [effects] give an emote changes over time, so drawing it reads the effect clock. A cursed or hyper
 * red emote keeps its color still, as `emoteEffectColorFilter` puts those ahead of rainbow and party.
 */
internal fun emoteEffectColorMoves(effects: EmoteEffects): Boolean =
    !effects.cursed && !effects.hyperRed && (effects.rainbow || effects.party)
