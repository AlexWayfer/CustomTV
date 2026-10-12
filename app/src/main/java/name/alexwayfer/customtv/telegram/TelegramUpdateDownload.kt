package name.alexwayfer.customtv.telegram

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.ui.UiText
import name.alexwayfer.customtv.update.AppUpdateDownload
import name.alexwayfer.customtv.update.downloadFraction
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

// The highest TDLib priority: the user waits for this file.
private const val DOWNLOAD_PRIORITY = 32

/** Downloads an update's APK from the release group, with its progress in [ui]. */
internal class TelegramUpdateDownload(
    private val scope: CoroutineScope,
    private val ui: MutableStateFlow<TelegramUiState>,
    private val client: () -> Client?,
) {
    fun start(fileId: Int) {
        val current = client() ?: return
        if (ui.value.download is AppUpdateDownload.Running) return
        AppLog.i(TELEGRAM_TAG, "download started")
        ui.update { it.copy(download = AppUpdateDownload.Running(fileId, 0f)) }
        scope.launch {
            val result = current.request(TdApi.DownloadFile(fileId, DOWNLOAD_PRIORITY, 0L, 0L, true))
            val path = (result as? TdApi.File)?.local?.takeIf { it.isDownloadingCompleted }?.path
            val state = when {
                !path.isNullOrEmpty() -> {
                    AppLog.i(TELEGRAM_TAG, "download finished")
                    AppUpdateDownload.Ready(fileId, path)
                }
                result is TdApi.Error -> {
                    AppLog.w(TELEGRAM_TAG, "download failed ${result.code}")
                    AppUpdateDownload.Failed(fileId, UiText.Raw(result.message))
                }
                else -> {
                    AppLog.w(TELEGRAM_TAG, "download stopped ${result.javaClass.simpleName}")
                    AppUpdateDownload.Failed(fileId, UiText.Resource(R.string.telegram_download_stopped))
                }
            }
            ui.update { it.copy(download = state) }
        }
    }

    fun onFileUpdate(file: TdApi.File) {
        ui.update { state ->
            val running = state.download as? AppUpdateDownload.Running
            if (running == null || running.fileId != file.id) return@update state
            val fraction = downloadFraction(file.local?.downloadedSize ?: 0L, file.size, file.expectedSize)
            state.copy(download = running.copy(fraction = fraction))
        }
    }
}
