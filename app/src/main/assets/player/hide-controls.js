(function() {
  // Returns false when the controls show but the player area is not there.
  function go(doc) {
    if (!doc) return true;
    var root = doc.querySelector('.video-player__default-player');
    if (!root || root.classList.contains('video-player__inactive')) return true;
    var area = doc.querySelector('[data-a-target="video-ref"]');
    if (!area) return false;
    var r = area.getBoundingClientRect();
    var away = { clientX: r.right + 1, clientY: r.bottom + 1, relatedTarget: doc.body };
    try {
      area.dispatchEvent(new MouseEvent('mouseleave', away));
      area.dispatchEvent(new MouseEvent('mouseout', Object.assign({ bubbles: true }, away)));
    } catch (e) {}
    return true;
  }
  var found = customTvDocs().map(go).every(Boolean);
  if (!found && !window.__customTvHoverAreaMissing) {
    window.__customTvHoverAreaMissing = true;
    try { CustomTvControls.onHoverAreaMissing(); } catch (e) {}
  }
})();
