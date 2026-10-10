package name.alexwayfer.customtv.ui.watch

internal data class DeletedMessageAppearance(
    val contentAlpha: Float,
    val linkPreviewAlpha: Float,
)

internal fun deletedMessageAppearance(deleted: Boolean): DeletedMessageAppearance =
    DeletedMessageAppearance(
        contentAlpha = if (deleted) 0.42f else 1f,
        linkPreviewAlpha = 1f,
    )
