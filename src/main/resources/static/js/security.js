(function () {
    function readCookie(name) {
        return document.cookie.split(';').map(function (part) { return part.trim(); })
            .filter(function (part) { return part.indexOf(name + '=') === 0; })
            .map(function (part) { return decodeURIComponent(part.substring(name.length + 1)); })[0] || '';
    }

    function isSameOrigin(input) {
        try {
            var url = typeof input === 'string' ? input : (input && input.url) || '';
            if (!url || url.charAt(0) === '/') return true;
            return new URL(url, window.location.href).origin === window.location.origin;
        } catch (e) {
            return false;
        }
    }

    function methodOf(input, init) {
        return ((init && init.method) || (input && input.method) || 'GET').toUpperCase();
    }

    var unsafeMethods = { POST: true, PUT: true, DELETE: true, PATCH: true };
    var originalFetch = window.fetch;
    if (!originalFetch) return;

    window.fetch = function (input, init) {
        var method = methodOf(input, init);
        if (unsafeMethods[method] && isSameOrigin(input)) {
            var token = readCookie('XSRF-TOKEN');
            if (token) {
                init = init || {};
                var headers = new Headers(init.headers || (input && input.headers) || {});
                if (!headers.has('X-XSRF-TOKEN')) {
                    headers.set('X-XSRF-TOKEN', token);
                }
                init.headers = headers;
            }
        }
        return originalFetch(input, init);
    };
})();
