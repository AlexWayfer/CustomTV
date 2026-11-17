// The auxiliary player shares Twitch's loaded worker, but has its own playback position and media element.
var customTvCreateHandoffPlayer = function(doc, player, onFailure) {
    var sdk = player.playerInstance;
    var core = sdk && sdk.core;
    if (!core || !core.worker || !Number.isInteger(core.id) || typeof sdk.getPath !== 'function') return null;
    var path = new URL(sdk.getPath());
    if (path.protocol !== 'https:' || path.host !== 'usher.ttvnw.net' ||
        !/^\/api\/channel\/hls\/[^/]+\.m3u8$/.test(path.pathname)) return null;
    if (!player.getQualities().some(function(q) { return q.group === 'audio_only'; })) return null;
    var media = doc.createElement('video');
    media.muted = true;
    media.playsInline = true;
    var auxiliary = new sdk.constructor({}, core.worker);
    try {
        auxiliary.attachHTMLVideoElement(media);
        auxiliary.setMuted(true);
        auxiliary.addEventListener('PlayerError', onFailure);
        auxiliary.addEventListener('PlayerAdBreakStarted', onFailure);
        auxiliary.addEventListener('Ready', function() {
            var audio = auxiliary.getQualities().find(function(q) { return q.group === 'audio_only'; });
            if (!audio) {
                onFailure();
                return;
            }
            try {
                auxiliary.setQuality(audio, false);
                auxiliary.play();
            } catch (error) {
                onFailure();
            }
        });
        media.__customTvPlayer = auxiliary;
        return { player: auxiliary, media: media, load: function() { auxiliary.load(path.href); } };
    } catch (error) {
        auxiliary.delete();
        throw error;
    }
};

var customTvAudioHandoff = function(doc) {
    var owner = doc.defaultView;
    if (!owner || !doc.location || !/[?&]channel=/.test(doc.location.search)) return null;
    if (owner.__customTvAudioHandoff) return owner.__customTvAudioHandoff;
    var tolerance = 0.008;
    var graph = null;
    var primary = null;
    var secondary = null;
    var wanted = false;
    var attempted = false;
    var active = false;
    var deadline = 0;
    var diagnostic;
    var cutAt = null;
    var stopMonitor = null;
    var warmupSamples = null;
    var ignoreUntil = 0;
    var triedSeek = false;

    function log(event) {
        if (diagnostic) diagnostic(doc.querySelector('video'), event);
    }
    function ready(source) {
        return source && source.player.getState() === 'Playing' && !source.media.paused &&
            !source.media.seeking && source.media.readyState >= 3;
    }
    function select(primaryValue, secondaryValue) {
        if (!graph || !primary.track) return;
        var now = graph.context.currentTime;
        [primary, secondary].forEach(function(source, i) {
            if (!source || !source.track) return;
            source.track.gain.cancelScheduledValues(0);
            source.track.gain.setValueAtTime(i === 0 ? primaryValue : secondaryValue, now);
        });
    }
    function discard(resumeVideo) {
        var previous = secondary;
        var wasActive = active;
        if (stopMonitor) stopMonitor();
        stopMonitor = null;
        warmupSamples = null;
        select(1, 0);
        cutAt = null;
        secondary = null;
        active = false;
        if (!previous) return;
        customTvFadeStop(previous.media);
        if (previous.track) previous.track.disconnect();
        previous.player.delete();
        if (resumeVideo !== false && wasActive && !owner.__customTvUserPaused && primary.player.isPaused()) {
            primary.player.play();
        }
    }
    function fail(event) {
        if (!secondary) return;
        log(event);
        discard();
    }
    function collect(data) {
        if (!wanted || cutAt !== null || !ready(primary) || !ready(secondary) ||
            data.at < ignoreUntil || graph.context.currentTime - data.at > 0.5) {
            warmupSamples = null;
            return;
        }
        function append(previous, block) {
            var count = Math.min(previous ? previous.length : 0, Math.round(data.rate * 3));
            var samples = new Float32Array(count + block.length);
            if (count) samples.set(previous.subarray(previous.length - count));
            samples.set(block, count);
            return samples;
        }
        warmupSamples = {
            first: append(warmupSamples && warmupSamples.first, data.first),
            second: append(warmupSamples && warmupSamples.second, data.second)
        };
        if (warmupSamples.first.length < data.rate) return;
        var delta = customTvMatchHandoffAudio(warmupSamples.first, warmupSamples.second, data.rate);
        if (delta === null) return;
        if (Math.abs(delta) <= tolerance) {
            // Give the audio renderer time to receive both changes before their common deadline.
            cutAt = graph.context.currentTime + 0.05;
            primary.track.gain.setValueAtTime(0, cutAt);
            secondary.track.gain.setValueAtTime(1, cutAt);
            return;
        }
        var delay = secondary.track.delay.delayTime;
        var nextDelay = delay.value + delta;
        if (nextDelay >= 0 && nextDelay <= delay.maxValue) {
            delay.value = nextDelay;
            ignoreUntil = graph.context.currentTime + delay.value + 0.5;
            warmupSamples = null;
        } else if (delta < 0 && !triedSeek) {
            var to = secondary.media.currentTime - delta + 0.15;
            var buffered = secondary.media.buffered;
            for (var i = 0; i < buffered.length; i++) {
                if (buffered.start(i) <= to && to < buffered.end(i)) {
                    triedSeek = true;
                    secondary.player.seekTo(to);
                    ignoreUntil = graph.context.currentTime + 0.5;
                    warmupSamples = null;
                    break;
                }
            }
        }
    }
    function route(source) {
        if (source !== secondary || !wanted) return;
        source.track = graph.track(source.media, false);
        if (!primary.track) primary.track = graph.track(primary.media, true);
        source.player.setMuted(primary.media.muted);
        source.player.setVolume(primary.media.volume);
        stopMonitor = graph.monitor(primary.track, source.track, function(data) {
            if (source !== secondary) return;
            try { collect(data); } catch (error) { fail('failed'); }
        });
    }
    async function openGraph(source) {
        source.openingGraph = true;
        try {
            if (!graph) {
                var opened = await customTvOpenHandoffGraph(owner);
                if (source !== secondary) {
                    await opened.context.close();
                    return;
                }
                graph = opened;
            }
            route(source);
        } catch (error) {
            if (source === secondary) fail('failed');
        }
    }
    function finishCut() {
        if (!secondary || cutAt === null || graph.context.currentTime < cutAt) return;
        cutAt = null;
        active = true;
        if (stopMonitor) stopMonitor();
        stopMonitor = null;
        warmupSamples = null;
        primary.player.pause();
        log('confirmed');
    }
    function tick(adBreak) {
        try {
            finishCut();
            var media = doc.querySelector('video');
            var player = media && (media.__customTvPlayer || customTvPlayer(doc));
            if (primary && (primary.media !== media || player !== primary.player)) {
                discard(false);
                if (graph) void graph.context.close().catch(function() {});
                graph = null;
                primary = null;
                attempted = false;
            }
            if (adBreak) {
                fail('failed');
                return;
            }
            if (secondary) {
                if (!wanted) {
                    // The video resumes muted. Keep the auxiliary audible until the video can render again.
                    if (ready(primary) || Date.now() >= deadline) {
                        if (ready(primary)) log('confirmed');
                        else log('timeout');
                        discard();
                    }
                } else if (!active && Date.now() >= deadline) {
                    fail('timeout');
                } else if (!active && !secondary.openingGraph && ready(secondary) &&
                    secondary.player.getQuality().group === 'audio_only') {
                    void openGraph(secondary);
                }
                return;
            }
            if (!wanted || attempted || !player || owner.__customTvUserPaused) return;
            if (!primary) primary = { media: media, player: player };
            if (!ready(primary) || !owner.AudioContext || !owner.AudioWorkletNode) return;
            attempted = true;
            triedSeek = false;
            ignoreUntil = 0;
            var source = customTvCreateHandoffPlayer(doc, player, function() {
                if (source === secondary) fail('failed');
            });
            if (!source) {
                log('failed');
                return;
            }
            secondary = source;
            deadline = Date.now() + CUSTOM_TV_QUALITY_TRANSITION_TIMEOUT_MS;
            log('queued');
            source.load();
        } catch (error) {
            if (secondary) fail('failed');
            else log('failed');
        }
    }
    owner.addEventListener('pagehide', function() {
        wanted = false;
        discard(false);
        if (graph) void graph.context.close().catch(function() {});
        graph = null;
    });
    var flow = {
        request: function(value, command) {
            finishCut();
            if (wanted === value) return;
            wanted = value;
            diagnostic = command;
            attempted = !!secondary;
            if (value && active) {
                primary.player.pause();
            } else if (!value && active && !owner.__customTvUserPaused) {
                deadline = Date.now() + CUSTOM_TV_QUALITY_TRANSITION_TIMEOUT_MS;
                primary.player.play();
            } else if (!value) {
                discard();
            }
        },
        tick: tick,
        output: function() { return active ? secondary : null; },
        isParallel: function() { return !!secondary; },
        command: function(playing) {
            if (playing && graph && graph.context.state !== 'running') {
                void graph.context.resume().catch(function() {});
            }
            finishCut();
            if (!active) {
                if (!playing && secondary) {
                    discard();
                    attempted = false;
                }
                return false;
            }
            var output = secondary;
            if (playing) {
                if (output.player.isPaused() || customTvFadingOut(output.media)) {
                    customTvFade(output.media, 1, function() { output.player.setVolume(1); });
                    if (output.player.isPaused()) output.player.play();
                }
                if (!wanted && primary.player.isPaused()) primary.player.play();
            } else if (!output.player.isPaused()) {
                customTvFade(output.media, 0, function() {
                    if (output !== secondary) return;
                    output.player.pause();
                    output.media.volume = 1;
                    if (!wanted) {
                        primary.player.pause();
                        discard();
                    }
                });
            }
            return true;
        }
    };
    owner.__customTvAudioHandoff = flow;
    return flow;
};
