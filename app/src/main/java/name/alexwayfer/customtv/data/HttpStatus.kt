package name.alexwayfer.customtv.data

/** 2xx: the request went through. */
internal val HTTP_SUCCESS = 200..299

/** 3xx: the answer points elsewhere. */
internal val HTTP_REDIRECT = 300..399

/** 4xx: this request was rejected, and sending it again unchanged fails the same way. */
internal val HTTP_CLIENT_ERROR = 400..499

/** 5xx: the server failed; a later try may go through. */
internal val HTTP_SERVER_ERROR = 500..599

internal const val HTTP_NOT_MODIFIED = 304
internal const val HTTP_BAD_REQUEST = 400
internal const val HTTP_UNAUTHORIZED = 401
internal const val HTTP_FORBIDDEN = 403
internal const val HTTP_NOT_FOUND = 404
internal const val HTTP_REQUEST_TIMEOUT = 408
internal const val HTTP_CONFLICT = 409
internal const val HTTP_PAYLOAD_TOO_LARGE = 413
internal const val HTTP_UNPROCESSABLE_CONTENT = 422
internal const val HTTP_TOO_EARLY = 425
internal const val HTTP_TOO_MANY_REQUESTS = 429
internal const val HTTP_BAD_GATEWAY = 502
internal const val HTTP_SERVICE_UNAVAILABLE = 503
internal const val HTTP_GATEWAY_TIMEOUT = 504
