(function(source) {
  function isAdBreak(doc) {
    if (!doc || !doc.querySelector) return false;
    if (doc.querySelector('[data-a-target*="ad-notice"], [data-a-target*="video-ad"], ' +
        '[data-test-selector*="ad-"], [class*="ad-overlay"], [class*="video-ads"]')) {
      return true;
    }
    var labeled = doc.querySelectorAll('[aria-label]');
    for (var i = 0; i < labeled.length; i++) {
      var a = (labeled[i].getAttribute('aria-label') || '').toLowerCase();
      if (a.indexOf('advertisement') !== -1 || a.indexOf('ad break') !== -1 ||
          a.indexOf('реклам') !== -1) {
        return true;
      }
    }
    return false;
  }
  function go(doc) {
    if (!doc) return;
    var w = doc.defaultView;
    if (w) {
      w.__customTvUserPaused = false;
      try {
        if (w.navigator && w.navigator.mediaSession) {
          w.navigator.mediaSession.playbackState = 'playing';
        }
      } catch (e) {}
    }
    var v = doc.querySelector('video');
    var diagnostic = customTvDiagnosticCommand(doc, 'play', source);
    if (v && !v.paused) {
      diagnostic(v, 'already-playing');
      // A pause that still fades out turns back; Twitch's player has not paused yet.
      if (customTvFadingOut(v)) customTvFade(v, 1, function() { customTvFullVolume(doc); });
      return;
    }
    diagnostic(v, 'request');
    if (isAdBreak(doc)) {
      diagnostic(v, 'ad-skipped');
      return;
    }
    var btn = doc.querySelector('[data-a-target="player-play-pause-button"]');
    var twitchPaused = true;
    if (btn) {
      var state = (btn.getAttribute('data-a-player-state') || '').toLowerCase();
      var label = (btn.getAttribute('aria-label') || '').toLowerCase();
      if (state === 'playing' || label.indexOf('pause') !== -1 || label.indexOf('пауза') !== -1) {
        twitchPaused = false;
      } else if (state === 'paused' || label.indexOf('play') !== -1 ||
          label.indexOf('воспроизвед') !== -1) {
        twitchPaused = true;
      }
    }
    // The sound starts silent and fades in once the video plays.
    if (v) customTvFade(v, 1, function() { customTvFullVolume(doc); });
    if (btn && twitchPaused) {
      diagnostic(v, 'button-before');
      try {
        btn.click();
        diagnostic(v, 'button-after');
      } catch (e) {
        diagnostic(v, 'button-failed');
      }
    }
    if (v && v.paused) {
      try {
        v.muted = false;
        v.defaultMuted = false;
        v.playsInline = true;
        diagnostic(v, 'native-before');
        var p = v.play();
        diagnostic(v, 'native-after');
        if (p && p.then) p.then(function() {
          diagnostic(v, 'native-resolved');
        }, function() {
          diagnostic(v, 'native-rejected');
        });
      } catch (e) {
        diagnostic(v, 'native-failed');
      }
    }
  }
  customTvDocs().forEach(go);
})
