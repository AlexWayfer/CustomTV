// What the video and Twitch's player show, for the log line the app writes before it reloads a page that has not
// started. The app logs the value as it comes, so it holds no address.
(function() {
  function round(n) {
    return typeof n === 'number' && isFinite(n) ? Math.round(n * 1000) / 1000 : null;
  }
  var docs = customTvDocs();
  for (var i = 0; i < docs.length; i++) {
    var v = docs[i].querySelector('video');
    if (!v) continue;
    var ranges = [];
    try {
      for (var r = 0; r < v.buffered.length; r++) {
        ranges.push([round(v.buffered.start(r)), round(v.buffered.end(r))]);
      }
    } catch (e) {}
    var s = {
      ready: v.readyState,
      network: v.networkState,
      paused: v.paused,
      time: round(v.currentTime),
      buffered: ranges,
      audioBytes: v.webkitAudioDecodedByteCount,
      videoBytes: v.webkitVideoDecodedByteCount,
      size: v.videoWidth + 'x' + v.videoHeight,
      error: v.error ? v.error.code : null,
    };
    var p = v.__customTvPlayer || customTvPlayer(docs[i]);
    if (p) {
      try { s.state = String(p.getState()); } catch (e) {}
      try { s.buffer = round(p.getBufferDuration()); } catch (e) {}
      try { s.latency = round(p.getLiveLatency()); } catch (e) {}
      try { s.bandwidth = p.getBandwidthEstimate(); } catch (e) {}
      try { s.quality = p.getQuality().name; } catch (e) {}
    }
    return s;
  }
  return { video: false, documents: docs.length };
})();
