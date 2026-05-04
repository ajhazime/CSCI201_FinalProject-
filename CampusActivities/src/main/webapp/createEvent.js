const activityOptions = [
    "Basketball",
    "Swimming",
    "Weightlifting",
    "Yoga",
    "Pilates",
    "Soccer",
    "Volleyball",
    "Running"
];

const locationOptions = [
    "Lyon Center",
    "USC Village Fitness Center",
    "Uytengsu Aquatics Center",
    "HSC Fitness Center",
    "PED South Gym"
];

const user = JSON.parse(sessionStorage.getItem("user"));
if (!user) {
    window.location.href = "login.html";
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

const activitySelect = document.getElementById("activityType");
const locationSelect = document.getElementById("location");
const dateInput = document.getElementById("date");
const startTimeInput = document.getElementById("startTime");
const endTimeInput = document.getElementById("endTime");
const capacityInput = document.getElementById("maxParticipants");
const calendarTitle = document.getElementById("calendarTitle");
const calendarGrid = document.getElementById("calendarGrid");
const calendarPrev = document.getElementById("calendarPrev");
const calendarNext = document.getElementById("calendarNext");
let calendarMonth = new Date();
calendarMonth.setDate(1);

const inviteSearch = document.getElementById("inviteSearch");
const inviteResults = document.getElementById("inviteResults");
const inviteSuggestions = document.getElementById("inviteSuggestions");
const inviteSelected = document.getElementById("inviteSelected");
const inviteeIdsInput = document.getElementById("inviteeIds");
const inviteStatus = document.getElementById("inviteStatus");
const selectedInvitees = new Map(); // id -> user

function getInitials(name) {
    const parts = String(name).trim().split(/\s+/);
    if (parts.length === 1) {
        return parts[0].substring(0, 2).toUpperCase();
    }
    return (parts[0][0] + parts[1][0]).toUpperCase();
}

function renderInviteSelected() {
    if (!inviteSelected || !inviteeIdsInput) return;
    inviteSelected.innerHTML = "";

    const ids = Array.from(selectedInvitees.keys());
    inviteeIdsInput.value = ids.join(",");

    if (ids.length === 0) {
        inviteSelected.innerHTML = `<div class="small-note">No invitees selected.</div>`;
        return;
    }

    ids.forEach((id) => {
        const u = selectedInvitees.get(id);
        const row = document.createElement("div");
        row.className = "invite-row";
        row.innerHTML = `
            <div>
                <strong>${escapeHtml(u.username || "User")}</strong>
                <small>${escapeHtml(u.email || "")}</small>
            </div>
        `;
        const btn = document.createElement("button");
        btn.type = "button";
        btn.textContent = "Remove";
        btn.addEventListener("click", () => {
            selectedInvitees.delete(id);
            renderInviteSelected();
        });
        row.appendChild(btn);
        inviteSelected.appendChild(row);
    });
}

function renderInviteResults(users) {
    if (!inviteResults) return;
    inviteResults.innerHTML = "";

    if (!users || users.length === 0) {
        inviteResults.innerHTML = `<div class="small-note">No users found.</div>`;
        return;
    }

    users.forEach((u) => {
        const id = Number(u.id);
        const row = document.createElement("div");
        row.className = "invite-row";
        row.innerHTML = `
            <div>
                <strong>${escapeHtml(u.username || "User")}</strong>
                <small>${escapeHtml(u.email || "")}</small>
            </div>
        `;

        const btn = document.createElement("button");
        btn.type = "button";
        if (selectedInvitees.has(id)) {
            btn.textContent = "Selected";
            btn.className = "primary";
            btn.disabled = true;
        } else {
            btn.textContent = "Invite";
            btn.className = "primary";
            btn.addEventListener("click", () => {
                selectedInvitees.set(id, u);
                renderInviteSelected();
                // re-render to reflect "Selected"
                const current = Array.from(inviteResults.querySelectorAll(".invite-row"));
                if (current.length > 0) {
                    renderInviteResults(users);
                }
            });
        }
        row.appendChild(btn);
        inviteResults.appendChild(row);
    });
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function populateSelect(select, options) {
    select.innerHTML = "";
    options.forEach((option) => {
        const el = document.createElement("option");
        el.value = option;
        el.textContent = option;
        select.appendChild(el);
    });
}

function renderInviteSuggestions(users) {
    if (!inviteSuggestions) return;
    inviteSuggestions.innerHTML = "";
    if (!users || users.length === 0) {
        inviteSuggestions.innerHTML = `<div class="small-note">No suggestions yet. Try searching below.</div>`;
        return;
    }

    users.forEach((u) => {
        const id = Number(u.id);
        const row = document.createElement("div");
        row.className = "invite-row";
        row.innerHTML = `
            <div>
                <strong>${escapeHtml(u.username || "User")}</strong>
                <small>${escapeHtml(u.email || "")}</small>
            </div>
        `;

        const btn = document.createElement("button");
        btn.type = "button";
        if (selectedInvitees.has(id)) {
            btn.textContent = "Selected";
            btn.className = "primary";
            btn.disabled = true;
        } else {
            btn.textContent = "Invite";
            btn.className = "primary";
            btn.addEventListener("click", () => {
                selectedInvitees.set(id, u);
                renderInviteSelected();
                renderInviteSuggestions(users);
            });
        }
        row.appendChild(btn);
        inviteSuggestions.appendChild(row);
    });
}

async function loadInviteSuggestions() {
    if (!inviteSuggestions) return;
    inviteSuggestions.innerHTML = `<div class="small-note">Loading suggestions...</div>`;
    try {
        const res = await fetch(campusFitUrl("api/users?limit=8"));
        if (res.status === 401) {
            window.location.href = "login.html";
            return;
        }
        if (!res.ok) {
            inviteSuggestions.innerHTML = `<div class="small-note">Couldn’t load suggestions (server error).</div>`;
            return;
        }
        const users = await res.json();
        renderInviteSuggestions(users);
    } catch (e) {
        inviteSuggestions.innerHTML = `<div class="small-note">Couldn’t load suggestions.</div>`;
    }
}

let inviteSearchTimer = null;
function wireInviteSearch() {
    if (!inviteSearch || !inviteStatus) return;
    renderInviteSelected();

    inviteSearch.addEventListener("input", () => {
        if (inviteSearchTimer) {
            window.clearTimeout(inviteSearchTimer);
        }
        inviteSearchTimer = window.setTimeout(async () => {
            const q = inviteSearch.value.trim();
            if (q.length < 2) {
                inviteStatus.textContent = "Type at least 2 characters to search.";
                if (inviteResults) inviteResults.innerHTML = "";
                return;
            }
            inviteStatus.textContent = "Searching...";
            try {
                const res = await fetch(
                    campusFitUrl(`api/users?query=${encodeURIComponent(q)}&limit=15`)
                );
                if (res.status === 401) {
                    window.location.href = "login.html";
                    return;
                }
                const users = await res.json();
                inviteStatus.textContent = `Found ${users.length} user${users.length === 1 ? "" : "s"}.`;
                renderInviteResults(users);
            } catch (e) {
                inviteStatus.textContent = "Error searching users.";
            }
        }, 250);
    });
}

function formatDate(dateValue) {
    if (!dateValue) return "-";
    const parsed = new Date(dateValue + "T12:00:00");
    if (Number.isNaN(parsed.getTime())) return dateValue;
    return parsed.toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}

function updatePreview() {
    document.getElementById("previewActivity").textContent = activitySelect.value || "-";
    document.getElementById("previewLocation").textContent = locationSelect.value || "-";
    document.getElementById("previewDate").textContent = formatDate(dateInput.value);

    const start = startTimeInput.value || "--:--";
    const end = endTimeInput.value || "--:--";
    document.getElementById("previewTime").textContent = `${start} - ${end}`;

    const capacity = Math.max(parseInt(capacityInput.value || "1", 10), 1);
    document.getElementById("previewCapacity").textContent = `${capacity} Participants`;
}

function toDateInputValue(dateObj) {
    const y = dateObj.getFullYear();
    const m = String(dateObj.getMonth() + 1).padStart(2, "0");
    const d = String(dateObj.getDate()).padStart(2, "0");
    return `${y}-${m}-${d}`;
}

function renderCalendar() {
    const monthLabel = calendarMonth.toLocaleDateString(undefined, { month: "long", year: "numeric" });
    calendarTitle.textContent = monthLabel;
    calendarGrid.innerHTML = "";

    ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"].forEach((label) => {
        const el = document.createElement("div");
        el.className = "calendar-day-label";
        el.textContent = label;
        calendarGrid.appendChild(el);
    });

    const year = calendarMonth.getFullYear();
    const month = calendarMonth.getMonth();
    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const selected = dateInput.value;

    for (let i = 0; i < firstDay; i++) {
        const spacer = document.createElement("button");
        spacer.type = "button";
        spacer.className = "calendar-day muted";
        spacer.disabled = true;
        spacer.textContent = "";
        calendarGrid.appendChild(spacer);
    }

    for (let day = 1; day <= daysInMonth; day++) {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "calendar-day";
        btn.textContent = String(day);
        const thisDate = new Date(year, month, day);
        const value = toDateInputValue(thisDate);
        if (selected === value) {
            btn.classList.add("selected");
        }
        btn.addEventListener("click", () => {
            dateInput.value = value;
            updatePreview();
            renderCalendar();
        });
        calendarGrid.appendChild(btn);
    }
}

populateSelect(activitySelect, activityOptions);
populateSelect(locationSelect, locationOptions);

document.getElementById("increaseCapacity").addEventListener("click", () => {
    const current = Math.max(parseInt(capacityInput.value || "1", 10), 1);
    capacityInput.value = current + 1;
    updatePreview();
});

document.getElementById("decreaseCapacity").addEventListener("click", () => {
    const current = Math.max(parseInt(capacityInput.value || "1", 10), 1);
    capacityInput.value = Math.max(current - 1, 1);
    updatePreview();
});

document.querySelectorAll(".quick-tag").forEach((btn) => {
    btn.addEventListener("click", () => {
        document.querySelectorAll(".quick-tag").forEach((t) => t.classList.remove("active"));
        btn.classList.add("active");

        const activity = btn.getAttribute("data-activity");
        const location = btn.getAttribute("data-location");
        const start = btn.getAttribute("data-start");
        const end = btn.getAttribute("data-end");
        const capacity = btn.getAttribute("data-capacity");

        if (activity) activitySelect.value = activity;
        if (location) locationSelect.value = location;
        if (start) startTimeInput.value = start;
        if (end) endTimeInput.value = end;
        if (capacity) capacityInput.value = capacity;

        if (!dateInput.value) {
            const nextDay = new Date();
            nextDay.setDate(nextDay.getDate() + 1);
            dateInput.value = toDateInputValue(nextDay);
        }
        updatePreview();
        renderCalendar();
    });
});

[activitySelect, locationSelect, dateInput, startTimeInput, endTimeInput, capacityInput].forEach((el) => {
    el.addEventListener("input", updatePreview);
    el.addEventListener("change", updatePreview);
});

updatePreview();
renderCalendar();
wireInviteSearch();
loadInviteSuggestions();

calendarPrev.addEventListener("click", () => {
    calendarMonth.setMonth(calendarMonth.getMonth() - 1);
    renderCalendar();
});

calendarNext.addEventListener("click", () => {
    calendarMonth.setMonth(calendarMonth.getMonth() + 1);
    renderCalendar();
});

dateInput.addEventListener("change", renderCalendar);

document.getElementById("createEventForm").addEventListener("submit", async function(e) {
    e.preventDefault();
    const errorEl = document.getElementById("error");
    if (errorEl) {
        errorEl.textContent = "";
    }

    const activityType = activitySelect.value;
    const location = locationSelect.value;
    if (!activityType || !location) {
        if (errorEl) {
            errorEl.textContent = "Please choose an activity and a USC facility.";
        }
        return;
    }

    const params = new URLSearchParams(new FormData(this));
    if (!params.get("time") && params.get("startTime")) {
        params.set("time", params.get("startTime"));
    }
    const createUrl =
        typeof campusFitUrl === "function" ? campusFitUrl("createEvent") : "createEvent";
    const res = await fetch(createUrl, { method: "POST", body: params, credentials: "same-origin" });
    if (res.status === 401) {
        window.location.href = "login.html";
        return;
    }
    const data = await res.json();
    if (data.success) {
        window.location.href = "activities.html";
    } else {
        if (errorEl) {
            errorEl.textContent = data.message;
        }
    }
});
