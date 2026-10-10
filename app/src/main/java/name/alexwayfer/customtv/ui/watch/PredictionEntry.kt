package name.alexwayfer.customtv.ui.watch

/**
 * Predicting from the chat card, where the build can predict: the viewer's outcome once they predicted, and
 * [onPredict], which opens the channel points sheet on the prediction.
 */
internal class PredictionEntry(val ownOutcomeId: String?, val onPredict: () -> Unit)
