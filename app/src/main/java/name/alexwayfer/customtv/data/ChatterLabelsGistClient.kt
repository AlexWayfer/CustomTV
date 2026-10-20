package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Reads and writes the Chatter Labels files in the user's GitHub Gist. */
internal object ChatterLabelsGistClient {
    const val LABELS_FILE = "labels.json"
    const val ASSIGNMENTS_FILE = "assignments.json"
    private const val TAG = "ChatterLabels"
    private val client = sharedHttpClient.newBuilder().callTimeout(25, TimeUnit.SECONDS).build()

    /**
     * The labels with the gist's ETag, or null when the gist still has [etag]: GitHub answers that with 304, which
     * does not count against the rate limit.
     */
    fun fetchLabels(credentials: ChatterLabelsCredentials, etag: String? = null): ChatterLabelsVersion? {
        val gist = requestFiles(credentials, etag) ?: return null
        val labels = JSONArray(readFile(gist.files, LABELS_FILE) ?: "[]")
        val assignments = JSONArray(readFile(gist.files, ASSIGNMENTS_FILE) ?: "[]")
        return ChatterLabelsVersion(parseChatterLabels(labels, assignments), gist.etag)
    }

    fun fetchFiles(credentials: ChatterLabelsCredentials): JSONObject =
        checkNotNull(requestFiles(credentials, etag = null)).files

    private fun requestFiles(credentials: ChatterLabelsCredentials, etag: String?): GistFiles? {
        val request = gistRequest(credentials)
            .apply { if (etag != null) header("If-None-Match", etag) }
            .build()
        val response = client.newCall(request).execute()
        return response.use {
            if (it.code == HTTP_NOT_MODIFIED) return null
            if (!it.isSuccessful) throw chatterLabelsReadFailure(it.code)
            GistFiles(labelFiles(JSONObject(it.body.string())), it.header("ETag"))
        }
    }

    private class GistFiles(val files: JSONObject, val etag: String?)

    /** The files of [gist], when it holds Chatter Labels data. */
    private fun labelFiles(gist: JSONObject): JSONObject {
        val files = gist.optJSONObject("files") ?: throw ChatterLabelsException(R.string.chatter_labels_error_no_files)
        if (!files.has(LABELS_FILE) && !files.has(ASSIGNMENTS_FILE) &&
            gist.optString("description") != "Chatter Labels — synced data"
        ) {
            throw ChatterLabelsException(R.string.chatter_labels_error_not_labels)
        }
        return files
    }

    /** Writes [files], by name, in one request, so they change together or not at all. */
    fun writeFiles(credentials: ChatterLabelsCredentials, files: Map<String, String>) {
        val filesJson = JSONObject()
        files.forEach { (name, content) -> filesJson.put(name, JSONObject().put("content", content)) }
        val body = JSONObject().put("files", filesJson)
        val request = gistRequest(credentials)
            .patch(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use {
            if (it.isSuccessful) return
            AppLog.w(TAG, "Saving ${files.keys.joinToString()} failed: HTTP ${it.code}")
            // The token and its permissions are the user's to fix, so those answers go to the card, not the report.
            Diagnostics.reportHttp(
                TAG,
                "gists PATCH",
                it.code,
                "",
                setOf(HTTP_UNAUTHORIZED, HTTP_FORBIDDEN, HTTP_NOT_FOUND),
            )
            throw chatterLabelsWriteFailure(it.code)
        }
    }

    private fun gistRequest(credentials: ChatterLabelsCredentials): Request.Builder = Request.Builder()
        .url("https://api.github.com/gists/${credentials.gistId}")
        .header("Authorization", "Bearer ${credentials.token}")
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2026-03-10")

    fun readFile(files: JSONObject, name: String): String? {
        val file = files.optJSONObject(name) ?: return null
        if (!file.optBoolean("truncated")) {
            return file.optString("content").takeIf { it.isNotEmpty() }
                ?: throw ChatterLabelsException(R.string.chatter_labels_error_file_empty, name)
        }
        return downloadTruncatedFile(file, name)
    }

    /** A file too large for the gist answer comes whole from its raw URL, which must be GitHub's. */
    private fun downloadTruncatedFile(file: JSONObject, name: String): String {
        val rawUrl = file.optString("raw_url")
        val url = rawUrl.toHttpUrlOrNull()
        if (url == null || url.scheme != "https" || url.host != "gist.githubusercontent.com") {
            throw ChatterLabelsException(R.string.chatter_labels_error_unexpected_url, name)
        }
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        return response.use {
            if (!it.isSuccessful) throw ChatterLabelsException(R.string.chatter_labels_error_file_http, name, it.code)
            it.body.string()
        }
    }
}
