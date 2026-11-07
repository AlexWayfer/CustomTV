(function() {
    var diagnostic = customTvDiagnosticCommand(document, 'quality', 'audio-only');
    diagnostic(document.querySelector('video'), 'request');
    customTvQualityFlow(document).audio(true, diagnostic);
})();
