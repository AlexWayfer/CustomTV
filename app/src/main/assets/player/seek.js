(function(time) {
  function go(doc) {
    if (!doc || !doc.querySelector) return false;
    var p = customTvPlayer(doc);
    if (p && p.seekTo) {
      try { p.seekTo(time); return true; } catch (e) {}
    }
    var v = doc.querySelector('video');
    if (!v) return false;
    try { v.currentTime = time; return true; } catch (e) {}
    return false;
  }
  customTvDocs().some(go);
})
