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
        if (infoRestriction) {
            infoRestriction.textContent = "Active until " + formatEndLocal(until);
        }

        const end = endMs;
        function tick() {
            const ms = end - Date.now();
            if (timerEl) {
                timerEl.textContent = formatRemainingMs(ms);
            }
            if (ms <= 0) {
                clearProfilePenaltyInterval();
                refreshOwnProfileFromServer();
            }
        }
        tick();
        profilePenaltyInterval = setInterval(tick, 1000);
    } else {
        panel.classList.remove("status-warning");
        if (iconEl) {
            iconEl.textContent = "✓";
            iconEl.classList.remove("status-warn-icon");
        }
        if (timerWrap) {
            timerWrap.hidden = true;
        }
        if (penalties > 0) {
            titleEl.textContent = "Account in good standing";
            descEl.textContent =
                "You have " +
                penalties +
                " penalty point(s) on record, but no active event restriction.";
        } else {
            titleEl.textContent = "Account in good standing";
            descEl.textContent = "No active penalties on record.";
        }
        if (infoRestriction) {
            infoRestriction.textContent = "None";
        }
    }
}

function refreshOwnProfileFromServer() {
    fetch(meApiUrl(), { credentials: "same-origin" })
        .then(function (res) {
            return res.json();
        })
        .then(function (u) {
            if (u && u.id !== undefined) {
                populateProfile(u, true);
                applyStatusPanel(u, true);
                try {
                    sessionStorage.setItem("user", JSON.stringify(u));
                } catch (e) {
                    /* ignore */
                }
            }
        })
        .catch(function () {
            /* ignore */
        });
}

if (!sessionUser) {
    window.location.href = "login.html";
} else {
    document.getElementById("sidebarUsername").textContent = sessionUser.username || "User";
    document.getElementById("sidebarInitials").textContent = getInitials(sessionUser.username);
}

const params = new URLSearchParams(window.location.search);
const viewingUserId = params.get("userId");

if (!sessionUser) {
    /* redirecting */
} else if (viewingUserId) {
    fetch(usersApiBase() + "?id=" + encodeURIComponent(viewingUserId), {
        credentials: "same-origin"
    })
        .then(function (response) {
            if (response.status === 401) {
                window.location.href = "login.html";
                return;
            }
            if (response.status === 404) {
                document.getElementById("profileUsername").textContent = "User not found";
                return;
            }
            return response.json();
        })
        .then(function (data) {
            if (data) {
                populateProfile(data, false);
                applyStatusPanel(data, false);
            }
        })
        .catch(function () {
            document.getElementById("profileUsername").textContent = "Error loading profile";
        });
} else if (sessionUser) {
    populateProfile(sessionUser, false);
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
                populateProfile(u, true);
                applyStatusPanel(u, true);
                try {
                    sessionStorage.setItem("user", JSON.stringify(u));
                } catch (e) {
                    /* ignore */
                }
            }
        })
        .catch(function () {
            applyStatusPanel(sessionUser, true);
        });
}

function populateProfile(user, fromServer) {
    const username = user.username || "User";
    const email = user.email || "No email available";
    const skillLevel = user.skill_level || user.skillLevel || "Student";
    const penalties =
        user.penalties !== undefined && user.penalties !== null ? Number(user.penalties) : 0;
    const interests = parseInterests(user.interests);

    document.getElementById("profileUsername").textContent = username;
    document.getElementById("infoUsername").textContent = username;
    document.getElementById("profileEmail").textContent = email;
    document.getElementById("infoEmail").textContent = email;
    document.getElementById("profileSkillLevel").textContent = skillLevel;
    document.getElementById("infoSkillLevel").textContent = skillLevel;
    document.getElementById("infoPenalties").textContent = penalties;
    document.getElementById("profileInitials").textContent = getInitials(username);

    renderInterests(interests);
}

function parseInterests(interests) {
    if (!interests) {
        return [];
    }

    if (Array.isArray(interests)) {
        return interests;
    }

    return String(interests)
        .split(",")
        .map(function (interest) {
            return interest.trim();
        })
        .filter(function (interest) {
            return interest.length > 0;
        });
}

function renderInterests(interests) {
    const interestTags = document.getElementById("interestTags");

    if (!interests || interests.length === 0) {
        interestTags.innerHTML = `<span class="empty-text">No interests added yet.</span>`;
        return;
    }

    interestTags.innerHTML = interests
        .map(function (interest) {
            return `<span class="interest-tag">${escapeHtml(interest)}</span>`;
        })
        .join("");
}

function getInitials(name) {
    const parts = String(name).trim().split(/\s+/);

    if (parts.length === 1) {
        return parts[0].substring(0, 2).toUpperCase();
    }

    return (parts[0][0] + parts[1][0]).toUpperCase();
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
