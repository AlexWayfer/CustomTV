package name.alexwayfer.customtv.diagnostics

import android.util.Log

/**
 * A crash on [thread]: the exception and its top frame name it, and the whole stack goes with it, after a line
 * with the app's [screen] at that moment.
 */
internal fun crashEvent(thread: Thread, error: Throwable, screen: DiagnosticScreen?): DiagnosticEvent {
    val frame = error.stackTrace.firstOrNull()?.toString()
    return DiagnosticEvent(
        kind = DiagnosticKind.Crash,
        source = thread.name,
        detail = listOfNotNull(error.javaClass.name, frame?.let { "at $it" }).joinToString(" "),
        message = diagnosticScreenLine(screen) + "\n" + Log.getStackTraceString(error),
        firstAtMillis = System.currentTimeMillis(),
    )
}
