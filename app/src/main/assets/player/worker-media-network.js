(function(page) {
    if (typeof BroadcastChannel !== 'function' || typeof self.fetch !== 'function' ||
        typeof ReadableStream !== 'function') return;
    var channel;
    try { channel = new BroadcastChannel('customtv-media-network/' + page); } catch (e) { return; }
    var worker = Math.random().toString(36).slice(2);
    var sequence = 0;
    var bodies = new WeakMap();
    var unansweredSegments = new Map();
    var pendingMedia = new Map();
    var segmentTimeoutMs = 10000;
    var retryStartLimitMs = 3000;

    function kindOf(input, options) {
        var method = (options && options.method) || (input && input.method) || 'GET';
        if (String(method).toUpperCase() !== 'GET') return '';
        var url = new URL(input && input.url || input);
        if (url.protocol !== 'https:' || !/(^|\.)(ttvnw\.net|hls\.live-video\.net)$/.test(url.hostname)) return '';
        var path = url.pathname;
        if (url.hostname === 'usher.ttvnw.net' && path.endsWith('.m3u8')) return 'manifest';
        if (path.startsWith('/v1/playlist/') || path.endsWith('.m3u8')) return 'playlist';
        return path.startsWith('/v1/segment/') || /\.(ts|mp4|m4s)$/.test(path) ? 'segment' : '';
    }

    function report(request, state) {
        try {
            channel.postMessage({
                id: request.id, kind: request.kind, started: request.started, state: state,
                status: request.status, headers: request.headers, elapsed: Math.max(0, Date.now() - request.started),
                retry: request.retry
            });
        } catch (e) {}
    }

    var fetch = self.fetch;
    channel.onmessage = function(event) {
        var message = event.data;
        if (!message || message.action !== 'retry-media') return;
        var controller = pendingMedia.get(message.id);
        if (!controller) return;
        pendingMedia.delete(message.id);
        controller.abort();
    };

    function fetchMedia(receiver, args, request) {
        var input = args[0];
        var options = args[1];
        var signal = options && options.signal !== undefined ? options.signal : input && input.signal;
        if (request.retry || !signal || typeof AbortSignal === 'undefined' ||
            typeof AbortSignal.any !== 'function') return fetch.apply(receiver, args);
        var controller = new AbortController();
        var attemptOptions = Object.assign({}, options, { signal: AbortSignal.any([signal, controller.signal]) });
        var first = fetch.call(receiver, input, attemptOptions);
        pendingMedia.set(request.id, controller);
        return first.then(function(response) {
            pendingMedia.delete(request.id);
            return response;
        }, function(error) {
            pendingMedia.delete(request.id);
            if (!controller.signal.aborted || signal.aborted) throw error;
            report(request, 'retrying');
            return fetch.apply(receiver, args);
        });
    }

    self.fetch = function(input, options) {
        var kind;
        try { kind = kindOf(input, options); } catch (e) {}
        if (!kind) return fetch.apply(this, arguments);
        var started = Date.now();
        var url = kind === 'segment' ? new URL(input && input.url || input).href : null;
        var failedAt = unansweredSegments.get(url);
        unansweredSegments.delete(url);
        var retry = failedAt !== undefined && started >= failedAt && started - failedAt <= retryStartLimitMs;
        var request = { id: worker + ':' + ++sequence, kind: kind, started: started, status: 0, headers: -1,
            retry: retry };
        var result = kind === 'manifest' ? fetch.apply(this, arguments) : fetchMedia(this, arguments, request);
        try {
            report(request, 'open');
            result.then(function(response) {
                request.status = response.status;
                request.headers = Math.max(0, Date.now() - request.started);
                if (response.status >= 400) {
                    report(request, 'http');
                } else {
                    if (response.body) bodies.set(response.body, request);
                    report(request, response.body ? 'headers' : 'closed');
                }
            }, function(error) {
                var failedAt = Date.now();
                if (url && !retry && failedAt - started >= segmentTimeoutMs) {
                    // Keep the signed URL inside this worker, only until a repeat or another recent failure.
                    unansweredSegments.set(url, failedAt);
                    unansweredSegments.forEach(function(at, key) {
                        if (failedAt - at > retryStartLimitMs) unansweredSegments.delete(key);
                    });
                    if (unansweredSegments.size > 16) unansweredSegments.delete(unansweredSegments.keys().next().value);
                }
                report(request, error && error.name === 'AbortError' ? 'aborted' : 'failed');
            }).catch(function() {});
        } catch (e) {}
        return result;
    };

    var getReader = ReadableStream.prototype.getReader;
    ReadableStream.prototype.getReader = function() {
        var reader = getReader.apply(this, arguments);
        try {
            var request = bodies.get(this);
            if (request) {
                // The SDK reads the original stream. A fulfilled closed promise can also mean cancellation.
                reader.closed.then(function() {
                    report(request, 'closed');
                }, function(error) {
                    report(request, error && error.name === 'AbortError' ? 'aborted' : 'body-error');
                });
            }
        } catch (e) {}
        return reader;
    };
})
