(function() {
  customTvLogMediaNetwork();
  // Twitch's player may resume a live stream elsewhere than where it paused; the play waits a moment, so the pause
  // is not undone before it runs. When this does not bring the stream back, the app reloads the page.
  customTvDocs().forEach(function(doc) {
    var output = doc.defaultView && doc.defaultView.__customTvAudioHandoff &&
      doc.defaultView.__customTvAudioHandoff.output();
    var p = output ? output.player : customTvPlayer(doc);
    if (!p || !p.pause || !p.play) return;
    customTvDiagnosticEvent(doc, doc.querySelector('video'), 'restart-request');
    try { p.pause(); } catch (e) { return; }
    setTimeout(function() {
      customTvDiagnosticEvent(doc, doc.querySelector('video'), 'restart-play');
      try { p.play(); } catch (e) {}
    }, 500);
  });
})();
