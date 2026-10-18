(function(gap) {
  // Opens room of [gap] px between a recording's time labels and its seek bar, where the app draws over the page. The
  // labels and the controls above them rise by as much; the bar stays. A gap of 0 closes the room.
  customTvDocs().forEach(function(doc) {
    var style = doc.getElementById('customtv-seekbar-gap');
    if (!(gap > 0)) {
      if (style) style.remove();
      return;
    }
    if (!style) {
      style = doc.createElement('style');
      style.id = 'customtv-seekbar-gap';
      (doc.head || doc.documentElement).appendChild(style);
    }
    style.textContent = '.vod-seekbar-time-labels { margin-bottom: ' + gap + 'px !important; }';
  });
})
