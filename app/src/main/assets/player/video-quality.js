(function() {
    var diagnostic = customTvDiagnosticCommand(document, 'quality', 'video-return');
    diagnostic(document.querySelector('video'), 'request');
    var handoff = document.defaultView && document.defaultView.__customTvAudioHandoff;
    if (handoff) handoff.request(false, diagnostic);
    else customTvQualityFlow(document).audio(false, diagnostic);
})();
