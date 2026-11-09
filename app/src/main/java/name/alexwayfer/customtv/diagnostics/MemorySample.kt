package name.alexwayfer.customtv.diagnostics

internal const val MEMORY_SAMPLE_HEADING =
    "\nMemory samples (sizes in bytes unless marked KiB; KeyChars in UTF-16 units):\n"

/** Bounded numeric history, prepared before a crash leaves too little heap to gather diagnostics. */
internal class MemorySampleHistory(private val capacity: Int) {
    private val samples = ArrayDeque<String>()

    @Synchronized
    fun add(line: String) {
        samples.addLast(line)
        while (samples.size > capacity) samples.removeFirst()
    }

    @Synchronized
    fun snapshot(): List<String> = samples.toList()
}

internal data class MemorySample(
    val timeMillis: Long,
    val heapUsed: Long,
    val heapLimit: Long,
    val nativeUsed: Long,
    val replayRows: Int,
    val imageBytes: Long,
) {
    fun line(): String =
        "$timeMillis heap=$heapUsed/$heapLimit native=$nativeUsed images=$imageBytes replayRows=$replayRows"
}
