(function(hideControls, hideDisclosure) {
  // Keep the transition after expanding too, so a fade can reverse without snapping. Twitch's transition wrapper
  // supplies the same duration and easing as the ordinary controls, including reduced-motion settings.
  var transition = '[data-a-target="player-controls"] { transition-property: opacity !important; ' +
    'transition-duration: inherit !important; transition-timing-function: inherit !important; }';
  var controls = '[data-a-target="player-controls"] { opacity: 0 !important; pointer-events: none !important; }';
  // Twitch mounts the disclosure outside the control bar and may replace it while the player is in PiP.
  var disclosure = customTvDisclosureSelector + ' { display: none !important; }';
  function go(doc) {
    if (!doc || !doc.documentElement) return;
    var style = doc.getElementById('customtv-pip');
    if (!style && !hideControls && !hideDisclosure) return;
    if (!style) {
      style = doc.createElement('style');
      style.id = 'customtv-pip';
      (doc.head || doc.documentElement).appendChild(style);
    }
    style.textContent = transition + (hideControls ? controls : '') + (hideDisclosure ? disclosure : '');
  }
  customTvDocs().forEach(go);
})
