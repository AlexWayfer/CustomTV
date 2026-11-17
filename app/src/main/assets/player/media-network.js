// Each document listens only to the worker script served for its own load.
var customTvMediaNetwork = typeof customTvMediaNetwork === 'undefined' ? null : customTvMediaNetwork;
var customTvStartMediaNetwork = function(page) {
    if (customTvMediaNetwork || typeof BroadcastChannel !== 'function') return;
    var channel;
    try { channel = new BroadcastChannel('customtv-media-network/' + page); } catch (e) { return; }
    var recent = new Map();
    var latestPlaylist = null;
    var latestPlaylistResponseAt = null;
    var latestSegmentRetry = null;
    var limit = 16;
    var segmentRetryMs = 2000;
    var recentPlaylistResponseMs = 3000;
    var earlyRetryMs = 4000;
    var playlistTimeoutMs = 10000;
    var lastRequestedSegmentRetry = null;
    var playlistRetryRequested = false;
    channel.onmessage = function(event) {
        var request = event.data;
        if (!request || typeof request.id !== 'string' || request.id.length > 32) return;
        if (!/^(manifest|playlist|segment)$/.test(request.kind) ||
            !/^(open|retrying|headers|closed|http|aborted|failed|body-error)$/.test(request.state)) return;
        if (![request.started, request.status, request.headers, request.elapsed].every(Number.isSafeInteger)) return;
        if (request.kind === 'playlist' && (!latestPlaylist || request.id === latestPlaylist.id ||
            request.started > latestPlaylist.started ||
            (request.started === latestPlaylist.started && request.state === 'open'))) latestPlaylist = request;
        if (request.kind === 'playlist' && request.state === 'closed' && request.status >= 200 &&
            request.status < 300) {
            latestPlaylistResponseAt = Math.max(latestPlaylistResponseAt || 0, request.started + request.elapsed);
            if (request === latestPlaylist) playlistRetryRequested = false;
        }
        if (request.kind === 'segment' && request.retry === true &&
            (!latestSegmentRetry || request.id === latestSegmentRetry.id ||
                request.started > latestSegmentRetry.started ||
                (request.started === latestSegmentRetry.started && request.state === 'open'))) {
            latestSegmentRetry = request;
        }
        recent.delete(request.id);
        recent.set(request.id, {
            kind: request.kind, state: request.state, status: request.status,
            started: request.started, headers: request.headers, elapsed: request.elapsed, retry: request.retry
        });
        if (recent.size > limit) recent.delete(recent.keys().next().value);
    };
    customTvMediaNetwork = {
        report: function() {
            var now = Date.now();
            return Array.from(recent.values(), function(request) {
                var age = Math.max(0, now - request.started);
                var elapsed = /^(open|retrying|headers)$/.test(request.state) ? age : request.elapsed;
                return [request.kind, request.state, request.status, request.headers, elapsed, age].join(' ');
            }).join('\n');
        },
        playlistWaiting: function() {
            return latestPlaylist && /^(open|retrying)$/.test(latestPlaylist.state) &&
                Date.now() - latestPlaylist.started >= playlistTimeoutMs;
        },
        segmentRetry: function() {
            if (!latestSegmentRetry || !/^(open|headers|closed)$/.test(latestSegmentRetry.state)) return null;
            var now = Date.now();
            var age = now - latestSegmentRetry.started;
            if (age < 0 || age >= segmentRetryMs) return null;
            return { id: latestSegmentRetry.id, remaining: segmentRetryMs - age };
        },
        hasRecentPlaylistResponse: function() {
            var age = Date.now() - latestPlaylistResponseAt;
            return latestPlaylistResponseAt !== null && age >= 0 && age <= recentPlaylistResponseMs &&
                !(latestPlaylist && /^(http|failed|aborted|body-error)$/.test(latestPlaylist.state));
        },
        retrySlowSegment: function() {
            if (!this.hasRecentPlaylistResponse()) return;
            var now = Date.now();
            for (var entry of recent) {
                var id = entry[0];
                var request = entry[1];
                if (request.kind !== 'segment' || request.state !== 'open' || request.retry ||
                    now - request.started < earlyRetryMs) continue;
                if (id !== lastRequestedSegmentRetry) {
                    lastRequestedSegmentRetry = id;
                    channel.postMessage({ action: 'retry-media', id: id });
                }
                return;
            }
        },
        retrySlowPlaylist: function() {
            if (playlistRetryRequested || !latestPlaylist || latestPlaylist.state !== 'open') return;
            var age = Date.now() - latestPlaylist.started;
            if (age < earlyRetryMs || age >= playlistTimeoutMs) return;
            playlistRetryRequested = true;
            channel.postMessage({ action: 'retry-media', id: latestPlaylist.id });
        },
    };
};

var customTvBufferedAhead = function(video) {
    var ranges = video.buffered;
    var position = video.currentTime;
    for (var i = 0; i < ranges.length; i++) {
        var end = ranges.end(i);
        if (ranges.start(i) <= position && position < end) return end - position;
    }
    return 0;
};

var customTvWaitingForPlaylist = function(bufferedAhead) {
    return !!customTvMediaNetwork && !!customTvMediaNetwork.playlistWaiting() && bufferedAhead < 0.5;
};

var customTvSegmentRetry = function() {
    return customTvMediaNetwork && customTvMediaNetwork.segmentRetry();
};

var customTvRetrySlowSegment = function(bufferedAhead) {
    if (customTvMediaNetwork && bufferedAhead <= 2) customTvMediaNetwork.retrySlowSegment();
};

var customTvRetrySlowPlaylist = function(bufferedAhead, canRetry) {
    if (canRetry === true && customTvMediaNetwork && bufferedAhead <= 2) customTvMediaNetwork.retrySlowPlaylist();
};

var customTvCanWaitForSegmentRetry = function(bufferedAhead) {
    return bufferedAhead < 0.5 && !!customTvMediaNetwork && customTvMediaNetwork.hasRecentPlaylistResponse();
};

var customTvHasPlaybackBuffer = function(video, state, bufferedAhead) {
    return state === 'Playing' && !video.paused && !video.seeking && video.readyState >= 3 && bufferedAhead >= 1;
};

var customTvLogMediaNetwork = function() {
    try {
        var report = customTvMediaNetwork && customTvMediaNetwork.report();
        CustomTvDiagnostics.onMediaNetwork(report || 'unobserved');
    } catch (e) {}
};
