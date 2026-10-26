(function() {
  function pause(doc, v) {
    var btn = doc.querySelector('[data-a-target="player-play-pause-button"]');
    if (btn) {
      try { btn.click(); } catch (e) {}
    }
    if (v && !v.paused) {
      try { v.pause(); } catch (e) {}
    }
  }
  function go(doc) {
    if (!doc) return;
    var w = doc.defaultView;
    if (w) w.__customTvUserPaused = true;
    var v = doc.querySelector('video');
    if (v && v.paused) {
      // A fade in that still waits for the stream to play is not wanted any more.
      customTvFadeStop(v);
      return;
    }
    if (!v) {
      pause(doc, v);
      return;
    }
    // Twitch's player pauses once the sound has faded out. The paused video gets its full volume back, so a play
    // that does not come through the app still has sound.
    customTvFade(v, 0, function() {
      if (!v.paused) pause(doc, v);
      v.volume = 1;
    });
  }
  customTvDocs().forEach(go);
})();
