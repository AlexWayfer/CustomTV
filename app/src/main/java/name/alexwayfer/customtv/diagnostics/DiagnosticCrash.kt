package name.alexwayfer.customtv.diagnostics

import android.util.Log

/** A crash on [thread]: the exception and its top frame name it, and the whole stack goes with it. */
internal fun crashEvent(thread: Thread, error: Throwable): DiagnosticEvent {
    val frame = error.stackTrace.firstOrNull()?.toString()
    return DiagnosticEvent(
        kind = DiagnosticKind.Crash,
        source = thread.name,
        detail = listOfNotNull(error.javaClass.name, frame?.let { "at $it" }).joinToString(" "),
        message = Log.getStackTraceString(error),
        firstAtMillis = System.currentTimeMillis(),
    )
}
