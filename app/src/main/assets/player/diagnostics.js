// Pure transition detection, shared by the live sampler and its tests.
var customTvDiagnosticChanges = function(before, after, live) {
    var changes = [];
    // Ignore subsecond clock corrections; a larger backwards move can reset the live timeline.
    var RESET_BACKSTEP_SECONDS = 1;
    if (live && before && after.time < before.time - RESET_BACKSTEP_SECONDS) changes.push('timeline-reset');
    var split = live && after.ranges.length > 1;
    var wasSplit = live && before && before.ranges.length > 1;
    if (split && !wasSplit) changes.push('buffer-split');
    if (!split && wasSplit) changes.push('buffer-joined');
    if (before && before.quality !== after.quality) changes.push('quality');
    return changes;
};

var customTvDiagnosticSnapshot = function(doc, v) {
    if (!v || !isFinite(v.currentTime)) return null;
    var ranges = [];
    try {
        for (var i = 0; i < v.buffered.length; i++) {
            ranges.push([v.buffered.start(i), v.buffered.end(i)]);
        }
    } catch (e) {}
    var quality = '';
    var p = v.__customTvPlayer || (v.__customTvPlayer = customTvPlayer(doc));
    try {
        var q = p && p.getQuality();
        if (q) quality = q.group || '';
    } catch (e) {}
    return { time: v.currentTime, ranges: ranges, quality: quality, paused: v.paused };
};

// Fixed fields only: no URLs, session data, or page text can enter the problem report.
var customTvDiagnosticWrite = function(event, snapshot) {
    if (!snapshot) return;
    var ranges = snapshot.ranges.map(function(r) {
        return '[' + r[0].toFixed(3) + ', ' + r[1].toFixed(3) + ']';
    }).join(' ');
    try {
        CustomTvDiagnostics.onDiagnostic(event, snapshot.time, snapshot.paused, ranges, snapshot.quality);
    } catch (e) {}
};

var customTvDiagnosticEvent = function(doc, v, event) {
    customTvDiagnosticWrite(event, customTvDiagnosticSnapshot(doc, v));
};

var customTvDiagnosticTick = function(doc, v, live) {
    var after = customTvDiagnosticSnapshot(doc, v);
    if (!after) return;
    var before = v.__customTvDiagnosticPrevious || null;
    v.__customTvDiagnosticPrevious = after;
    var changes = customTvDiagnosticChanges(before, after, live);
    var last = v.__customTvDiagnosticLast || (v.__customTvDiagnosticLast = {});
    // Oscillating ranges must not fill the report; retain at most one pair per kind in this interval.
    var REPEAT_INTERVAL_MS = 10000;
    changes.forEach(function(event) {
        var now = Date.now();
        if (last[event] !== undefined && now - last[event] < REPEAT_INTERVAL_MS) return;
        last[event] = now;
        customTvDiagnosticWrite(event + '-before', before);
        customTvDiagnosticWrite(event + '-after', after);
    });
};
