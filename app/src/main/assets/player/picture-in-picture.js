(function(hide) {
  var css = '[data-a-target="player-controls"] { opacity: 0 !important; pointer-events: none !important; }';
  function go(doc) {
    if (!doc || !doc.documentElement) return;
    var style = doc.getElementById('customtv-pip');
    if (!hide) {
      if (style) style.remove();
      return;
    }
    if (style) return;
    style = doc.createElement('style');
    style.id = 'customtv-pip';
    style.textContent = css;
    (doc.head || doc.documentElement).appendChild(style);
  }
  customTvDocs().forEach(go);
})
