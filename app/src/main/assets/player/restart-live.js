(function() {
  // Twitch's player may resume a live stream elsewhere than where it paused; the play waits a moment, so the pause
  // is not undone before it runs. When this does not bring the stream back, the app reloads the page.
  customTvDocs().forEach(function(doc) {
    var p = customTvPlayer(doc);
    if (!p || !p.pause || !p.play) return;
    try { p.pause(); } catch (e) { return; }
    setTimeout(function() {
      try { p.play(); } catch (e) {}
    }, 500);
  });
})();
