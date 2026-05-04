const user = JSON.parse(sessionStorage.getItem("user"));

let dashboardPenaltyInterval = null;

function meApiUrl() {
    return typeof campusFitUrl === "function" ? campusFitUrl("api/users?me=true") : "api/users?me=true";
}

function clearDashboardPenaltyInterval() {
    if (dashboardPenaltyInterval) {
        clearInterval(dashboardPenaltyInterval);
        dashboardPenaltyInterval = null;
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

function applyPenaltyDashboard(u) {
    const penalties =
        u.penalties !== undefined && u.penalties !== null ? Number(u.penalties) : 0;
    const until = u.eventRestrictionUntil;
    const endMs = until ? Date.parse(until) : NaN;
    const active = until && !isNaN(endMs) && endMs >