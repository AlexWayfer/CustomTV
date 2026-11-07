(function() {
    var diagnostic = customTvDiagnosticCommand(document, 'quality', 'video-return');
    diagnostic(document.querySelector('video'), 'request');
    customTvQualityFlow(document).audio(false, diagnostic);
})();
