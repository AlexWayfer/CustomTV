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
