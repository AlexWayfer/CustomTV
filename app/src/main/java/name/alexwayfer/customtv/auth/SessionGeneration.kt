package name.alexwayfer.customtv.auth

internal fun sessionWriteAllowed(startedGeneration: Int, currentGeneration: Int): Boolean {
    return startedGeneration == currentGeneration
}
