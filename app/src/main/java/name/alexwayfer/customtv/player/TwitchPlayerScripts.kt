package name.alexwayfer.customtv.player

import android.net.Uri
import androidx.core.net.toUri

internal object TwitchPlayerScripts {
        const val KEEP_ALIVE_MS = 2_000L

        const val PLAY_JS = """
            (function() {
              function isAdBreak(doc) {
                if (!doc || !doc.querySelector) return false;
                if (doc.querySelector('[data-a-target*="ad-notice"], [data-a-target*="video-ad"], [data-test-selector*="ad-"], [class*="ad-overlay"], [class*="video-ads"]')) {
                  return true;
                }
                var labeled = doc.querySelectorAll('[aria-label]');
                for (var i = 0; i < labeled.length; i++) {
                  var a = (labeled[i].getAttribute('aria-label') || '').toLowerCase();
                  if (a.indexOf('advertisement') !== -1 || a.indexOf('ad break') !== -1 || a.indexOf('реклам') !== -1) {
                    return true;
                  }
                }
                return false;
              }
              function go(doc) {
                if (!doc) return;
                var w = doc.defaultView;
                if (w) {
                  w.__customTvUserPaused = false;
                  try {
                    if (w.navigator && w.navigator.mediaSession) {
                      w.navigator.mediaSession.playbackState = 'playing';
                    }
                  } catch (e) {}
                }
                if (isAdBreak(doc)) return;
                var v = doc.querySelector('video');
                if (v && !v.paused) return;
                var btn = doc.querySelector('[data-a-target="player-play-pause-button"]');
                var twitchPaused = true;
                if (btn) {
                  var state = (btn.getAttribute('data-a-player-state') || '').toLowerCase();
                  var label = (btn.getAttribute('aria-label') || '').toLowerCase();
                  if (state === 'playing' || label.indexOf('pause') !== -1 || label.indexOf('пауза') !== -1) {
                    twitchPaused = false;
                  } else if (state === 'paused' || label.indexOf('play') !== -1 || label.indexOf('воспроизвед') !== -1) {
                    twitchPaused = true;
                  }
                }
                if (btn && twitchPaused) {
                  try { btn.click(); } catch (e) {}
                }
                if (v && v.paused) {
                  try {
                    v.muted = false;
                    v.defaultMuted = false;
                    v.playsInline = true;
                    var p = v.play();
                    if (p && p.catch) p.catch(function() {});
                  } catch (e) {}
                }
              }
              go(document);
              var iframe = document.querySelector('iframe');
              try { if (iframe && iframe.contentDocument) go(iframe.contentDocument); } catch (e) {}
            })();
        """

        const val PAUSE_JS = """
            (function() {
              function go(doc) {
                if (!doc) return;
                var w = doc.defaultView;
                if (w) w.__customTvUserPaused = true;
                var v = doc.querySelector('video');
                if (v && v.paused) return;
                var btn = doc.querySelector('[data-a-target="player-play-pause-button"]');
                if (btn) {
                  try { btn.click(); } catch (e) {}
                }
                if (v && !v.paused) {
                  try { v.pause(); } catch (e) {}
                }
              }
              go(document);
              var iframe = document.querySelector('iframe');
              try { if (iframe && iframe.contentDocument) go(iframe.contentDocument); } catch (e) {}
            })();
        """

        /** Moves a recording to [seconds] through Twitch's player, or through the video when the player is not found. */
        fun seekJs(seconds: Double): String = """
            (function() {
              var time = $seconds;
              function player(doc) {
                var nodes = [doc.querySelector('video')].concat([].slice.call(doc.querySelectorAll('div')));
                for (var i = 0; i < nodes.length; i++) {
                  var n = nodes[i];
                  if (!n) continue;
                  var key = Object.keys(n).find(function(k) { return k.indexOf('__reactFiber') === 0; });
                  if (!key) continue;
                  for (var f = n[key], depth = 0; f && depth < 80; f = f.return, depth++) {
                    if (f.memoizedProps && f.memoizedProps.mediaPlayerInstance) return f.memoizedProps.mediaPlayerInstance;
                  }
                }
                return null;
              }
              function go(doc) {
                if (!doc || !doc.querySelector) return false;
                var p = player(doc);
                if (p && p.seekTo) {
                  try { p.seekTo(time); return true; } catch (e) {}
                }
                var v = doc.querySelector('video');
                if (!v) return false;
                try { v.currentTime = time; return true; } catch (e) {}
                return false;
              }
              if (go(document)) return;
              var iframe = document.querySelector('iframe');
              try { if (iframe && iframe.contentDocument) go(iframe.contentDocument); } catch (e) {}
            })();
        """

        /**
         * Shows a recording's controls with a pointer that enters the player and moves over the video: a tap there would
         * play or pause it. They hide on Twitch's own timer, or at once with [HIDE_CONTROLS_JS].
         */
        const val SHOW_CONTROLS_JS = """
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
                  area.dispatchEvent(new MouseEvent('mouseover', Object.assign({ bubbles: true, relatedTarget: doc.body }, at)));
                  el.dispatchEvent(new MouseEvent('mousemove', Object.assign({ bubbles: true }, at)));
                } catch (e) {}
              }
              go(document);
              var iframe = document.querySelector('iframe');
              try { if (iframe && iframe.contentDocument) go(iframe.contentDocument); } catch (e) {}
            })();
        """

        /**
         * Hides a recording's shown controls at once with a pointer that leaves the player; a tap on the video would
         * also play or pause it. When the page shows controls without that player area, the app hears once per page
         * load, so a Twitch change shows in the report.
         */
        const val HIDE_CONTROLS_JS = """
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
              var found = go(document);
              var iframe = document.querySelector('iframe');
              try { if (iframe && iframe.contentDocument) found = go(iframe.contentDocument) && found; } catch (e) {}
              if (!found && !window.__customTvHoverAreaMissing) {
                window.__customTvHoverAreaMissing = true;
                try { CustomTvPlayback.onHoverAreaMissing(); } catch (e) {}
              }
            })();
        """

        const val RELOAD_PLAYER_JS = """
            (function() {
              function visible(el) {
                if (!el || !el.getBoundingClientRect) return false;
                var r = el.getBoundingClientRect();
                var s = el.ownerDocument && el.ownerDocument.defaultView && el.ownerDocument.defaultView.getComputedStyle(el);
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
              var docs = [document];
              var iframes = document.querySelectorAll('iframe');
              for (var i = 0; i < iframes.length; i++) {
                try {
                  var frameDoc = iframes[i].contentDocument;
                  if (frameDoc && docs.indexOf(frameDoc) < 0) docs.push(frameDoc);
                } catch (e) {}
              }
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
        """

        fun playerEmbedUrl(target: TwitchPlaybackTarget): String = when (target) {
            is TwitchPlaybackTarget.Channel ->
                "https://player.twitch.tv/?channel=${target.login}&parent=player.twitch.tv&autoplay=true&muted=false"
            is TwitchPlaybackTarget.Video ->
                "https://player.twitch.tv/?video=v${target.id.removePrefix("v")}&parent=player.twitch.tv&autoplay=true&muted=false"
        }

        fun isPlayerDocumentUrl(url: String): Boolean {
            if (url.isBlank() || url.startsWith("about:") || url.startsWith("data:")) return false
            val host = url.toUri().host?.lowercase() ?: return url.contains("player.twitch.tv")
            return host == "player.twitch.tv" || host.endsWith(".player.twitch.tv")
        }

        fun shouldBlockNavigation(uri: Uri, isMainFrame: Boolean): Boolean {
            val url = uri.toString()
            if (url.startsWith("about:") || url.startsWith("data:") || url.startsWith("javascript:")) {
                return false
            }
            val host = uri.host?.lowercase() ?: return isMainFrame
            if (host == "player.twitch.tv" || host.endsWith(".player.twitch.tv")) return false
            if (isMainFrame) return true
            if (host == "www.twitch.tv" || host == "twitch.tv" || host == "m.twitch.tv") {
                val path = uri.path.orEmpty()
                return !(path.startsWith("/embed") || path.startsWith("/player"))
            }
            return false
        }

        const val HIDE_TOP_OVERLAY_JS = """
            (function() {
              function allDocs() {
                var docs = [document];
                var iframes = document.querySelectorAll('iframe');
                for (var i = 0; i < iframes.length; i++) {
                  try {
                    var d = iframes[i].contentDocument;
                    if (d && docs.indexOf(d) < 0) docs.push(d);
                  } catch (e) {}
                }
                return docs;
              }
              function playerDoc() {
                var docs = allDocs();
                for (var i = 0; i < docs.length; i++) {
                  if (docs[i].querySelector && docs[i].querySelector('video')) return docs[i];
                }
                return docs[0];
              }
              function disclosureText(el) {
                return ((el && (el.getAttribute('aria-label') || '')) + ' ' + ((el && el.textContent) || '')).toLowerCase();
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
                node.style.setProperty('max-width', 'calc(100% - 60px)', 'important');
                node.style.setProperty('box-sizing', 'border-box', 'important');
                node.style.setProperty('overflow-wrap', 'anywhere', 'important');
                node.style.setProperty('word-break', 'break-word', 'important');
                applyDisclosureTextWrap(node);
              }
              function compactText(el) {
                return ((el && el.textContent) || '').replace(/\s+/g, ' ').trim().toLowerCase();
              }
              function isDisclosureTitle(el) {
                var t = compactText(el);
                if (!t || t.length > 72) return false;
                return t.indexOf('certain audiences') !== -1
                  || t.indexOf('paid promotion') !== -1
                  || t.indexOf('определённ') !== -1
                  || t.indexOf('определенн') !== -1;
              }
              function applyDisclosureTextWrap(root) {
                var nodes = root.querySelectorAll('a, button, h1, h2, h3, h4, p, span, div, li, [role="button"]');
                for (var i = 0; i < nodes.length; i++) {
                  var el = nodes[i];
                  if (!el.style) continue;
                  if (isDisclosureTitle(el)) {
                    el.style.setProperty('white-space', 'nowrap', 'important');
                    continue;
                  }
                  el.style.setProperty('white-space', 'normal', 'important');
                  el.style.setProperty('max-width', '100%', 'important');
                  el.style.setProperty('overflow-wrap', 'anywhere', 'important');
                  el.style.setProperty('word-break', 'break-word', 'important');
                }
                if (isDisclosureTitle(root)) {
                  root.style.setProperty('white-space', 'nowrap', 'important');
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
                var named = doc.querySelectorAll('.top-bar, .stream-info-card, .stream-info-card__image, .stream-info-social-panel');
                for (var n = 0; n < named.length; n++) {
                  if (!isProtected(named[n]) && !isDisclosure(named[n])) hideNode(named[n]);
                }
                var overlay = doc.querySelector('.video-player__overlay');
                var first = overlay && overlay.firstElementChild;
                if (first && !isProtected(first) && !first.querySelector('.player-controls, video, [data-a-target="player-controls"]')) {
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
                    '  max-width: calc(100% - 60px) !important;',
                    '  box-sizing: border-box !important;',
                    '  overflow-wrap: anywhere !important;',
                    '}',
                    // As the plaque hides, Twitch lays the fading card out absolutely in a root that has shrunk to
                    // nothing at its right edge. Capped at that root's width and placed from its left, the card would
                    // collapse and spill its text to the right; it keeps its width and ends at that edge instead.
                    '.disclosure-tool .tw-transition, .dt-attach-top-right .tw-transition {',
                    '  left: auto !important;',
                    '  right: 0 !important;',
                    '  max-width: none !important;',
                    '}',
                    '.disclosure-tool *:not(svg):not(path):not(img), .dt-attach-top-right *:not(svg):not(path):not(img) {',
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
                  var docs = allDocs();
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
                if (doc.querySelector('[data-a-target*="ad-notice"], [data-a-target*="video-ad"], [data-test-selector*="ad-"], [class*="ad-overlay"], [class*="video-ads"]')) {
                  return true;
                }
                var labeled = doc.querySelectorAll('[aria-label]');
                for (var i = 0; i < labeled.length; i++) {
                  var a = (labeled[i].getAttribute('aria-label') || '').toLowerCase();
                  if (a.indexOf('advertisement') !== -1 || a.indexOf('ad break') !== -1 || a.indexOf('реклам') !== -1) {
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
              // Only a recording reads the position; a live stream would pay a bridge call per timeupdate.
              var recordingPage = /[?&]video=/.test(location.search);
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
                var s = el.ownerDocument && el.ownerDocument.defaultView && el.ownerDocument.defaultView.getComputedStyle(el);
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
                var docs = allDocs();
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
              function notifyPlayerError() {
                var button = playerErrorButton();
                if (!button || button.__customTvErrorReported) return;
                button.__customTvErrorReported = true;
                try { CustomTvPlayback.onPlayerError(); } catch (e) {}
              }
              function isOffline() {
                var docs = allDocs();
                for (var d = 0; d < docs.length; d++) {
                  var doc = docs[d];
                  if (!doc || !doc.querySelector) continue;
                  if (doc.querySelector('[data-a-target*="offline"], [class*="player-overlay-offline"], [class*="offline-overlay"]')) {
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
                var docs = allDocs();
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
                var docs = allDocs();
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
                try { CustomTvPlayback.onControlBar(fraction); } catch (e) {}
              }
              // Twitch's settings menu rises over the bottom corner, where the app's chat buttons sit.
              function readSettingsMenuOpen() {
                var docs = allDocs();
                for (var i = 0; i < docs.length; i++) {
                  var d = docs[i];
                  if (d.querySelector && d.querySelector('[data-a-target="player-settings-menu"]')) return true;
                }
                return false;
              }
              var reportedSettingsMenu = null;
              function notifySettingsMenu() {
                var open = readSettingsMenuOpen();
                if (open === reportedSettingsMenu) return;
                reportedSettingsMenu = open;
                try { CustomTvPlayback.onSettingsMenu(open); } catch (e) {}
              }
              var reportedControls = null;
              function notifyControls() {
                notifyControlBar();
                notifySettingsMenu();
                var controls = readControlsVisible();
                if (controls === reportedControls) return;
                reportedControls = controls;
                try { CustomTvPlayback.onControlsVisible(controls); } catch (e) {}
              }
              function armChromeSync(doc) {
                var root = doc && doc.querySelector && doc.querySelector('.video-player__default-player');
                if (!root || root.__customTvChromeObs) return;
                root.__customTvChromeObs = true;
                var obs = new MutationObserver(notifyControls);
                obs.observe(root, { attributes: true, attributeFilter: ['class'] });
                // The settings menu comes and goes as an element under the player.
                new MutationObserver(notifySettingsMenu).observe(root, { childList: true, subtree: true });
                notifyControls();
              }
              function armScrollableTouch(doc) {
                if (!doc || !doc.documentElement || doc.documentElement.__customTvTouchBound) return;
                doc.documentElement.__customTvTouchBound = true;
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
                  // The layer over the video that plays or pauses a recording on a click; buttons and notes sit apart.
                  var video = !!(event.target && event.target.closest &&
                    event.target.closest('[data-a-target="player-overlay-click-handler"]'));
                  try { CustomTvPlayback.onTouchStart(scrollable, controls, video); } catch (e) {}
                }, { capture: true, passive: true });
              }
              function armPlaybackSync(targetDoc) {
                if (!targetDoc || !targetDoc.documentElement || targetDoc.documentElement.__customTvSync) return;
                targetDoc.documentElement.__customTvSync = true;
                function bind(v) {
                  if (v.__customTvBound) return;
                  v.__customTvBound = true;
                  notifyPlayerReady();
                  v.addEventListener('playing', function() { notifyPlaybackIfRendered(v); });
                  v.addEventListener('timeupdate', function() {
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
                var docs = allDocs();
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
              if (window.__customTvOverlayBooted) return;
              window.__customTvOverlayBooted = true;
              // The DOM observers and iframe loads schedule inject(); layout changes only on resize.
              window.addEventListener('resize', schedule);
              window.addEventListener('resize', notifyControlBar);
              setInterval(function() {
                notifyControls();
                notifyOffline();
                armPlayerReload();
                notifyPlayerError();
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
        """

        /**
         * Switches the player to sound only, remembering whether it chose the quality itself or played a fixed one.
         * Does nothing while the playlist lacks the audio-only rendition.
         */
        const val SOUND_ONLY_JS = """
            (function() {
              function player() {
                var nodes = [document.querySelector('video')].concat([].slice.call(document.querySelectorAll('div')));
                for (var i = 0; i < nodes.length; i++) {
                  var n = nodes[i];
                  if (!n) continue;
                  var key = Object.keys(n).find(function(k) { return k.indexOf('__reactFiber') === 0; });
                  if (!key) continue;
                  for (var f = n[key], depth = 0; f && depth < 80; f = f.return, depth++) {
                    if (f.memoizedProps && f.memoizedProps.mediaPlayerInstance) return f.memoizedProps.mediaPlayerInstance;
                  }
                }
                return null;
              }
              var p = player();
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
        """

        /** Sets the player's own volume to full, so only the system volume and the mute button change the loudness. */
        const val FULL_VOLUME_JS = """
            (function() {
              var nodes = [document.querySelector('video')].concat([].slice.call(document.querySelectorAll('div')));
              for (var i = 0; i < nodes.length; i++) {
                var n = nodes[i];
                if (!n) continue;
                var key = Object.keys(n).find(function(k) { return k.indexOf('__reactFiber') === 0; });
                if (!key) continue;
                for (var f = n[key], depth = 0; f && depth < 80; f = f.return, depth++) {
                  var p = f.memoizedProps && f.memoizedProps.mediaPlayerInstance;
                  if (p && p.setVolume) {
                    if (p.getVolume && p.getVolume() < 1) p.setVolume(1);
                    return;
                  }
                }
              }
            })();
        """

        /** Brings the video back as it played before [SOUND_ONLY_JS]: the player's own choice or the fixed quality. */
        const val VIDEO_QUALITY_JS = """
            (function() {
              var before = window.__customTvVideoQuality;
              if (!before) return;
              window.__customTvVideoQuality = null;
              var nodes = [document.querySelector('video')].concat([].slice.call(document.querySelectorAll('div')));
              var p = null;
              for (var i = 0; i < nodes.length && !p; i++) {
                var n = nodes[i];
                if (!n) continue;
                var key = Object.keys(n).find(function(k) { return k.indexOf('__reactFiber') === 0; });
                if (!key) continue;
                for (var f = n[key], depth = 0; f && depth < 80; f = f.return, depth++) {
                  if (f.memoizedProps && f.memoizedProps.mediaPlayerInstance) { p = f.memoizedProps.mediaPlayerInstance; break; }
                }
              }
              if (!p || !p.getQualities) return;
              var current = p.getQuality();
              if (!current || current.group !== 'audio_only') return;
              var fixed = before.group && p.getQualities().find(function(q) { return q.group === before.group; });
              if (!before.auto && fixed) p.setQuality(fixed);
              else if (p.setAutoQualityMode) p.setAutoQualityMode(true);
              else p.setQuality(p.getQualities()[0]);
            })();
        """

        /**
         * Hides the player's control bar in picture-in-picture, where the system draws its own controls over the small
         * window. Called with `true` or `false`; the bar keeps its layout, so the reported control bar height stays.
         */
        private const val PICTURE_IN_PICTURE_JS = """
            (function(hide) {
              var css = '[data-a-target="player-controls"] { opacity: 0 !important; pointer-events: none !important; }';
              function go(doc) {
                if (!doc || !doc.documentElement) return;
                var style = doc.getElementById('customtv-pip');
                if (!hide) {
                  if (style) style.remove();
                  return;
                }
                if (style) return;
                style = doc.createElement('style');
                style.id = 'customtv-pip';
                style.textContent = css;
                (doc.head || doc.documentElement).appendChild(style);
              }
              go(document);
              var iframes = document.querySelectorAll('iframe');
              for (var i = 0; i < iframes.length; i++) {
                try { go(iframes[i].contentDocument); } catch (e) {}
              }
            })"""

        fun pictureInPictureJs(enabled: Boolean): String = "$PICTURE_IN_PICTURE_JS($enabled);"
}
