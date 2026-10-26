// Helpers the player scripts share. TwitchPlayerScripts runs this file before each script, so they are there after
// the page reloads too.

// The page and the documents of its iframes that the page may read.
var customTvDocs = function() {
  var docs = [document];
  var iframes = document.querySelectorAll('iframe');
  for (var i = 0; i < iframes.length; i++) {
    try {
      var d = iframes[i].contentDocument;
      if (d && docs.indexOf(d) < 0) docs.push(d);
    } catch (e) {}
  }
  return docs;
};

// Twitch's media player behind the video of [doc], found through the React fiber of the video or a div around it.
var customTvPlayer = function(doc) {
  var nodes = [doc.querySelector('video')].concat([].slice.call(doc.querySelectorAll('div')));
  for (var i = 0; i < nodes.length; i++) {
    var n = nodes[i];
    if (!n) continue;
    var key = Object.keys(n).find(function(k) { return k.indexOf('__reactFiber') === 0; });
    if (!key) continue;
    for (var f = n[key], depth = 0; f && depth < 80; f = f.return, depth++) {
      if (f.memoizedProps && f.memoizedProps.mediaPlayerInstance) {
        return f.memoizedProps.mediaPlayerInstance;
      }
    }
  }
  return null;
};

// Sets the player's own volume to full, so only the system volume and the mute button change the loudness.
var customTvFullVolume = function(doc) {
  var p = customTvPlayer(doc);
  if (p && p.setVolume && p.getVolume && p.getVolume() < 1) p.setVolume(1);
};

// How often a fade sets the volume.
var CUSTOM_TV_FADE_STEP_MS = 16;

// How long the sound fades in or out, as the app tells the page on every load ahead of the other scripts: 0 with
// the app's Smooth sound off, and no fade before the app has told it.
var customTvFadeMs = function() {
  var ms = window.__customTvFadeMs;
  return typeof ms === 'number' && ms > 0 ? ms : 0;
};

// The volume property of the page that [v] belongs to, which reads and writes the video's volume past a fade's hold.
var customTvVolumeProperty = function(v) {
  var w = (v.ownerDocument && v.ownerDocument.defaultView) || window;
  return Object.getOwnPropertyDescriptor(w.HTMLMediaElement.prototype, 'volume');
};

// Twitch's player sets the video's volume on its own: to silence once it has paused, and back to its volume as it
// plays again, before the video has sound. While a fade runs, the fade owns the volume: a write from anyone else
// waits in the fade's [wanted] until the fade lets go.
var customTvHoldVolume = function(v) {
  if (v.__customTvVolumeHeld) return;
  v.__customTvVolumeHeld = true;
  var native = customTvVolumeProperty(v);
  Object.defineProperty(v, 'volume', {
    configurable: true,
    get: function() { return native.get.call(v); },
    set: function(value) {
      var fade = v.__customTvFade;
      if (fade) fade.wanted = value;
      else native.set.call(v, value);
    },
  });
};

// Gives the video's volume back to everyone, set to [value] when there is one.
var customTvLetGoVolume = function(v, value) {
  if (v.__customTvVolumeHeld) {
    delete v.volume;
    v.__customTvVolumeHeld = false;
  }
  if (value !== undefined) v.volume = value;
};

// Fades the sound of the video [v] in to full ([to] is 1) or out to silence ([to] is 0), then calls [done]. The
// level moves evenly. A fade in sets the volume to the level, so the sound comes back at once; a fade out sets it to
// the level's square, which the ear hears as an even fall. A fade the other way turns around where the sound is, so
// it takes only the part of the time it has to go back; one the same way leaves the running fade alone. A fade in
// waits for the video to play, so it does not pass while the stream loads. Without a fade length, the volume stays
// as Twitch's player sets it and [done] runs at once.
var customTvFade = function(v, to, done) {
  var fade = v.__customTvFade;
  if (fade && fade.to === to) return;
  var fadeMs = customTvFadeMs();
  if (fadeMs === 0) {
    customTvFadeStop(v);
    if (done) done();
    return;
  }
  function volumeAt(level) {
    return to === 1 ? level : level * level;
  }
  var native = customTvVolumeProperty(v);
  // A fade in from silence starts at nothing; any other starts at the level its own curve gives the sound now.
  var volume = native.get.call(v);
  var level = to === 1 ? (fade ? volume : 0) : Math.sqrt(volume);
  var wanted = fade ? fade.wanted : undefined;
  if (fade) {
    clearInterval(fade.timer);
    if (fade.waiting) v.removeEventListener('playing', fade.waiting);
  }
  fade = { to: to, level: level, timer: null, waiting: null, wanted: wanted };
  v.__customTvFade = fade;
  customTvHoldVolume(v);
  native.set.call(v, volumeAt(level));
  function start() {
    fade.waiting = null;
    var from = fade.level;
    var ms = fadeMs * Math.abs(to - from);
    var began = Date.now();
    fade.timer = setInterval(function() {
      var t = ms > 0 ? Math.min(1, (Date.now() - began) / ms) : 1;
      fade.level = from + (to - from) * t;
      native.set.call(v, volumeAt(fade.level));
      if (t < 1) return;
      clearInterval(fade.timer);
      v.__customTvFade = null;
      // A fade in ends at what Twitch's player asked for meanwhile, such as silence when it paused; a fade out keeps
      // the silence, so a write that came on the way does not sound before [done] pauses the video.
      customTvLetGoVolume(v, to === 1 ? (fade.wanted === undefined ? 1 : fade.wanted) : undefined);
      if (done) done();
    }, CUSTOM_TV_FADE_STEP_MS);
  }
  if (to === 1 && v.paused) {
    fade.waiting = start;
    v.addEventListener('playing', start, { once: true });
  } else {
    start();
  }
};

// Drops the fade of [v] without its end and gives the video the volume Twitch's player asked for, or full.
var customTvFadeStop = function(v) {
  var fade = v.__customTvFade;
  if (!fade) return;
  clearInterval(fade.timer);
  if (fade.waiting) v.removeEventListener('playing', fade.waiting);
  v.__customTvFade = null;
  customTvLetGoVolume(v, fade.wanted === undefined ? 1 : fade.wanted);
};

// Whether the sound of [v] fades in or out now.
var customTvFading = function(v) {
  return !!(v && v.__customTvFade);
};

// Whether [v] fades out to the app's pause: it still plays, but the viewer has paused it.
var customTvFadingOut = function(v) {
  return !!(v && v.__customTvFade && v.__customTvFade.to === 0);
};
