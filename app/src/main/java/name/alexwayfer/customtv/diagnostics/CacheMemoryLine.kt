package name.alexwayfer.customtv.diagnostics

/** Counts key references and UTF-16 units, not heap bytes. No key contents enter the report. */
internal fun cacheMemoryLine(name: String, keys: Collection<String>): String =
    "$name=${keys.size} ${name}KeyChars=${keys.sumOf { it.length.toLong() }}"
