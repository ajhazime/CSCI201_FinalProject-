(function () {
    function clearClientStorage() {
        try {
            sessionStorage.removeItem("user");
            localStorage.removeItem("user");
        } catch (e) {
            /* ignore */
        }
    }

    /**
     * Full page navigation to /logout (session cleared server-side, then redirected to login).
     * Works even when fetch behaves oddly with redirects/caching.
     */
    window.performLogout = function (ev) {
        if (ev) {
            ev.preventDefault();
        }
        clearClientStorage();
        var url = typeof campusFitUrl === "function" ? campusFitUrl("logout") : "logout";
        window.location.assign(url);
    };

    function wireLogoutLink() {
        var el = document.getElementById("logoutLink");
        if (!el) {
            return;
        }
        el.addEventListener("click", window.performLogout);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", wireLogoutLink);
    } else {
        wireLogoutLink();
    }
})();
