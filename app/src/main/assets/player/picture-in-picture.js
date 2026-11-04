(function(hide) {
  // Keep the transition after expanding too, so a fade can reverse without snapping. Twitch's transition wrapper
  // supplies the same duration and easing as the ordinary controls, including reduced-motion settings.
  var transition = '[data-a-target="player-controls"] { transition-property: opacity !important; ' +
    'transition-duration: inherit !important; transition-timing-function: inherit !important; }';
  var css = '[data-a-target="player-controls"] { opacity: 0 !important; pointer-events: none !important; }';
  function go(doc) {
    if (!doc || !doc.documentElement) return;
    var style = doc.getElementById('customtv-pip');
    if (!style && !hide) return;
    if (!style) {
      style = doc.createElement('style');
      style.id = 'customtv-pip';
      (doc.head || doc.documentElement).appendChild(style);
    }
    style.textContent = transition + (hide ? css : '');
  }
  customTvDocs().forEach(go);
})
