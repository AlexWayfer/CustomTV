(function() {
  // A fade in sets the full volume at its end; setting it now would cut the fade short.
  if (customTvFading(document.querySelector('video'))) return;
  customTvFullVolume(document);
})();
