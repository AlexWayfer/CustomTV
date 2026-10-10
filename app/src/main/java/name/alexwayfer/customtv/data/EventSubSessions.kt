package name.alexwayfer.customtv.data

/**
 * EventSub closes a socket that has no subscription 10 seconds after its welcome. A subscribe answer that arrives
 * after that describes the closed session, and the reconnected socket subscribes again on its own.
 */
internal fun eventSubAnswerOutdated(requestedSession: String, currentSession: String?): Boolean =
    requestedSession != currentSession
