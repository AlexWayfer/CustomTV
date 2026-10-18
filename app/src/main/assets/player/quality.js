(function(mode) {
  // 'user' brings back the viewer's own quality; 'auto' lets the player choose; 'best' plays the highest one.
  // The viewer's own quality and Twitch's saved choice wait in localStorage, so a player closed in full screen or
  // zoomed still gets them back with the next page.
  var KEY = 'customtv-user-quality';
  var TWITCH_KEYS = ['video-quality', 'quality-bitrate', 'video-quality-highest-available'];
  var p = customTvPlayer(document);
  if (!p || !p.getQualities) return;
  var current = p.getQuality();
  var soundOnly = !!current && current.group === 'audio_only';
  var auto = p.isAutoQualityMode ? p.isAutoQualityMode() : true;
  var saved = null;
  try { saved = JSON.parse(localStorage.getItem(KEY)); } catch (e) {}
  // A quality picked in the menu after the app let the player choose is the viewer's own now.
  if (saved && saved.applied === 'auto' && !auto && !soundOnly) {
    localStorage.removeItem(KEY);
    saved = null;
  }
  if (mode === 'user') {
    if (!saved) return;
    localStorage.removeItem(KEY);
    TWITCH_KEYS.forEach(function(k) {
      var value = saved.stored[k];
      if (value == null) localStorage.removeItem(k);
      else localStorage.setItem(k, value);
    });
    var own = { auto: saved.auto, group: saved.group };
    // Sound only brings back the quality it found once the video returns: make that the viewer's own.
    if (soundOnly) {
      if (window.__customTvVideoQuality) window.__customTvVideoQuality = own;
      return;
    }
    var fixed = !own.auto && own.group && p.getQualities().find(function(q) { return q.group === own.group; });
    if (fixed) {
      if (auto || !current || current.group !== fixed.group) p.setQuality(fixed);
    } else if (!auto && p.setAutoQualityMode) {
      p.setAutoQualityMode(true);
    }
    return;
  }
  // Sound only plays in the background, where neither full screen nor the zoom is seen.
  if (soundOnly) return;
  if (!saved) {
    var stored = {};
    TWITCH_KEYS.forEach(function(k) { stored[k] = localStorage.getItem(k); });
    saved = { auto: auto, group: current ? current.group : null, stored: stored };
  }
  saved.applied = mode;
  localStorage.setItem(KEY, JSON.stringify(saved));
  if (mode === 'auto') {
    if (!auto && p.setAutoQualityMode) p.setAutoQualityMode(true);
    return;
  }
  var best = p.getQualities()
    .filter(function(q) { return q.group !== 'audio_only'; })
    .sort(function(a, b) {
      return (b.height || 0) - (a.height || 0) ||
        (b.framerate || 0) - (a.framerate || 0) ||
        (b.bitrate || 0) - (a.bitrate || 0);
    })[0];
  // The best quality already playing, fixed or chosen by Auto, stays: a switch would only restart the stream.
  if (best && (!current || current.group !== best.group)) p.setQuality(best);
})
