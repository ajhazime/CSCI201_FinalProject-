(function () {
    function campusFitBase() {
        var pathname = window.location.pathname || "/";
        if (pathname === "/" || pathname === "") {
            return "/";
        }
        if (pathname.endsWith("/")) {
            return pathname;
        }
        var lastSlash = pathname.lastIndexOf("/");
        var lastSegment = pathname.slice(lastSlash + 1);
        if (lastSegment.indexOf(".") === -1) {
            return pathname + "/";
        }
        return pathname.slice(0, lastSlash + 1);
    }

    /**
     * Resolve a URL path relative to this webapp (the directory that serves *.html).
     * Examples: 'logout', 'login', 'login.html', 'api/users?limit=8'
     */
    window.campusFitUrl = function (path) {
        var rel = String(path || "").replace(/^\/+/, "");
        var resolved = new URL(rel, window.location.origin + campusFitBase());
        return resolved.pathname + resolved.search + resolved.hash;
    };
})();
