(function() {
  function go(doc) {
    if (!doc) return;
    var w = doc.defaultView;
    if (w) w.__customTvUserPaused = true;
    var v = doc.querySelector('video');
    if (v && v.paused) return;
    var btn = doc.querySelector('[data-a-target="player-play-pause-button"]');
    if (btn) {
      try { btn.click(); } catch (e) {}
    }
    if (v && !v.paused) {
      try { v.pause(); } catch (e) {}
    }
  }
  customTvDocs().forEach(go);
})();
