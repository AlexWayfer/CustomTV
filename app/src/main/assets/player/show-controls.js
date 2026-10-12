(function() {
  function go(doc) {
    if (!doc) return;
    var area = doc.querySelector('[data-a-target="video-ref"]');
    var el = doc.querySelector('[data-a-target="player-overlay-click-handler"]');
    if (!area || !el) return;
    var r = el.getBoundingClientRect();
    var at = { clientX: r.left + r.width / 2, clientY: r.top + r.height / 2 };
    try {
      // After the pointer left, a move alone does not bring the controls back.
      area.dispatchEvent(new MouseEvent('mouseenter', Object.assign({ relatedTarget: doc.body }, at)));
      area.dispatchEvent(
        new MouseEvent('mouseover', Object.assign({ bubbles: true, relatedTarget: doc.body }, at))
      );
      el.dispatchEvent(new MouseEvent('mousemove', Object.assign({ bubbles: true }, at)));
    } catch (e) {}
  }
  customTvDocs().forEach(go);
})();
