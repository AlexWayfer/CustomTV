// Find the difference in broadcast position from the decoded signal. Each player's media clock has its own zero.
var customTvMatchHandoffAudio = function(first, second, rate, maxLag) {
    var windowSize = Math.round(rate / 3);
    var length = Math.min(first.length, second.length);
    var limit = Math.min(length - windowSize - 1, Math.round(rate * (maxLag || 3)));
    if (limit < 0) return null;
    var scores = new Float32Array(2 * limit + 1);
    var best = -1;
    var bestShift = 0;
    for (var shift = -limit; shift <= limit; shift++) {
        var a = length - windowSize - Math.max(shift, 0);
        var b = length - windowSize + Math.min(shift, 0);
        var sum = 0;
        var energyA = 0;
        var energyB = 0;
        // Compare changes in the signal so steady noise or silence cannot confirm an alignment.
        for (var i = 1; i < windowSize; i += 2) {
            var x = first[a + i] - first[a + i - 1];
            var y = second[b + i] - second[b + i - 1];
            sum += x * y;
            energyA += x * x;
            energyB += y * y;
        }
        var score = energyA > 0.0001 && energyB > 0.0001 ? sum / Math.sqrt(energyA * energyB) : 0;
        scores[shift + limit] = score;
        if (score > best) {
            best = score;
            bestShift = shift;
        }
    }
    if (best < 0.9) return null;
    // Periodic sound can have several nearly equal matches well outside the precision of the cut.
    var radius = Math.ceil(rate * 0.008);
    for (var j = 0; j < scores.length; j++) {
        if (Math.abs(j - limit - bestShift) > radius && scores[j] > best - 0.05) return null;
    }
    return -bestShift / rate || 0;
};

// Serialized into a worklet module. Only short, downsampled blocks go back to the page for comparison.
var customTvHandoffWorklet = function() {
    registerProcessor('customtv-audio-handoff', class extends AudioWorkletProcessor {
        constructor() {
            super();
            this.stride = Math.max(1, Math.round(sampleRate / 3000));
            this.rate = sampleRate / this.stride;
            this.blockSize = Math.round(this.rate / 2);
            this.nextBlock();
            this.frame = 0;
        }
        nextBlock() {
            this.first = new Float32Array(this.blockSize);
            this.second = new Float32Array(this.blockSize);
            this.index = 0;
        }
        process(inputs) {
            var first = inputs[0][0];
            var second = inputs[1][0];
            if (!first || !second) {
                if (this.index) this.nextBlock();
                return true;
            }
            for (; this.frame < first.length; this.frame += this.stride) {
                this.first[this.index] = first[this.frame];
                this.second[this.index++] = second[this.frame];
                if (this.index === this.blockSize) {
                    this.port.postMessage({ first: this.first, second: this.second, rate: this.rate, at: currentTime },
                        [this.first.buffer, this.second.buffer]);
                    this.nextBlock();
                }
            }
            this.frame -= first.length;
            return true;
        }
    });
};

var customTvOpenHandoffGraph = async function(owner) {
    var context = new owner.AudioContext();
    try {
        var module = owner.URL.createObjectURL(new owner.Blob(['(' + customTvHandoffWorklet + ')()'],
            { type: 'text/javascript' }));
        try {
            await context.audioWorklet.addModule(module);
            await context.resume();
            if (context.state !== 'running') throw new Error('Audio context suspended');
        } finally {
            owner.URL.revokeObjectURL(module);
        }
    } catch (error) {
        void context.close().catch(function() {});
        throw error;
    }
    function track(media, audible) {
        var delay = context.createDelay(3);
        var gain = context.createGain();
        gain.gain.value = audible ? 1 : 0;
        delay.connect(gain).connect(context.destination);
        var source;
        try {
            source = context.createMediaElementSource(media);
            source.connect(delay);
        } catch (error) {
            delay.disconnect();
            gain.disconnect();
            throw error;
        }
        return {
            delay: delay,
            gain: gain.gain,
            disconnect: function() { source.disconnect(); delay.disconnect(); gain.disconnect(); }
        };
    }
    function monitor(first, second, callback) {
        var node = new owner.AudioWorkletNode(context, 'customtv-audio-handoff', {
            numberOfInputs: 2, channelCount: 1, channelCountMode: 'explicit'
        });
        first.delay.connect(node, 0, 0);
        second.delay.connect(node, 0, 1);
        node.connect(context.destination);
        node.port.onmessage = function(event) { callback(event.data); };
        return function() {
            node.port.close();
            first.delay.disconnect(node, 0, 0);
            second.delay.disconnect(node, 0, 1);
            node.disconnect();
        };
    }
    return { context: context, track: track, monitor: monitor };
};
