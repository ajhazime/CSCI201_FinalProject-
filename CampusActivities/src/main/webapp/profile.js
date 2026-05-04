const sessionUser = JSON.parse(sessionStorage.getItem("user"));

let profilePenaltyInterval = null;

function meApiUrl() {
    return typeof campusFitUrl === "function" ? campusFitUrl("api/users?me=true") : "api/users?me=true";
}

function usersApiBase() {
    return typeof campusFitUrl === "function" ? campusFitUrl("api/users") : "api/users";
}

function clearProfilePenaltyInterval() {
    if (profilePenaltyInterval) {
        clearInterval(profilePenaltyInterval);
        profilePenaltyInterval = null;
    }
}

function formatRemainingMs(ms) {
    if (ms <= 0) {
        return "0s";
    }
    const totalSec = Math.floor(ms / 1000);
    const h = Math.floor(totalSec / 3600);
    const m = Math.floor((totalSec % 3600) / 60);
    const s = totalSec % 60;
    const parts = [];
    if (h > 0) {
        parts.push(h + "h");
    }
    if (h > 0 || m > 0) {
        parts.push(m + "m");
    }
    parts.push(s + "s");
    return parts.join(" ");
}

function formatEndLocal(iso) {
    try {
        const d = new Date(iso);
        return d.toLocaleString(undefined, {
            dateStyle: "medium",
            timeStyle: "short"
        });
    } catch (e) {
        return iso;
    }
}

function applyStatusPanel(u, isOwnProfile) {
    const penalties =
        u.penalties !== undefined && u.penalties !== null ? Number(u.penalties) : 0;
    const until = u.eventRestrictionUntil;
    const endMs = until ? Date.parse(until) : NaN;
    const active =
        isOwnProfile && until && !isNaN(endMs) && endMs > Date.now();

    const titleEl = document.getElementById("statusTitle");
    const descEl = document.getElementById("statusDesc");
    const iconEl = document.getElementById("statusIcon");
    const panel = document.getElementById("statusPanel");
    const timerWrap = document.getElementById("statusTimerWrap");
    const timerEl = document.getElementById("statusTimer");
    const infoRestriction = document.getElementById("infoRestriction");

    clearProfilePenaltyInterval();

    if (!titleEl || !descEl || !panel) {
        return;
    }

    if (!isOwnProfile) {
        panel.classList.remove("status-warning");
        if (iconEl) {
            iconEl.textContent = "✓";
            iconEl.classList.remove("status-warn-icon");
        }
        if (timerWrap) {
            timerWrap.hidden = true;
        }
        if (timerEl) {
            timerEl.textContent = "—";
        }
        if (infoRestriction) {
            infoRestriction.textContent = "—";
        }
        if (penalties > 0) {
            titleEl.textContent = "Penalty points on record";
            descEl.textContent =
                "This account has " +
                penalties +
                " penalty point(s). Active restrictions are private.";
        } else {
            titleEl.textContent = "Good standing";
            descEl.textContent = "No penalty points on record.";
        }
        return;
    }

    if (active) {
        panel.classList.add("status-warning");
        if (iconEl) {
            iconEl.textContent = "!";
            iconEl.classList.add("status-warn-icon");
        }
        titleEl.textContent = "Event restriction active";
        descEl.textContent =
            "You cannot register for or create campus events until this restriction lifts (typically after a no-show).";
        if (timerWrap) {
            timerWrap.hidden = false;
        }
        if 