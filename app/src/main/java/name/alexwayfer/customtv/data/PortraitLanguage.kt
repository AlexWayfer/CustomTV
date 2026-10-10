package name.alexwayfer.customtv.data

/** The language AI chatter portraits are written in. */
enum class PortraitLanguage {
    System,
    English,
    Russian,
}

fun storedPortraitLanguage(value: String?): PortraitLanguage = when (value) {
    PortraitLanguage.English.name -> PortraitLanguage.English
    PortraitLanguage.Russian.name -> PortraitLanguage.Russian
    else -> PortraitLanguage.System
}
