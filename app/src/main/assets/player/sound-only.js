(function() {
  var p = customTvPlayer(document);
  if (!p || !p.getQualities) return;
  var audio = p.getQualities().find(function(q) { return q.group === 'audio_only'; });
  if (!audio) return;
  var current = p.getQuality();
  if (current && current.group === 'audio_only') return;
  window.__customTvVideoQuality = {
    auto: p.isAutoQualityMode ? p.isAutoQualityMode() : true,
    group: current ? current.group : null
  };
  p.setQuality(audio);
})();
