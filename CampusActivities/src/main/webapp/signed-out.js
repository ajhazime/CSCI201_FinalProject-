/** Strip ?signedOut=1 and drop any cached client "user" blob after server logout. */
(function () {
    try {
        var params = new URLSearchParams(window.location.search);
        if (params.has("signedOut")) {
            sessionStorage.removeItem("user");
            localStorage.removeItem("user");
            params.delete("signedOut");
            var qs = params.toString();
            var url = window.location.pathname + (qs ? "?" + qs : "");
            window.history.replaceState({}, document.title, url);
        }
    } catch (e) {
        /* ignore */
    }
})();
