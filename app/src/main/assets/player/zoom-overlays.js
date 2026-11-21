(function(zoomed) {
  // The page zooms with the picture: the audience plaque, which Twitch brings back with every new stream of the
  // quality, would cover the zoomed video.
  var css = customTvDisclosureSelector + ' { visibility: hidden !important; }';
  customTvDocs().forEach(function(doc) {
    var style = doc.getElementById('customtv-zoom');
    if (!zoomed) {
      if (style) style.remove();
      return;
    }
    if (style) return;
    style = doc.createElement('style');
    style.id = 'customtv-zoom';
    style.textContent = css;
    (doc.head || doc.documentElement).appendChild(style);
  });
})
