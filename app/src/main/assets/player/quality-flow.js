// Track requested modes separately from the state currently reported by the SDK.
var CUSTOM_TV_QUALITY_TRANSITION_TIMEOUT_MS = 10000;

var customTvQualityMatches = function(target, actual) {
    if (!actual || !actual.group) return false;
    if (target.auto) return actual.auto && actual.group !== 'audio_only';
    return target.group === actual.group && !actual.auto;
};

var customTvQualitySame = function(a, b) {
    return !!a && !!b && a.auto === b.auto && (a.auto || a.group === b.group);
};

// Leaving Auto must also cancel an ABR upgrade still queued for the currently reported rendition.
var customTvQualityAdaptive = function(currentGroup, nextGroup, automatic) {
    return !automatic && !!currentGroup && !!nextGroup && currentGroup !== 'audio_only' && nextGroup !== 'audio_only';
};

var customTvQualityHasPicture = function(width, height, audioOnly) {
    return width > 0 && height > 0 || audioOnly;
};

var customTvQualityResolve = function(target, qualities, canAuto) {
    if (!target.auto && qualities.some(function(q) { return q.group === target.group; })) return target;
    if (target.group === 'audio_only') return null;
    var video = qualities.find(function(q) { return q.group !== 'audio_only'; });
    if (!video) return null;
    return canAuto ? { auto: true, group: null } : { auto: false, group: video.group };
};

var customTvQualityFlow = function(doc) {
    var owner = doc.defaultView || window;
    if (owner.__customTvQualityFlow) return owner.__customTvQualityFlow;
    var player = null;
    var pending = null;
    var desired = null;
    var videoTarget = null;
    var audioOnly = false;
    var busy = false;

    function now() {
        return typeof performance !== 'undefined' ? performance.now() : Date.now();
    }
    function snapshot() {
        var quality = player && player.getQuality();
        return {
            group: quality ? quality.group : null,
            auto: player && player.isAutoQualityMode ? player.isAutoQualityMode() : false
        };
    }
    function log(request, event) {
        if (request && request.diagnostic) request.diagnostic(doc.querySelector('video'), event);
    }
    function connect() {
        var next = customTvPlayer(doc);
        if (!next) return null;
        if (next === player) return player;
        if (player && player.removeEventListener) {
            player.removeEventListener('PlayerQualityChanged', tick);
            player.removeEventListener('PlayerError', failed);
        }
        player = next;
        // A replacement player cannot complete an operation issued to the old one.
        pending = null;
        if (player.addEventListener) {
            player.addEventListener('PlayerQualityChanged', tick);
            player.addEventListener('PlayerError', failed);
        }
        return player;
    }
    function confirmed(actual) {
        if (!pending || !customTvQualityMatches(pending.target, actual)) return;
        var completed = pending;
        pending = null;
        log(completed, 'confirmed');
        if (desired && customTvQualityMatches(desired.target, actual)) {
            if (desired !== completed) log(desired, 'confirmed');
            desired = null;
        }
    }
    function failed() {
        var previous = pending;
        pending = null;
        log(previous, 'failed');
        // Do not retry a failed target indefinitely; a newer request can still restore the requested mode.
        if (previous && desired && customTvQualitySame(previous.target, desired.target)) desired = null;
        if (!busy) tick();
    }
    function tick() {
        if (busy || (!pending && !desired)) return;
        busy = true;
        try {
            if (!connect() || !player.getQualities) return;
            var actual = snapshot();
            confirmed(actual);
            if (pending && now() - pending.startedAt >= CUSTOM_TV_QUALITY_TRANSITION_TIMEOUT_MS) {
                var expired = pending;
                pending = null;
                log(expired, 'timeout');
                if (desired && customTvQualitySame(expired.target, desired.target)) desired = null;
                else if (desired) desired.force = true;
            }
            if (pending || !desired) return;
            if (!desired.force && customTvQualityMatches(desired.target, actual)) {
                log(desired, 'confirmed');
                desired = null;
                return;
            }
            var request = desired;
            var qualities = player.getQualities() || [];
            var target = customTvQualityResolve(request.target, qualities, !!player.setAutoQualityMode);
            if (!target) return;
            request.target = target;
            var fixed = !target.auto && qualities.find(function(q) { return q.group === target.group; });
            pending = request;
            pending.startedAt = now();
            var event = target.auto ? 'auto' : 'fixed';
            log(request, event + '-before');
            try {
                if (target.auto) player.setAutoQualityMode(true);
                else player.setQuality(fixed, !request.force && customTvQualityAdaptive(actual.group, target.group, actual.auto));
                log(request, event + '-after');
                if (!request.force) confirmed(snapshot());
            } catch (e) {
                failed();
            }
        } catch (e) {
            // SDK state may be unavailable while it is loading or being replaced; the page sampler tries again.
        } finally {
            busy = false;
        }
    }
    function submit(target, diagnostic) {
        if (desired && customTvQualitySame(desired.target, target)) {
            log({ diagnostic: diagnostic }, 'deferred');
            tick();
            return;
        }
        if (desired) log(desired, 'superseded');
        desired = { target: target, diagnostic: diagnostic };
        if (pending) log(desired, 'queued');
        tick();
    }
    var flow = {
        tick: tick,
        isPending: function() { return !!pending || !!desired; },
        isAudioOnly: function() { return audioOnly; },
        videoPreference: function() {
            if (videoTarget) return videoTarget;
            if (connect()) {
                var actual = snapshot();
                if (actual.group && actual.group !== 'audio_only') return actual;
            }
            return { auto: true, group: null };
        },
        video: function(target, diagnostic) {
            videoTarget = target;
            if (audioOnly) log({ diagnostic: diagnostic }, 'deferred');
            else submit(target, diagnostic);
        },
        audio: function(value, diagnostic) {
            if (value && !audioOnly) {
                // A manual menu choice made while idle is the viewer's current preference.
                if (!pending && !desired && connect()) {
                    var actual = snapshot();
                    if (actual.group && actual.group !== 'audio_only') videoTarget = actual;
                }
                if (!videoTarget) videoTarget = flow.videoPreference();
            }
            if (!value && !audioOnly) return;
            audioOnly = value;
            owner.__customTvAudioOnlyRequested = value;
            submit(value ? { auto: false, group: 'audio_only' } : videoTarget, diagnostic);
        }
    };
    owner.__customTvQualityFlow = flow;
    return flow;
};

// The existing sampler also handles SDKs that acknowledge Auto without a quality-change event.
var customTvQualityTick = function(doc) {
    var owner = doc.defaultView || window;
    if (owner.__customTvQualityFlow) owner.__customTvQualityFlow.tick();
};
