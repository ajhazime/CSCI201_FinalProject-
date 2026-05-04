let user = null;
try {
    const raw = sessionStorage.getItem("user");
    if (raw) {
        user = JSON.parse(raw);
    }
} catch (e) {
    user = null;
}

if (!user) {
    window.location.href =
        typeof campusFitUrl === "function" ? campusFitUrl("login.html") : "login.html";
} else {
    const username = user.username || "User";

    const sidebarUsername = document.getElementById("sidebarUsername");
    const sidebarInitials = document.getElementById("sidebarInitials");

    if (sidebarUsername) {
        sidebarUsername.textContent = username;
    }

    if (sidebarInitials) {
        sidebarInitials.textContent = getInitials(username);
    }
}

const calTitle = document.getElementById("calTitle");
const calGrid = document.getElementById("calGrid");
const calPrev = document.getElementById("calPrev");
const calNext = document.getElementById("calNext");
const calToday = document.getElementById("calToday");
const calAll = document.getElementById("calAll");
const calHint = document.getElementById("calHint");

let allEvents = [];
let calendarMonth = new Date();
calendarMonth.setDate(1);
let selectedDate = null; // "YYYY-MM-DD" or null

const myUserId = user && user.id !== undefined && user.id !== null ? Number(user.id) : -1;

function parseEventDateTime(dateStr, timeStr) {
    if (!dateStr || !timeStr) {
        return null;
    }
    const t = String(timeStr).trim();
    const timePart = t.length === 5 ? t + ":00" : t;
    const d = new Date(String(dateStr).trim() + "T" + timePart);
    return isNaN(d.getTime()) ? null : d;
}

function getEventEndDate(e) {
    if (e.endTime) {
        const end = parseEventDateTime(e.date, e.endTime);
        if (end) {
            return end;
        }
    }
    const start = parseEventDateTime(e.date, e.time);
    if (!start) {
        return null;
    }
    return new Date(start.getTime() + 2 * 60 * 60 * 1000);
}

function getEventProgressState(e) {
    const start = parseEventDateTime(e.date, e.time);
    const end = getEventEndDate(e);
    if (!start || !end) {
        return { key: "unknown", label: "—" };
    }
    const now = new Date();
    if (now < start) {
        return { key: "upcoming", label: "Upcoming" };
    }
    if (now >= end) {
        return { key: "completed", label: "Completed" };
    }
    return { key: "in_progress", label: "In progress" };
}

/** Completed (past end time) events are excluded from the top summary cards only. */
function isEventActiveForSummary(e) {
    return getEventProgressState(e).key !== "completed";
}

function eventsForSummaryStats(events) {
    return (events || []).filter(isEventActiveForSummary);
}

/**
 * Status column: for completed events where the user participated, show Present / Not present.
 */
function getStatusDisplay(e, progress) {
    const joined = !!e.currentUserJoined;
    const host = myUserId >= 0 && Number(e.creatorId) === myUserId;
    const p = e.participantPresent;
    if (progress.key === "completed" && joined && !host) {
        if (p === true) {
            return { label: "Present", cssKey: "present-confirmed" };
        }
        return { label: "Not present", cssKey: "not-present" };
    }
    if (progress.key === "unknown") {
        return { label: progress.label, cssKey: "unknown" };
    }
    return { label: progress.label, cssKey: progress.key.replace(/_/g, "-") };
}

function buildActionColumn(e, progress) {
    const maxParticipants = Number(e.maxParticipants || 0);
    const currentParticipants = Number(e.currentParticipants || 0);
    const openSpots = Math.max(maxParticipants - currentParticipants, 0);
    const host = myUserId >= 0 && Number(e.creatorId) === myUserId;
    const joined = !!e.currentUserJoined;

    if (host) {
        if (progress.key === "in_progress") {
            return (
                '<div class="action-host-stack">' +
                '<span class="action-host">Hosting</span>' +
                '<button type="button" class="btn-attendance" data-attendance-event-id="' +
                Number(e.id) +
                '">Attendance</button>' +
                "</div>"
            );
        }
        return '<span class="action-host">Hosting</span>';
    }

    if (progress.key === "completed") {
        if (joined) {
            if (e.participantPresent === true) {
                return '<span class="action-muted">Attended</span>';
            }
            return '<span class="action-not-present">Not present</span>';
        }
        return '<span class="action-muted">—</span>';
    }

    if (progress.key === "in_progress") {
        if (joined) {
            return '<span class="action-live">In session</span>';
        }
        return '<span class="action-muted">Started</span>';
    }

    if (progress.key === "unknown") {
        return '<span class="action-muted">—</span>';
    }

    if (joined) {
        return (
            '<div class="action-stack">' +
            '<span class="btn-joined-label">Joined</span>' +
            '<button type="button" class="btn-link-leave" data-leave-id="' +
            Number(e.id) +
            '">Leave</button>' +
            "</div>"
        );
    }

    if (openSpots <= 0) {
        return '<span class="action-muted">Full</span>';
    }

    return '<button type="button" class="btn-join" data-join-id="' + Number(e.id) + '">Join</button>';
}

function bindActionButtons(tbody) {
    if (!tbody) {
        return;
    }
    tbody.querySelectorAll("[data-join-id]").forEach(function(btn) {
        btn.addEventListener("click", function() {
            const id = Number(btn.getAttribute("data-join-id"));
            joinEventById(id, btn);
        });
    });
    tbody.querySelectorAll("[data-leave-id]").forEach(function(btn) {
        btn.addEventListener("click", function() {
            const id = Number(btn.getAttribute("data-leave-id"));
            leaveEventById(id, btn);
        });
    });
    tbody.querySelectorAll("[data-attendance-event-id]").forEach(function(btn) {
        btn.addEventListener("click", function() {
            const id = Number(btn.getAttribute("data-attendance-event-id"));
            openAttendanceModal(id);
        });
    });
}

let attendanceModalEventId = null;

function attendanceGetUrl(eventId) {
    var base =
        typeof campusFitUrl === "function" ? campusFitUrl("eventAttendance") : "eventAttendance";
    return base + "?eventId=" + encodeURIComponent(String(eventId));
}

function markAttendancePostUrl() {
    return typeof campusFitUrl === "function" ? campusFitUrl("markAttendance") : "markAttendance";
}

function openAttendanceModal(eventId) {
    var overlay = document.getElementById("attendanceOverlay");
    var listEl = document.getElementById("attendanceList");
    var errEl = document.getElementById("attendanceError");
    if (!overlay || !listEl) {
        return;
    }
    if (errEl) {
        errEl.textContent = "";
    }
    listEl.innerHTML = '<div class="modal-loading">Loading roster…</div>';
    overlay.classList.add("is-open");
    overlay.setAttribute("aria-hidden", "false");
    attendanceModalEventId = eventId;

    fetch(attendanceGetUrl(eventId), { credentials: "same-origin" })
        .then(function(res) {
            return res.json().then(function(data) {
                return { ok: res.ok, data: data };
            });
        })
        .then(function(result) {
            if (!result.data || !result.data.success) {
                listEl.innerHTML =
                    '<div class="modal-empty">' +
                    escapeHtml((result.data && result.data.message) || "Could not load attendance.") +
                    "</div>";
                return;
            }
            var parts = result.data.participants || [];
            if (parts.length === 0) {
                listEl.innerHTML =
                    '<div class="modal-empty">No participants have joined yet (besides you as host).</div>';
                return;
            }
            listEl.innerHTML = "";
            parts.forEach(function(p) {
                var uid = Number(p.userId);
                var checked = p.present === true;
                var row = document.createElement("label");
                row.className = "attendance-row";
                row.innerHTML =
                    '<input type="checkbox" data-att-user="' +
                    uid +
                    '" ' +
                    (checked ? "checked " : "") +
                    '/> <span class="attendance-name">' +
                    escapeHtml(p.username || "User") +
                    "</span> <span class=\"attendance-hint\">Present</span>";
                listEl.appendChild(row);
            });
        })
        .catch(function() {
            listEl.innerHTML = '<div class="modal-empty">Could not load attendance.</div>';
        });
}

function closeAttendanceModal() {
    var overlay = document.getElementById("attendanceOverlay");
    if (overlay) {
        overlay.classList.remove("is-open");
        overlay.setAttribute("aria-hidden", "true");
    }
    attendanceModalEventId = null;
}

function saveAttendanceFromModal() {
    var listEl = document.getElementById("attendanceList");
    var errEl = document.getElementById("attendanceError");
    var saveBtn = document.getElementById("attendanceSave");
    if (!attendanceModalEventId || !listEl) {
        return;
    }
    var boxes = listEl.querySelectorAll("input[data-att-user]");
    if (boxes.length === 0) {
        closeAttendanceModal();
        return;
    }
    var marks = [];
    boxes.forEach(function(cb) {
        marks.push({
            userId: Number(cb.getAttribute("data-att-user")),
            present: cb.checked
        });
    });
    if (errEl) {
        errEl.textContent = "";
    }
    if (saveBtn) {
        saveBtn.disabled = true;
    }

    fetch(markAttendancePostUrl(), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "same-origin",
        body: JSON.stringify({ eventId: attendanceModalEventId, marks: marks })
    })
        .then(function(res) {
            return res.json().then(function(data) {
                return { ok: res.ok, data: data };
            });
        })
        .then(function(result) {
            if (result.data && result.data.success) {
                closeAttendanceModal();
                fetchAndRender();
            } else if (errEl) {
                errEl.textContent =
                    (result.data && result.data.message) || "Could not save attendance.";
            }
        })
        .catch(function() {
            if (errEl) {
                errEl.textContent = "Could not save attendance.";
            }
        })
        .then(function() {
            if (saveBtn) {
                saveBtn.disabled = false;
            }
        });
}

function joinEventUrl() {
    return typeof campusFitUrl === "function" ? campusFitUrl("joinEvent") : "joinEvent";
}

function leaveEventUrl() {
    return typeof campusFitUrl === "function" ? campusFitUrl("leaveEvent") : "leaveEvent";
}

function joinEventById(eventId, btn) {
    btn.disabled = true;
    const params = new URLSearchParams({ eventId: String(eventId) });
    fetch(joinEventUrl(), { method: "POST", body: params, credentials: "same-origin" })
        .then(function(res) {
            return res.json().then(function(data) {
                return { ok: res.ok, data: data };
            });
        })
        .then(function(result) {
            if (result.data && result.data.success) {
                fetchAndRender();
            } else {
                var msg = (result.data && result.data.message) || "Could not join this event.";
                alert(msg);
                btn.disabled = false;
            }
        })
        .catch(function() {
            alert("Could not join this event.");
            btn.disabled = false;
        });
}

function leaveEventById(eventId, btn) {
    btn.disabled = true;
    const params = new URLSearchParams({ eventId: String(eventId) });
    fetch(leaveEventUrl(), { method: "POST", body: params, credentials: "same-origin" })
        .then(function(res) {
            return res.json().then(function(data) {
                return { ok: res.ok, data: data };
            });
        })
        .then(function(result) {
            if (result.data && result.data.success) {
                fetchAndRender();
            } else {
                var msg = (result.data && result.data.message) || "Could not leave this event.";
                alert(msg);
                btn.disabled = false;
            }
        })
        .catch(function() {
            alert("Could not leave this event.");
            btn.disabled = false;
        });
}

function toDateInputValue(dateObj) {
    const y = dateObj.getFullYear();
    const m = String(dateObj.getMonth() + 1).padStart(2, "0");
    const d = String(dateObj.getDate()).padStart(2, "0");
    return `${y}-${m}-${d}`;
}

function groupEventsByDate(events) {
    const map = new Map(); // date -> events[]
    (events || []).forEach((e) => {
        const d = String(e.date || "").trim();
        if (!d) return;
        if (!map.has(d)) map.set(d, []);
        map.get(d).push(e);
    });
    for (const [d, list] of map.entries()) {
        list.sort((a, b) => String(a.time || "").localeCompare(String(b.time || "")));
        map.set(d, list);
    }
    return map;
}

function setSelectedDate(newDate) {
    selectedDate = newDate;
    if (calHint) {
        calHint.textContent = selectedDate
            ? `Showing events for ${selectedDate}. Click “All events” to clear the filter.`
            : "Click a day to filter the table below.";
    }
    if (calAll) {
        calAll.classList.toggle("primary", !selectedDate);
    }
    renderCalendar();
    renderTable();
}

function renderCalendar() {
    if (!calGrid || !calTitle) return;
    const monthLabel = calendarMonth.toLocaleDateString(undefi