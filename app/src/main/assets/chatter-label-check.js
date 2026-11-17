// Chatter Labels check contract, with archive requests delegated to the Android app.
class ChatterCheckError extends Error {
    constructor(reason, status) {
        super(reason);
        this.reason = reason;
        this.status = status;
    }
}

async function runChatterCheck(input, script) {
    const port = await android.getNamedPort('chatterCheck');
    const pending = new Map();
    let requestId = 0;
    port.onmessage = ({ data }) => {
        const response = JSON.parse(data);
        const resolve = pending.get(response.id);
        pending.delete(response.id);
        resolve?.(response);
    };
    const request = payload => new Promise(resolve => {
        const id = ++requestId;
        pending.set(id, resolve);
        port.postMessage(JSON.stringify({ ...payload, id }));
    });
    const search = (query, limit, offset) => request({ kind: 'archive', query, limit, offset });
    const deadline = Date.now() + input.timeoutMillis;
    const signal = Object.freeze({
        get aborted() { return Date.now() >= deadline; },
        throwIfAborted() {
            if (this.aborted) throw new ChatterCheckError('timeout');
        },
    });
    let needsLogin = false;
    const requireLogin = () => {
        needsLogin = true;
        throw new ChatterCheckError('twitch-login');
    };
    const twitch = input.twitchAvailable
        ? createChatterCheckTwitch(request, signal, requireLogin)
        : new Proxy({}, { get: requireLogin });
    const bestLogs = Object.freeze({
        async countMessages(keywords, options) {
            return (await bestLogs.searchMessages(keywords, options)).count;
        },
        searchMessages(keywords, options = {}) {
            if (!input.channelId) throw new ChatterCheckError('channel-required');
            return searchBestLogs(input, search, signal, keywords, options);
        },
    });

    try {
        const value = await script(input.user, input.channel, bestLogs, twitch, signal);
        return JSON.stringify(needsLogin ? { kind: 'twitch-login' } : { kind: 'result', value: normalizeResult(value) });
    } catch (error) {
        if (needsLogin) return JSON.stringify({ kind: 'twitch-login' });
        if (error instanceof ChatterCheckError) {
            return JSON.stringify({ kind: 'check-error', reason: error.reason, status: error.status });
        }
        return JSON.stringify({ kind: 'error', text: String(error?.message ?? error) });
    }
}

function createChatterCheckTwitch(request, signal, requireLogin) {
    const twitch = {
        async gql(query, variables = {}, { authenticated = false } = {}) {
            signal.throwIfAborted();
            const result = await request({ kind: 'twitch-gql', query, variables, authenticated: authenticated === true });
            signal.throwIfAborted();
            if (result.error === 'twitch-login') requireLogin();
            if (result.error) throw new ChatterCheckError(result.error, result.status);
            if (typeof result.message === 'string') throw new Error(result.message);
            return result.data;
        },
        async getUser(id, fields) {
            if (typeof id !== 'string' || !id.trim() || typeof fields !== 'string' || !fields.trim()) {
                throw new ChatterCheckError('twitch-user-input');
            }
            const data = await twitch.gql(`query($id: ID!) {
                user(id: $id, lookupType: ALL) { ${fields} }
            }`, { id });
            if (!data?.user) throw new ChatterCheckError('twitch-user-missing');
            return data.user;
        },
    };
    return Object.freeze(twitch);
}

function normalizeResult(value) {
    if (value == null) return null;
    if (['boolean', 'string', 'number'].includes(typeof value)) return value;
    if (typeof value !== 'object' || Array.isArray(value) || typeof value.text !== 'string') {
        throw new ChatterCheckError('result-type');
    }
    if (!Array.isArray(value.messages)) throw new ChatterCheckError('message-array');
    return {
        text: value.text,
        messages: value.messages.map(message => {
            if (!message || typeof message.text !== 'string' ||
                (message.timestamp !== undefined && typeof message.timestamp !== 'string')) {
                throw new ChatterCheckError('message-type');
            }
            return {
                text: message.text,
                timestamp: message.timestamp ?? '',
                id: typeof message.id === 'string' ? message.id : '',
                channelId: typeof message.channelId === 'string' ? message.channelId : '',
            };
        }),
    };
}

function bestLogsKeywords(keywords) {
    if (!Array.isArray(keywords) || keywords.some(word => typeof word !== 'string')) {
        throw new ChatterCheckError('keywords-type');
    }
    const terms = new Set(keywords.map(word => word.trim().toLowerCase()).filter(Boolean));
    if (!terms.size) throw new ChatterCheckError('keywords-empty');
    const patterns = [];
    const queries = new Set();
    for (const term of terms) {
        if (!/^[\p{L}\p{M}\p{N}_*]+$/u.test(term)) {
            throw new ChatterCheckError('keyword-format');
        }
        const parts = term.normalize('NFC').split('*');
        if (!parts.some(Boolean)) throw new ChatterCheckError('keyword-format');
        patterns.push(new RegExp('^' + parts.join('.*') + '$', 'u'));
        queries.add(parts.reduce((longest, part) => part.length > longest.length ? part : longest, ''));
    }
    return { patterns, queries };
}

async function searchBestLogs(input, search, signal, keywords, options) {
    signal.throwIfAborted();
    if (!options || typeof options !== 'object' || Array.isArray(options) ||
        Object.keys(options).some(key => !['filter', 'limit', 'maxMessages'].includes(key))) {
        throw new ChatterCheckError('options');
    }
    const { filter, limit = Infinity, maxMessages = Infinity } = options;
    if (filter !== undefined && typeof filter !== 'function') throw new ChatterCheckError('filter-type');
    if (limit !== Infinity && (!Number.isSafeInteger(limit) || limit < 1)) {
        throw new ChatterCheckError('limits');
    }
    if (maxMessages !== Infinity && (!Number.isSafeInteger(maxMessages) || maxMessages < 1)) {
        throw new ChatterCheckError('limits');
    }
    const { patterns, queries } = bestLogsKeywords(keywords);
    const matches = [];
    const inspected = new Set();
    const result = () => ({ count: matches.length, checked: inspected.size, messages: matches });

    for (const query of queries) {
        let offset = 0;
        let previousPage = null;
        for (;;) {
            signal.throwIfAborted();
            if (inspected.size >= maxMessages) return result();
            const pageSize = Math.min(500, maxMessages - inspected.size);
            const response = await search(query, pageSize, offset);
            const batch = bestLogsBatch(response, pageSize);
            if (!batch.length) break;
            const fingerprint = JSON.stringify(batch.map(message => [message?.id, message?.timestamp]));
            if (fingerprint === previousPage) throw new ChatterCheckError('pagination');
            previousPage = fingerprint;
            for (const message of batch) {
                if (!isChatterMessage(message, input) || inspected.has(message.id)) continue;
                inspected.add(message.id);
                const words = message.text.toLowerCase().normalize('NFC').match(/[\p{L}\p{M}\p{N}_]+/gu) ?? [];
                const matchedWords = [...new Set(words.filter(word => patterns.some(pattern => pattern.test(word))))];
                if (!matchedWords.length) continue;
                if (filter) {
                    const accepted = await filter(message, matchedWords);
                    signal.throwIfAborted();
                    if (typeof accepted !== 'boolean') throw new ChatterCheckError('filter-result');
                    if (!accepted) continue;
                }
                matches.push({ ...message, channelId: input.channelId });
                if (matches.length >= limit) return result();
            }
            if (batch.length < pageSize) break;
            offset += batch.length;
        }
    }
    return result();
}

function bestLogsBatch(response, pageSize) {
    if (response.error) throw new ChatterCheckError(response.error);
    if (response.status === 404) {
        if (response.body.trim() === 'Not found') return [];
        throw new ChatterCheckError('no-archive');
    }
    if (response.status === 429) throw new ChatterCheckError('rate-limit');
    if (response.status < 200 || response.status >= 300) throw new ChatterCheckError('archive-http', response.status);
    const data = JSON.parse(response.body);
    if (!Array.isArray(data?.messages)) throw new ChatterCheckError('archive-invalid');
    return data.messages.slice(0, pageSize);
}

function isChatterMessage(message, input) {
    const tags = message?.tags;
    if (tags?.['target-user-id'] || tags?.['target-msg-id']) return false;
    if (!message?.id || typeof message.text !== 'string' || !tags?.['user-id'] || !tags?.['room-id']) {
        throw new ChatterCheckError('archive-invalid');
    }
    return String(tags['user-id']) === input.user.id && String(tags['room-id']) === input.channelId;
}
