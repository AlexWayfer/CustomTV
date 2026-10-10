package name.alexwayfer.customtv.update

/** A dotted release number, such as 1.1 or 1.1.1. Missing parts count as zero, so 1.1 equals 1.1.0. */
internal class AppVersion private constructor(private val parts: List<Int>) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int {
        for (index in 0 until maxOf(parts.size, other.parts.size)) {
            val diff = parts.getOrElse(index) { 0 }.compareTo(other.parts.getOrElse(index) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    override fun equals(other: Any?): Boolean = other is AppVersion && compareTo(other) == 0

    override fun hashCode(): Int = parts.dropLastWhile { it == 0 }.hashCode()

    override fun toString(): String = parts.joinToString(".")

    companion object {
        val ZERO = AppVersion(listOf(0))

        // The first number in a build's version name, such as 1.0 in "1.0-debug".
        private val NUMBER = Regex("""\d+(?:\.\d+)*""")

        // A file name names the version with or without a v: CustomTV-1.1.apk, CustomTV-premium-v1.1.1.apk.
        private val FILE_VERSION = Regex("""(?<![\d.])\d+(?:\.\d+)+""")

        fun parse(versionName: String): AppVersion? = NUMBER.find(versionName)?.value?.let(::fromDotted)

        /** The version a release file names. A post caption does not count: a changelog names older versions too. */
        fun ofRelease(fileName: String): AppVersion? = FILE_VERSION.find(fileName)?.value?.let(::fromDotted)

        private fun fromDotted(text: String): AppVersion? {
            val parts = text.split('.').map { it.toIntOrNull() ?: return null }
            return AppVersion(parts)
        }
    }
}
