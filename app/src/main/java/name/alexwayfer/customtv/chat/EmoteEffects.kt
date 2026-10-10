package name.alexwayfer.customtv.chat

/** Visual effects applied by BTTV and FFZ modifier emotes. */
data class EmoteEffects(
    val flipX: Boolean = false,
    val flipY: Boolean = false,
    val widthMultiplier: Float = 1f,
    val rotationDegrees: Float = 0f,
    val rainbow: Boolean = false,
    val party: Boolean = false,
    val hyperRed: Boolean = false,
    val shake: Boolean = false,
    val hyperShake: Boolean = false,
    val cursed: Boolean = false,
    val jam: Boolean = false,
    val bounce: Boolean = false,
    val removeSpaceBefore: Boolean = false,
) {
    fun combine(other: EmoteEffects): EmoteEffects = EmoteEffects(
        flipX = flipX || other.flipX,
        flipY = flipY || other.flipY,
        widthMultiplier = maxOf(widthMultiplier, other.widthMultiplier),
        rotationDegrees = rotationDegrees + other.rotationDegrees,
        rainbow = rainbow || other.rainbow,
        party = party || other.party,
        hyperRed = hyperRed || other.hyperRed,
        shake = shake || other.shake,
        hyperShake = hyperShake || other.hyperShake,
        cursed = cursed || other.cursed,
        jam = jam || other.jam,
        bounce = bounce || other.bounce,
        removeSpaceBefore = removeSpaceBefore || other.removeSpaceBefore,
    )

    /** Whether the emote draws differently from its plain image; [removeSpaceBefore] only moves it. */
    val changesLook: Boolean
        get() = copy(removeSpaceBefore = false) != EmoteEffects()

    companion object {
        // BetterTTV's complete prefix modifier set. `z!` controls zero-width layout,
        // which this text renderer already handles through its overlay emote support.
        fun bttv(token: String): EmoteEffects? = when (token) {
            "w!" -> EmoteEffects(widthMultiplier = 2f)
            "h!" -> EmoteEffects(flipX = true)
            "v!" -> EmoteEffects(flipY = true)
            "l!" -> EmoteEffects(rotationDegrees = -90f)
            "r!" -> EmoteEffects(rotationDegrees = 90f)
            "c!" -> EmoteEffects(cursed = true)
            "p!" -> EmoteEffects(party = true)
            "s!" -> EmoteEffects(shake = true)
            "z!" -> EmoteEffects(removeSpaceBefore = true)
            else -> null
        }

        fun bttvPrefix(text: String): EmoteEffects? {
            val tokens = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (tokens.isEmpty()) return null
            return tokens.fold(EmoteEffects()) { current, token ->
                val effect = bttv(token) ?: return null
                current.combine(effect)
            }
        }

        // These names are also recognized directly by BTTV's modifier parser.
        // They can be applied while FFZ's emote catalog is still loading.
        fun knownFfz(token: String): EmoteEffects? = when (token) {
            "ffzW" -> EmoteEffects(widthMultiplier = 2f)
            "ffzX" -> EmoteEffects(flipX = true)
            "ffzY" -> EmoteEffects(flipY = true)
            "ffzCursed" -> EmoteEffects(cursed = true)
            else -> null
        }

        fun ffz(flags: Int): EmoteEffects = EmoteEffects(
            flipX = flags and FFZ_FLIP_X != 0,
            flipY = flags and FFZ_FLIP_Y != 0,
            widthMultiplier = if (flags and FFZ_GROW_X != 0) 2f else 1f,
            rainbow = flags and FFZ_RAINBOW != 0,
            hyperRed = flags and FFZ_HYPER_RED != 0,
            hyperShake = flags and FFZ_HYPER_SHAKE != 0,
            cursed = flags and FFZ_CURSED != 0,
            jam = flags and FFZ_JAM != 0,
            bounce = flags and FFZ_BOUNCE != 0,
        )

        const val FFZ_HIDDEN = 1
        private const val FFZ_FLIP_X = 2
        private const val FFZ_FLIP_Y = 4
        private const val FFZ_GROW_X = 8
        private const val FFZ_RAINBOW = 2048
        private const val FFZ_HYPER_RED = 4096
        private const val FFZ_HYPER_SHAKE = 8192
        private const val FFZ_CURSED = 16384
        private const val FFZ_JAM = 32768
        private const val FFZ_BOUNCE = 65536
    }
}
