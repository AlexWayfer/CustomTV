(function() {
  function visible(el) {
    if (!el || !el.getBoundingClientRect) return false;
    var r = el.getBoundingClientRect();
    var s = el.ownerDocument && el.ownerDocument.defaultView &&
      el.ownerDocument.defaultView.getComputedStyle(el);
    return r.width > 0 && r.height > 0 && (!s || (s.display !== 'none' && s.visibility !== 'hidden'));
  }
  function isReloadButton(el) {
    var target = (el.getAttribute('data-a-target') || '').toLowerCase();
    if (target.indexOf('reload') !== -1 || target.indexOf('retry') !== -1) return true;
    // The error gate's button has no stable attribute and a localized label;
    // the gate's text carries the error code ("#2000") in every language.
    var gate = el.closest && el.closest('[data-a-target="player-overlay-content-gate"]');
    return !!gate && /#\d+/.test(gate.textContent || '');
  }
  var docs = customTvDocs();
  for (var d = 0; d < docs.length; d++) {
    var buttons = docs[d].querySelectorAll('button, [role="button"]');
    for (var b = 0; b < buttons.length; b++) {
      if (visible(buttons[b]) && isReloadButton(buttons[b])) {
        try { buttons[b].click(); } catch (e) {}
        return;
      }
    }
  }
})();
