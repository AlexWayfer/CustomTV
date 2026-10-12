package name.alexwayfer.customtv.chat

private const val RGB_HEX_DIGITS = 6
private const val HEX_RADIX = 16
private const val OPAQUE_ALPHA = 0xFF000000L

/** A color written as `RRGGBB`, with or without `#`, as an opaque ARGB value; null for anything else. */
internal fun opaqueArgbFromHex(hex: String?): Long? {
    val raw = hex?.trim()?.removePrefix("#")?.takeIf { it.length == RGB_HEX_DIGITS } ?: return null
    return raw.toLongOrNull(HEX_RADIX)?.let { it or OPAQUE_ALPHA }
}
