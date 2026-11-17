(function() {
    var diagnostic = customTvDiagnosticCommand(document, 'quality', 'audio-only');
    diagnostic(document.querySelector('video'), 'request');
    var handoff = typeof customTvAudioHandoff === 'function' && customTvAudioHandoff(document);
    if (handoff) handoff.request(true, diagnostic);
    else customTvQualityFlow(document).audio(true, diagnostic);
})();
