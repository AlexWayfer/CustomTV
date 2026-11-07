(function(start, end) {
    // buffered is the intersection of audio and video; it can hide old data in either track.
    // Keep the SDK's scheduling, but remove the entire requested interval from every track.
    var sink = this;
    Object.keys(sink.sourceBuffers).forEach(function(track) {
        sink.sourceBuffers[track].schedule(function(buffer) {
            var first = Math.max(0, start);
            var duration = sink.mediaSource.duration;
            // The duration can change while a queued operation waits for an append to finish.
            if (!isNaN(duration) && first < duration && first < end) buffer.remove(first, end);
        });
    });
})
