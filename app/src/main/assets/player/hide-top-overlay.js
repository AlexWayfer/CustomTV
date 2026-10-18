(function() {
  function playerDoc() {
    var docs = customTvDocs();
    for (var i = 0; i < docs.length; i++) {
      if (docs[i].querySelector && docs[i].querySelector('video')) return docs[i];
    }
    return docs[0];
  }
  function disclosureText(el) {
    var label = el && (el.getAttribute('aria-label') || '');
    return (label + ' ' + ((el && el.textContent) || '')).toLowerCase();
  }
  function isDisclosure(el) {
    if (!el || !el.closest) return false;
    if (el.closest('.disclosure-tool')) return true;
    if (el.closest('[class*="disclosure-tool"]')) return true;
    var t = disclosureText(el);
    return t.indexOf('certain audiences') !== -1
      || t.indexOf('content classification') !== -1
      || t.indexOf('paid promotion') !== -1
      || t.indexOf('определённ') !== -1
      || t.indexOf('определенн') !== -1;
  }
  function isProtected(el) {
    if (!el || !el.closest) return true;
    if (isDisclosure(el)) return true;
    if (el.closest('.player-controls')) return true;
    if (el.closest('[data-a-target="player-controls"]')) return true;
    if (el.closest('[data-a-target="player-twitch-logo-button"]')) return true;
    if (el.tagName === 'VIDEO' || el.closest('video')) return true;
    if (el.querySelector && el.querySelector('video')) return true;
    var label = disclosureText(el);
    if (label.indexOf('skip') !== -1) return true;
    return false;
  }
  function isUnwantedCta(el) {
    if (isDisclosure(el)) return false;
    var label = disclosureText(el);
    if (label.indexOf('subscribe') !== -1) return true;
    if (label.indexOf('follow') !== -1) return true;
    if (label.indexOf('gift') !== -1) return true;
    if (label.indexOf('подари') !== -1) return true;
    if (label.indexOf('гифт') !== -1) return true;
    if (label.indexOf('подписк') !== -1) return true;
    if (label.indexOf('learn more') !== -1) return true;
    if (label.indexOf('подробнее') !== -1) return true;
    if (label.indexOf('узнать больше') !== -1) return true;
    var href = (el.getAttribute('href') || '').toLowerCase();
    if (el.tagName === 'A' && href.indexOf('twitch.tv') !== -1 && href.indexOf('player.twitch.tv') === -1) {
      return true;
    }
    return false;
  }
  function pinDisclosure(node) {
    if (!node || !node.style) return;
    node.style.setProperty('position', 'absolute', 'important');
    node.style.setProperty('top', '8px', 'important');
    // Clears the app's sleep timer button, 44px wide, in the top right corner.
    node.style.setProperty('right', '52px', 'important');
    node.style.setProperty('left', 'auto', 'important');
    node.style.setProperty('bottom', 'auto', 'important');
    node.style.removeProperty('inset');
    node.style.setProperty('transform', 'translate(0, 0)', 'important');
    node.style.setProperty('margin', '0', 'important');
    node.style.setProperty('align-self', 'flex-start', 'important');
    node.style.setProperty('justify-self', 'end', 'important');
    node.style.setProperty('width', 'auto', 'important');
    // Also clears the minimize button, 44px wide, in the top left corner.
    node.style.setProperty('max-width', 'calc(100% - 104px)', 'important');
    node.style.setProperty('box-sizing', 'border-box', 'important');
    node.style.setProperty('overflow-wrap', 'anywhere', 'important');
    node.style.setProperty('word-break', 'break-word', 'important');
    applyDisclosureTextWrap(node);
  }
  function applyDisclosureTextWrap(root) {
    root.style.setProperty('white-space', 'normal', 'important');
    var nodes = root.querySelectorAll('a, button, h1, h2, h3, h4, p, span, div, li, [role="button"]');
    for (var i = 0; i < nodes.length; i++) {
      var el = nodes[i];
      // The card's own width cap is in the stylesheet; an inline 100% here would override it.
      if (!el.style || el.classList.contains('tw-transition')) continue;
      el.style.setProperty('white-space', 'normal', 'important');
      el.style.setProperty('max-width', '100%', 'important');
      el.style.setProperty('overflow-wrap', 'anywhere', 'important');
      el.style.setProperty('word-break', 'break-word', 'important');
    }
  }
  function disclosureRoot(el) {
    if (!el || !el.closest) return el;
    return el.closest('.disclosure-tool')
      || el.closest('[class*="disclosure-tool"]')
      || el.closest('.dt-attach-top-right')
      || el;
  }
  // The full-player "Start Watching" gate also says "certain audiences"; it stays where Twitch puts it.
  var AUDIENCE_GATE = '[data-a-target="content-classification-gate-overlay"]';
  function touchesAudienceGate(el) {
    return !!(el.closest(AUDIENCE_GATE) || el.querySelector(AUDIENCE_GATE));
  }
  function pinAudiencePlaques(doc, vw, vh) {
    var pinned = [];
    function add(el) {
      var root = disclosureRoot(el);
      if (!root || pinned.indexOf(root) !== -1) return;
      pinned.push(root);
      pinDisclosure(root);
    }
    var named = doc.querySelectorAll('.disclosure-tool, .dt-attach-top-right');
    for (var n = 0; n < named.length; n++) add(named[n]);
    if (pinned.length) return;
    var blocks = doc.querySelectorAll('button, div, aside, section');
    for (var i = 0; i < blocks.length; i++) {
      var el = blocks[i];
      var t = disclosureText(el);
      if (t.indexOf('certain audiences') === -1
        && t.indexOf('paid promotion') === -1
        && t.indexOf('определённ') === -1
        && t.indexOf('определенн') === -1) {
        continue;
      }
      if (touchesAudienceGate(el)) continue;
      var node = el;
      for (var d = 0; d < 8 && node && node.parentElement; d++) {
        var parent = node.parentElement;
        if (parent === doc.body || parent === doc.documentElement) break;
        if (touchesAudienceGate(parent)) break;
        var pr = parent.getBoundingClientRect();
        if (pr.width > vw * 0.85 || pr.height > vh * 0.45) break;
        node = parent;
        if (String(node.className || '').indexOf('disclosure-tool') !== -1) break;
      }
      add(node);
      if (pinned.length) return;
    }
  }
  function hideNode(node) {
    if (!node || !node.style) return;
    node.style.setProperty('display', 'none', 'important');
    node.style.setProperty('pointer-events', 'none', 'important');
  }
  function hideOverlayFrom(el, vw, vh) {
    var node = el;
    var body = el.ownerDocument && el.ownerDocument.body;
    for (var d = 0; d < 10 && node && node !== body; d++) {
      if (!node || isProtected(node) || isDisclosure(node)) break;
      var nr = node.getBoundingClientRect();
      var topPlaque = nr.top >= 0 && nr.top < vh * 0.42 &&
        nr.height > 32 && nr.height < vh * 0.45 &&
        nr.width > 72 && nr.width < vw * 0.85;
      if (topPlaque) {
        hideNode(node);
        return;
      }
      node = node.parentElement;
    }
  }
  function hideTopChannelPlaques(doc, vw, vh) {
    var named = doc.querySelectorAll(
      '.top-bar, .stream-info-card, .stream-info-card__image, .stream-info-social-panel'
    );
    for (var n = 0; n < named.length; n++) {
      if (!isProtected(named[n]) && !isDisclosure(named[n])) hideNode(named[n]);
    }
    var overlay = doc.querySelector('.video-player__overlay');
    var first = overlay && overlay.firstElementChild;
    var controls = first &&
      first.querySelector('.player-controls, video, [data-a-target="player-controls"]');
    if (first && !isProtected(first) && !controls) {
      var skeleton = first.querySelector(':scope > .tw-transition');
      if (skeleton && !isProtected(skeleton)) hideNode(skeleton);
    }
    var seeds = doc.querySelectorAll('button, a, [role="button"], img');
    for (var i = 0; i < seeds.length; i++) {
      var el = seeds[i];
      if (isProtected(el) || isDisclosure(el)) continue;
      if (el.tagName === 'IMG') {
        var r = el.getBoundingClientRect();
        if (r.top > vh * 0.35 || r.height > 96) continue;
      } else if (!isUnwantedCta(el)) {
        continue;
      }
      hideOverlayFrom(el, vw, vh);
    }
  }
  // A recording's controls lie over the lower half of the video, and a tap on their empty room or their time labels
  // would reach neither the video nor a control: it neither shows nor hides them. Only the controls themselves take a
  // touch; the rest goes through to the video, where the app shows or hides them. Hidden controls take none.
  var RECORDING_CONTROLS_TOUCH_CSS = [
    '.player-controls { pointer-events: none !important; }',
    '.player-controls button, .player-controls a, .player-controls input, .player-controls [role="button"],',
    '.player-controls [role="slider"], .player-controls [data-a-target="player-seekbar"],',
    '.player-controls [data-a-target="player-settings-menu"], .player-controls [data-a-target="player-settings-menu"] *,',
    '.player-controls [role="menu"], .player-controls [role="menu"] *,',
    '.player-controls [role="dialog"], .player-controls [role="dialog"] * {',
    '  pointer-events: auto !important;',
    '}',
    '.video-player__inactive .player-controls, .video-player__inactive .player-controls * {',
    '  pointer-events: none !important;',
    '}',
    // Twitch keeps the chapter list above the time labels and the seek bar, which leaves a phone's player room for
    // two chapters: it reaches down over them to just above the buttons, where its own button closes it, and up to
    // near the top.
    '.chapter-select-button__chapters [role="dialog"] { margin-bottom: 4px !important; }',
    '.chapter-select-button__chapters [role="dialog"] > * { max-height: calc(100vh - 52px) !important; }',
  ].join('\n');
  function injectInto(doc) {
    if (!doc) return;
    var style = doc.getElementById('customtv-hide-top');
    var css = [
        'html, body, #root, .root { background: transparent !important; overflow: hidden !important; }',
        '#root .top-bar, #root .pl-controls-top, #root .player-streaminfo,',
        '#root .theatre-social-panel, #root .follow-panel-overlay,',
        '#root .stream-info-card, #root .stream-info-card__image, #root .stream-info-social-panel {',
        '  display: none !important; visibility: hidden !important;',
        '}',
        '#root .video-player__overlay > div:first-child > .tw-transition {',
        '  display: none !important; visibility: hidden !important;',
        '}',
        '[data-a-target="player-overlay-follow-button"] { display: none !important; }',
        '[data-a-target="player-overlay-subscribe-button"] { display: none !important; }',
        // The player plays at full volume (FULL_VOLUME_JS); the system volume sets the loudness.
        '.volume-slider__slider-container { display: none !important; }',
        recordingPage ? RECORDING_CONTROLS_TOUCH_CSS : '',
        // A clip turns its sound on as it starts (setUpClip), but Twitch's hint to tap for sound stays a few seconds.
        clipPage ? '.click-to-unmute__container { display: none !important; }' : '',
        '[data-a-target*="overlay-gift"] { display: none !important; }',
        '[data-a-target="follow-button"] { display: none !important; }',
        '[data-a-target="subscribe-button"] { display: none !important; }',
        '[data-a-target*="learn-more"] { display: none !important; pointer-events: none !important; }',
        '[data-a-target*="learn_more"] { display: none !important; pointer-events: none !important; }',
        '[class*="learn-more"] { display: none !important; pointer-events: none !important; }',
        '[class*="LearnMore"] { display: none !important; pointer-events: none !important; }',
        '[aria-label*="Learn more" i] { display: none !important; pointer-events: none !important; }',
        '[aria-label*="Learn More"] { display: none !important; pointer-events: none !important; }',
        '.disclosure-tool, .dt-attach-top-right {',
        '  position: absolute !important;',
        '  top: 8px !important;',
        '  right: 52px !important;',
        '  left: auto !important;',
        '  bottom: auto !important;',
        '  transform: translate(0, 0) !important;',
        '  margin: 0 !important;',
        '  align-self: flex-start !important;',
        '  width: auto !important;',
        '  max-width: calc(100% - 104px) !important;',
        '  box-sizing: border-box !important;',
        '  overflow-wrap: anywhere !important;',
        '}',
        // As the plaque hides, Twitch lays the fading card out absolutely in a root that has shrunk to
        // nothing at its right edge. Capped at that root's width and placed from its left, the card would
        // collapse and wrap its text word by word; it keeps its width and ends at that edge instead.
        '.disclosure-tool .tw-transition, .dt-attach-top-right .tw-transition {',
        '  left: auto !important;',
        '  right: 0 !important;',
        '  width: max-content !important;',
        '  max-width: calc(100vw - 104px) !important;',
        '}',
        // Twitch's buffering hint keeps its desktop width of 20rem and grows up past the top of a phone's
        // player, taking its close button with it. It spans the width between the app's corner buttons
        // instead, so it takes fewer lines and stays clear of them.
        '*:has(> .low-latency-notification) {',
        '  right: 52px !important;',
        '  margin-right: 0 !important;',
        '}',
        '.low-latency-notification {',
        '  max-width: calc(100vw - 104px) !important;',
        '}',
        '.disclosure-tool *:not(svg):not(path):not(img),',
        '.dt-attach-top-right *:not(svg):not(path):not(img) {',
        '  max-width: 100% !important;',
        '  white-space: normal !important;',
        '  overflow-wrap: anywhere !important;',
        '  word-break: break-word !important;',
        '}'
    ].join('\n');
    if (!style) {
      style = doc.createElement('style');
      style.id = 'customtv-hide-top';
    }
    if (style.textContent !== css) style.textContent = css;
    var parent = doc.head || doc.documentElement;
    if (style.parentNode !== parent || style !== parent.lastElementChild) {
      parent.appendChild(style);
    }
    var vw = (doc.defaultView && doc.defaultView.innerWidth) || 0;
    var vh = (doc.defaultView && doc.defaultView.innerHeight) || 0;
    if (vh <= 0) return;
    pinAudiencePlaques(doc, vw, vh);
    hideTopChannelPlaques(doc, vw, vh);
  }
  function observeDoc(doc) {
    if (!doc || !doc.documentElement || doc.documentElement.__customTvTopObs) return;
    doc.documentElement.__customTvTopObs = true;
    // Class changes show overlays too; style changes are left out because inject() writes them itself.
    var obs = new MutationObserver(schedule);
    obs.observe(doc.documentElement, {
      childList: true, subtree: true, attributes: true, attributeFilter: ['class'],
    });
  }
  function watchIframes(doc) {
    if (!doc || !doc.querySelectorAll) return;
    var iframes = doc.querySelectorAll('iframe');
    for (var i = 0; i < iframes.length; i++) {
      var iframe = iframes[i];
      if (iframe.__customTvLoad) continue;
      iframe.__customTvLoad = true;
      iframe.addEventListener('load', function() { schedule(); });
    }
  }
  var injecting = false;
  function inject() {
    if (injecting) return;
    injecting = true;
    try {
      var docs = customTvDocs();
      for (var i = 0; i < docs.length; i++) {
        injectInto(docs[i]);
        observeDoc(docs[i]);
        watchIframes(docs[i]);
      }
      armPlayerReload();
    } finally {
      injecting = false;
    }
  }
  var scheduled = false;
  function schedule() {
    if (scheduled) return;
    scheduled = true;
    requestAnimationFrame(function() {
      scheduled = false;
      inject();
      armAllPlayback();
    });
  }
  function isAdBreak(doc) {
    if (!doc || !doc.querySelector) return false;
    if (doc.querySelector('[data-a-target*="ad-notice"], [data-a-target*="video-ad"], ' +
        '[data-test-selector*="ad-"], [class*="ad-overlay"], [class*="video-ads"]')) {
      return true;
    }
    var labeled = doc.querySelectorAll('[aria-label]');
    for (var i = 0; i < labeled.length; i++) {
      var a = (labeled[i].getAttribute('aria-label') || '').toLowerCase();
      if (a.indexOf('advertisement') !== -1 || a.indexOf('ad break') !== -1 ||
          a.indexOf('реклам') !== -1) {
        return true;
      }
    }
    return false;
  }
  // The app reads the offline screen only until playback starts, so the text scan stops then.
  var offlineCheckDone = false;
  function notifyPlayback(playing) {
    if (playing) offlineCheckDone = true;
    try { CustomTvPlayback.onState(playing); } catch (e) {}
  }
  // Only a recording or a clip reads the position; a live stream would pay a bridge call per timeupdate.
  var recordingPage = /[?&](video|clip)=/.test(location.search);
  var clipPage = /[?&]clip=/.test(location.search);
  // A clip plays with sound, though its embed starts muted whatever the URL asks, and over and over: at its end
  // Twitch covers it with recommendations, and nothing plays on from there. Each once per video, so a mute the user
  // chooses stays. Twitch plays a clip muted under its audience gate and keeps it muted once the viewer accepts, so
  // the sound waits for the gate to go. The gate can show a moment after playback starts and mute a clip that was
  // already turned up; that one is turned up again once the gate goes.
  function setUpClip(doc, v) {
    if (!clipPage) return;
    var p = customTvPlayer(doc);
    if (!p || !p.setLooping || !p.setMuted) return;
    if (!v.__customTvClipLooped) {
      v.__customTvClipLooped = true;
      p.setLooping(true);
    }
    if (doc.querySelector(AUDIENCE_GATE)) {
      v.__customTvClipUnmuted = false;
      return;
    }
    if (v.__customTvClipUnmuted) return;
    v.__customTvClipUnmuted = true;
    if (p.isMuted && p.isMuted()) p.setMuted(false);
  }
  function notifyPosition(v) {
    if (!recordingPage || !v || !isFinite(v.currentTime) || v.currentTime < 0) return;
    try { CustomTvPlayback.onPosition(v.currentTime); } catch (e) {}
  }
  function notifySeek(v) {
    if (!v || !isFinite(v.currentTime) || v.currentTime < 0) return;
    try { CustomTvPlayback.onSeek(v.currentTime); } catch (e) {}
  }
  function notifyPlayerReady() {
    try { CustomTvPlayback.onPlayerReady(); } catch (e) {}
  }
  function hasRenderedFrame(v) {
    // Sound only has no frames at all; playing sound counts then.
    var picture = (v && v.videoWidth > 0 && v.videoHeight > 0) || !!window.__customTvVideoQuality;
    return v && !v.paused && v.readyState >= 3 && picture && v.currentTime > 0;
  }
  function notifyPlaybackIfRendered(v) {
    if (hasRenderedFrame(v)) notifyPlayback(true);
  }
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
  function playerErrorButton() {
    var docs = customTvDocs();
    for (var d = 0; d < docs.length; d++) {
      var buttons = docs[d].querySelectorAll('button, [role="button"]');
      for (var b = 0; b < buttons.length; b++) {
        if (isReloadButton(buttons[b]) && visible(buttons[b])) return buttons[b];
      }
    }
    return null;
  }
  function armPlayerReload() {
    var button = playerErrorButton();
    if (!button || button.__customTvReloadBound) return;
    button.__customTvReloadBound = true;
    button.addEventListener('click', function() {
      try { CustomTvPlayback.onPlayerReload(); } catch (e) {}
    });
  }
  // Twitch's audience gate waits for a tap, and a stream, a recording, or a clip may not start under it, while the
  // app shows the page only once the video plays: the app shows it for the gate.
  var reportedAudienceGate = false;
  function notifyAudienceGate() {
    if (reportedAudienceGate) return;
    var docs = customTvDocs();
    for (var d = 0; d < docs.length; d++) {
      var gate = docs[d].querySelector && docs[d].querySelector(AUDIENCE_GATE);
      if (gate && visible(gate)) {
        reportedAudienceGate = true;
        try { CustomTvPlayback.onAudienceGate(); } catch (e) {}
        return;
      }
    }
  }
  function notifyPlayerError() {
    var button = playerErrorButton();
    if (!button || button.__customTvErrorReported) return;
    button.__customTvErrorReported = true;
    try { CustomTvPlayback.onPlayerError(); } catch (e) {}
  }
  function isOffline() {
    var docs = customTvDocs();
    for (var d = 0; d < docs.length; d++) {
      var doc = docs[d];
      if (!doc || !doc.querySelector) continue;
      if (doc.querySelector(
        '[data-a-target*="offline"], [class*="player-overlay-offline"], [class*="offline-overlay"]'
      )) {
        return true;
      }
      var nodes = doc.querySelectorAll('h1, h2, h3, p, span, [data-a-target], [aria-label]');
      for (var i = 0; i < nodes.length; i++) {
        var t = ((nodes[i].getAttribute('aria-label') || '') + ' ' + (nodes[i].textContent || ''))
          .toLowerCase().replace(/\s+/g, ' ').trim();
        if (!t || t.length > 64) continue;
        if (t === 'offline' || t.indexOf('is offline') !== -1 || t.indexOf('channel is offline') !== -1
          || t.indexOf('не в сети') !== -1) {
          return true;
        }
      }
    }
    return false;
  }
  function notifyOffline() {
    if (offlineCheckDone || !isOffline()) return;
    offlineCheckDone = true;
    try { CustomTvPlayback.onOffline(); } catch (e) {}
  }
  function readControlsVisible() {
    var docs = customTvDocs();
    for (var i = 0; i < docs.length; i++) {
      var root = docs[i].querySelector && docs[i].querySelector('.video-player__default-player');
      if (root) return root.classList.contains('video-player__inactive') === false;
    }
    return true;
  }
  // The share of the page under the top of the bottom-right control buttons, or on a recording
  // under its time labels, which sit over the seek bar above those buttons. A stream keeps the
  // buttons' top even when Twitch gives it a seek bar. Offsets ignore the transforms that slide
  // the bar while it shows or hides.
  function readControlBarFraction() {
    var docs = customTvDocs();
    for (var i = 0; i < docs.length; i++) {
      var d = docs[i];
      if (!d.querySelector) continue;
      var labels = recordingPage && d.querySelector('.vod-seekbar-time-labels');
      var el = (labels && labels.offsetHeight ? labels : null)
        || d.querySelector('.player-controls__right-control-group')
        || d.querySelector('[data-a-target="player-controls"]');
      if (!el || !el.offsetHeight) continue;
      var top = 0;
      for (var node = el; node; node = node.offsetParent) top += node.offsetTop;
      var height = d.documentElement.clientHeight;
      if (!height) continue;
      return Math.max(0, Math.min(1, (height - top) / height));
    }
    return -1;
  }
  var reportedControlBar = null;
  function notifyControlBar() {
    var fraction = Math.round(readControlBarFraction() * 1000) / 1000;
    if (fraction < 0 || fraction === reportedControlBar) return;
    reportedControlBar = fraction;
    try { CustomTvControls.onControlBar(fraction); } catch (e) {}
  }
  function pageOffset(el) {
    var left = 0, top = 0;
    for (var node = el; node; node = node.offsetParent) {
      left += node.offsetLeft;
      top += node.offsetTop;
    }
    return { left: left, top: top };
  }
  // Where a recording's seek bar sits, as shares of the page: the bar's left and right edges, its top, and the top of
  // the area that takes its touches. Offsets ignore the transforms that slide the bar while it shows or hides.
  function readSeekbar() {
    if (!recordingPage || clipPage) return null;
    var docs = customTvDocs();
    for (var i = 0; i < docs.length; i++) {
      var d = docs[i];
      if (!d.querySelector) continue;
      var area = d.querySelector('[data-a-target="player-seekbar"]');
      var bar = area && area.querySelector('.seekbar-bar');
      if (!bar || !bar.offsetWidth) continue;
      var width = d.documentElement.clientWidth;
      var height = d.documentElement.clientHeight;
      if (!width || !height) continue;
      var barAt = pageOffset(bar);
      return [
        barAt.left / width,
        (barAt.left + bar.offsetWidth) / width,
        barAt.top / height,
        pageOffset(area).top / height,
      ].map(function(share) { return Math.round(share * 1000) / 1000; });
    }
    return null;
  }
  var reportedSeekbar = null;
  function notifySeekbar() {
    var seekbar = readSeekbar();
    if (!seekbar || seekbar.join() === reportedSeekbar) return;
    reportedSeekbar = seekbar.join();
    try { CustomTvControls.onSeekbar(seekbar[0], seekbar[1], seekbar[2], seekbar[3]); } catch (e) {}
  }
  // Twitch's settings menu rises over the bottom corner, where the app's chat buttons sit, and a recording's chapter
  // list over the seek bar, where the app draws the chat's heat.
  function readSettingsMenuOpen() {
    var docs = customTvDocs();
    for (var i = 0; i < docs.length; i++) {
      var d = docs[i];
      if (!d.querySelector) continue;
      if (d.querySelector('[data-a-target="player-settings-menu"]')) return true;
      var chapters = d.querySelector(CHAPTER_LIST);
      if (chapters && chapters.offsetHeight) return true;
    }
    return false;
  }
  var reportedSettingsMenu = null;
  function notifySettingsMenu() {
    var open = readSettingsMenuOpen();
    if (open === reportedSettingsMenu) return;
    reportedSettingsMenu = open;
    try { CustomTvControls.onSettingsMenu(open); } catch (e) {}
  }
  var reportedControls = null;
  function notifyControls() {
    notifyControlBar();
    notifySeekbar();
    notifySettingsMenu();
    var controls = readControlsVisible();
    if (controls === reportedControls) return;
    reportedControls = controls;
    try { CustomTvControls.onControlsVisible(controls); } catch (e) {}
  }
  function armChromeSync(doc) {
    var root = doc && doc.querySelector && doc.querySelector('.video-player__default-player');
    if (!root || root.__customTvChromeObs) return;
    root.__customTvChromeObs = true;
    var obs = new MutationObserver(notifyControls);
    obs.observe(root, { attributes: true, attributeFilter: ['class'] });
    // The settings menu comes and goes as a modal dialog that Twitch puts at the end of the body, not under the player.
    // The chapter list stays in place and only opens or closes: a tap on the controls checks again once it has.
    if (doc.body && !doc.body.__customTvSettingsObs) {
      doc.body.__customTvSettingsObs = true;
      new MutationObserver(notifySettingsMenu).observe(doc.body, { childList: true, subtree: true });
      doc.addEventListener('click', function(event) {
        if (!(event.target && event.target.closest && event.target.closest('.player-controls'))) return;
        setTimeout(notifySettingsMenu, 50);
        setTimeout(notifySettingsMenu, 300);
      }, true);
    }
    notifyControls();
  }
  // The layer over the video that plays or pauses a recording on a click; buttons and notes sit apart.
  var VIDEO_CLICK_LAYER = '[data-a-target="player-overlay-click-handler"]';
  function armScrollableTouch(doc) {
    if (!doc || !doc.documentElement || doc.documentElement.__customTvTouchBound) return;
    doc.documentElement.__customTvTouchBound = true;
    // A touch that began on a note, such as the audience plaque as it closes, can lift over the video once the note
    // has gone; its click would play or pause a recording, so the page drops it.
    var touchStartedOnVideo = true;
    ['mousedown', 'mouseup', 'click'].forEach(function(type) {
      doc.addEventListener(type, function(event) {
        if (touchStartedOnVideo) return;
        if (!(event.target && event.target.closest && event.target.closest(VIDEO_CLICK_LAYER))) return;
        event.stopPropagation();
        event.preventDefault();
      }, true);
    });
    doc.addEventListener('touchstart', function(event) {
      var node = event.target;
      var scrollable = false;
      while (node && node !== doc.documentElement) {
        if (node.nodeType === 1) {
          if (node.matches('[data-a-target="player-settings-menu"], [class*="settings-menu"]')) {
            scrollable = true;
            break;
          }
          var style = doc.defaultView.getComputedStyle(node);
          if (node.scrollHeight > node.clientHeight + 1 &&
              (style.overflowY === 'auto' || style.overflowY === 'scroll')) {
            scrollable = true;
            break;
          }
        }
        node = node.parentElement;
      }
      var controls = !!(event.target && event.target.closest &&
        event.target.closest('[data-a-target="player-controls"]'));
      var video = !!(event.target && event.target.closest && event.target.closest(VIDEO_CLICK_LAYER));
      touchStartedOnVideo = video;
      var disclosure = !!(event.target && event.target.closest &&
        event.target.closest('.disclosure-tool, [class*="disclosure-tool"]'));
      try { CustomTvControls.onTouchStart(scrollable, controls, video, disclosure); } catch (e) {}
    }, { capture: true, passive: true });
    armSeekbarHold(doc);
  }
  // Twitch hides its controls five seconds after they showed, and a pointer move does not put that off while they
  // show. Having the pointer leave the player and enter it again does: the enter starts Twitch's five seconds anew,
  // and React applies the leave and the enter together, so the controls never hide in between.
  function reenterPlayer(doc) {
    var area = doc.querySelector('[data-a-target="video-ref"]');
    if (!area) return false;
    try {
      area.dispatchEvent(new MouseEvent('mouseleave', { relatedTarget: doc.body }));
      area.dispatchEvent(new MouseEvent('mouseenter', { relatedTarget: doc.body }));
    } catch (e) {}
    return true;
  }
  // The chapter list closes with the controls, even while a finger scrolls it: while it is open, the controls stay.
  var CHAPTER_LIST = '.chapter-select-button__chapters [role="dialog"]';
  function keepChapterListUp() {
    var docs = customTvDocs();
    for (var i = 0; i < docs.length; i++) {
      var list = docs[i].querySelector && docs[i].querySelector(CHAPTER_LIST);
      if (list && list.offsetHeight) reenterPlayer(docs[i]);
    }
  }
  // A finger that holds the seek bar moves no pointer at all: while it holds, the controls stay.
  // Twitch's preview of the time and the frame follows only a pointer over the bar, while a finger's drag moves only
  // the thumb: the pointer comes over the bar with the finger, follows it, and leaves the bar as it lifts.
  var SEEKBAR = '[data-a-target="player-seekbar"]';
  function armSeekbarHold(doc) {
    var bar = null;
    var timer = null;
    function point(type, touch) {
      var r = bar.getBoundingClientRect();
      try {
        bar.dispatchEvent(new MouseEvent(type, {
          bubbles: true, relatedTarget: type === 'mousemove' ? null : doc.body,
          clientX: touch.clientX, clientY: r.top + r.height / 2,
        }));
      } catch (e) {}
    }
    function stop(touch) {
      if (timer) clearInterval(timer);
      timer = null;
      if (bar && bar.isConnected && touch) point('mouseout', touch);
      bar = null;
    }
    function reenter() {
      if (!bar || !bar.isConnected || !reenterPlayer(doc)) stop();
    }
    doc.addEventListener('touchstart', function(event) {
      var held = event.target && event.target.closest && event.target.closest(SEEKBAR);
      var touch = event.touches && event.touches[0];
      if (!held || !touch) return;
      stop();
      bar = held;
      point('mouseover', touch);
      point('mousemove', touch);
      timer = setInterval(reenter, 1000);
    }, { capture: true, passive: true });
    doc.addEventListener('touchmove', function(event) {
      var touch = event.touches && event.touches[0];
      if (bar && bar.isConnected && touch) point('mousemove', touch);
    }, { capture: true, passive: true });
    ['touchend', 'touchcancel'].forEach(function(type) {
      doc.addEventListener(type, function(event) {
        if (!event.touches || !event.touches.length) stop(event.changedTouches && event.changedTouches[0]);
      }, { capture: true, passive: true });
    });
  }
  function armPlaybackSync(targetDoc) {
    if (!targetDoc || !targetDoc.documentElement || targetDoc.documentElement.__customTvSync) return;
    targetDoc.documentElement.__customTvSync = true;
    function bind(v) {
      if (v.__customTvBound) return;
      v.__customTvBound = true;
      notifyPlayerReady();
      v.addEventListener('playing', function() {
        setUpClip(targetDoc, v);
        notifyPlaybackIfRendered(v);
      });
      v.addEventListener('timeupdate', function() {
        setUpClip(targetDoc, v);
        notifyPlaybackIfRendered(v);
        notifyPosition(v);
      });
      v.addEventListener('seeking', function() { notifySeek(v); });
      v.addEventListener('seeked', function() {
        notifyPosition(v);
        notifySeek(v);
      });
      v.addEventListener('pause', function() {
        if (isAdBreak(targetDoc)) return;
        notifyPlayback(false);
      });
      notifyPlaybackIfRendered(v);
    }
    function scan() {
      var videos = targetDoc.querySelectorAll('video');
      for (var i = 0; i < videos.length; i++) bind(videos[i]);
    }
    scan();
    var obs = new MutationObserver(scan);
    obs.observe(targetDoc.documentElement, { childList: true, subtree: true });
  }
  function armAllPlayback() {
    var docs = customTvDocs();
    for (var i = 0; i < docs.length; i++) {
      armPlaybackSync(docs[i]);
      armChromeSync(docs[i]);
      armScrollableTouch(docs[i]);
    }
  }
  inject();
  armAllPlayback();
  notifyControls();
  notifyOffline();
  armPlayerReload();
  notifyPlayerError();
  notifyAudienceGate();
  if (window.__customTvOverlayBooted) return;
  window.__customTvOverlayBooted = true;
  // The DOM observers and iframe loads schedule inject(); layout changes only on resize.
  window.addEventListener('resize', schedule);
  window.addEventListener('resize', notifyControlBar);
  window.addEventListener('resize', notifySeekbar);
  setInterval(function() {
    notifyControls();
    keepChapterListUp();
    notifyOffline();
    armPlayerReload();
    notifyPlayerError();
    notifyAudienceGate();
    var d = playerDoc();
    if (isAdBreak(d)) {
      notifyPlaybackIfRendered(d && d.querySelector('video'));
      return;
    }
    var v = d && d.querySelector('video');
    if (v) {
      if (hasRenderedFrame(v)) notifyPlayback(true);
      else if (v.paused) notifyPlayback(false);
    }
  }, 800);
})();
