(function() {
  var p = customTvPlayer(document);
  if (p && p.setVolume && p.getVolume && p.getVolume() < 1) p.setVolume(1);
})();
