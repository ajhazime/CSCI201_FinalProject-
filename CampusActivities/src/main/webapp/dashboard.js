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
    const active = until && !isNaN(endMs) && endMs > Date.now();

    const statVal = document.getElementById("statPenaltyValue");
    const statSub = document.getElementById("statPenaltySub");
    const card = document.getElementById("statPenaltyCard");
    const mini = document.getElementById("profilePenaltyMini");
    const recentBody = document.getElementById("recentPenaltiesBody");

    if (statVal) {
        statVal.textContent = String(penalties);
    }
    if (mini) {
        mini.textContent = String(penalties);
    }

    if (statSub && card) {
        if (active) {
            statSub.textContent = "Active restriction — cannot join or create events";
            card.classList.add("stat-card-warn");
        } else {
            statSub.textContent =
                penalties > 0 ? "No active timer (recorded points only)" : "All clear";
            card.classList.remove("stat-card-warn");
        }
    }

    clearDashboardPenaltyInterval();
    if (!recentBody) {
        return;
    }

    if (active) {
        recentBody.innerHTML =
            '<div class="penalty-active">' +
            '<div class="penalty-active-title">No-show event restriction</div>' +
            '<p class="penalty-active-desc">Joining and creating events is blocked until this timer ends.</p>' +
            '<div class="penalty-timer-label">Time remaining</div>' +
            '<div class="penalty-timer" id="dashboardPenaltyTimer">—</div>' +
            "</div>";

        const end = endMs;
        function tick() {
            const el = document.getElementById("dashboardPenaltyTimer");
            const ms = end - Date.now();
            if (el) {
                el.textContent = formatRemainingMs(ms);
            }
            if (ms <= 0) {
                clearDashboardPenaltyInterval();
                loadDashboardPenaltyInfo();
            }
        }
        tick();
        dashboardPenaltyInterval = setInterval(tick, 1000);
    } else if (penalties > 0) {
        recentBody.innerHTML =
            '<div class="penalty-record-only">' +
            '<div class="penalty-record-title">Penalty points on file</div>' +
            "<p>" +
            penalties +
            " total — no active event restriction right now.</p>" +
            "</div>";
    } else {
        recentBody.innerHTML =
            '<div class="penalty-empty-state">' +
            '<div class="penalty-check">✓</div>' +
            "<div>No penalties on record</div>" +
            "</div>";
    }
}

function loadDashboardPenaltyInfo() {
    fetch(meApiUrl(), { credentials: "same-origin" })
        .then(function (res) {
            if (res.status === 401) {
                window.location.href = "login.html";
                return null;
            }
            return res.json();
        })
        .then(function (u) {
            if (u && u.id !== undefined) {
                applyPenaltyDashboard(u);
                try {
                    sessionStorage.setItem("user", JSON.stringify(u));
                } catch (e) {
                    /* ignore */
                }
            }
        })
        .catch(function () {
            const statVal = document.getElementById("statPenaltyValue");
            const statSub = document.getElementById("statPenaltySub");
            const recentBody = document.getElementById("recentPenaltiesBody");
            if (statVal) {
                statVal.textContent = "—";
            }
            if (statSub) {
                statSub.textContent = "Could not load";
            }
            if (recentBody) {
                recentBody.innerHTML =
                    '<div class="penalty-loading error">Could not load penalty info.</div>';
            }
        });
}

if (!user) {
    window.location.href = "login.html";
} else {
    const username = user.username || "User";

    document.getElementById("username").textContent = username;

    const sidebarUsername = document.getElementById("sidebarUsername");
    if (sidebarUsername) {
        sidebarUsername.textContent = username;
    }

    const profileNameLarge = document.getElementById("profileNameLarge");
    if (profileNameLarge) {
        profileNameLarge.textContent = username;
    }

    const initials = getInitials(username);

    const sidebarInitials = document.getElementById("sidebarInitials");
    if (sidebarInitials) {
        sidebarInitials.textContent = initials;
    }

    const profileInitials = document.getElementById("profileInitials");
    if (profileInitials) {
        profileInitials.textContent = initials;
    }

    loadDashboardPenaltyInfo();
    loadUpcomingEvents();
}

function upcomingEventsUrl() {
    return typeof campusFitUrl === "function"
        ? campusFitUrl("upcomingEvents?limit=8")
        : "upcomingEvents?limit=8";
}

function escapeHtmlDash(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function loadUpcomingEvents() {
    const body = document.getElementById("upcomingEventsBody");
    if (!body) {
        return;
    }

    fetch(upcomingEventsUrl(), { credentials: "same-origin" })
        .then(function (res) {
            if (res.status === 401) {
                window.location.href = "login.html";
                return null;
            }
            return res.json();
        })
        .then(function (events) {
            if (!body) {
                return;
            }
            if (!events || !Array.isArray(events) || events.length === 0) {
                body.innerHTML =
                    '<div class="empty-state empty-state--compact">' +
                    '<div class="empty-icon">📅</div>' +
                    "<h4>No upcoming events</h4>" +
                    "<p>Browse activities or create an event to see it here.</p>" +
                    "</div>";
                return;
            }

            body.innerHTML = events
                .map(function (ev) {
                    const when =
                        escapeHtmlDash(ev.date || "") + " · " + escapeHtmlDash(ev.time || "");
                    return (
                        '<div class="upcoming-row">' +
                        '<div class="upcoming-row-main">' +
                        '<div class="upcoming-title">' +
                        escapeHtmlDash(ev.activityType || "Event") +
                        "</div>" +
                        '<div class="upcoming-meta">' +
                        escapeHtmlDash(ev.location || "") +
                        "</div>" +
                        '<div class="upcoming-when">' +
                        when +
                        "</div>" +
                        "</div>" +
                        "</div>"
                    );
                })
                .join("");
        })
        .catch(function () {
            body.innerHTML =
                '<div class="upcoming-error">Could not load upcoming events.</div>';
        });
}

function getInitials(name) {
    const parts = String(name).trim().split(/\s+/);

    if (parts.length === 1) {
        return parts[0].substring(0, 2).toUpperCase();
    }

    return (parts[0][0] + parts[1][0]).toUpperCase();
}
