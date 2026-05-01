(function () {
    function clearClientStorage() {
        try {
            sessionStorage.removeItem("user");
            localStorage.removeItem("user");
        } catch (e) {
            /* ignore */
        }
    }

    function redirectToLogin() {
        clearClientStorage();
        var target =
            typeof campusFitUrl === "function" ? campusFitUrl("login.html") : "login.html";
        window.location.replace(target);
    }

    /** Called from sidebar "Sign out" links. Works with any servlet context path. */
    window.performLogout = function (ev) {
        if (ev) {
            ev.preventDefault();
        }
        var url = typeof campusFitUrl === "function" ? campusFitUrl("logout") : "logout";
        fetch(url, {
            method: "GET",
            credentials: "same-origin",
            cache: "no-store"
        })
            .catch(function () {
                /* still clear UI + client state */
            })
            .finally(redirectToLogin);
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
