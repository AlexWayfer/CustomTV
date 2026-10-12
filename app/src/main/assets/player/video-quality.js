(function() {
  var before = window.__customTvVideoQuality;
  if (!before) return;
  window.__customTvVideoQuality = null;
  var p = customTvPlayer(document);
  if (!p || !p.getQualities) return;
  var current = p.getQuality();
  if (!current || current.group !== 'audio_only') return;
  var fixed = before.group && p.getQualities().find(function(q) { return q.group === before.group; });
  if (!before.auto && fixed) p.setQuality(fixed);
  else if (p.setAutoQualityMode) p.setAutoQualityMode(true);
  else p.setQuality(p.getQualities()[0]);
})();
