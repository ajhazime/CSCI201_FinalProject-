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
    const monthLabel = calendarMonth.toLocaleDateString(undefined, { month: "long", year: "numeric" });
    calTitle.textContent = monthLabel;
    calGrid.innerHTML = "";

    ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"].forEach((label) => {
        const el = document.createElement("div");
        el.className = "cal-dow";
        el.textContent = label;
        calGrid.appendChild(el);
    });

    const eventsByDate = groupEventsByDate(allEvents);

    const year = calendarMonth.getFullYear();
    const month = calendarMonth.getMonth();
    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    const prevMonthDays = new Date(year, month, 0).getDate();
    for (let i = 0; i < firstDay; i++) {
        const dayNum = prevMonthDays - (firstDay - 1 - i);
        const cell = document.createElement("div");
        cell.className = "cal-day muted";
        cell.innerHTML = `<div class="cal-day-top"><div class="cal-num">${dayNum}</div></div>`;
        calGrid.appendChild(cell);
    }

    for (let day = 1; day <= daysInMonth; day++) {
        const thisDate = new Date(year, month, day);
        const value = toDateInputValue(thisDate);
        const todaysEvents = eventsByDate.get(value) || [];

        const cell = document.createElement("div");
        cell.className = "cal-day";
        if (selectedDate === value) {
            cell.classList.add("selected");
        }

        const badge = todaysEvents.length > 0 ? `<div class="cal-badge">${todaysEvents.length}</div>` : "";
        const pills = todaysEvents.slice(0, 2).map((e) => {
            return `<div class="cal-pill" title="${escapeHtml((e.activityType || "Event") + " @ " + (e.location || ""))}">${escapeHtml(e.activityType || "Event")}</div>`;
        }).join("");
        const more = todaysEvents.length > 2 ? `<div class="cal-more">+${todaysEvents.length - 2} more</div>` : "";

        cell.innerHTML = `
            <div class="cal-day-top">
                <div class="cal-num">${day}</div>
                ${badge}
            </div>
            <div class="cal-events">
                ${pills}
                ${more}
            </div>
        `;

        cell.addEventListener("click", () => setSelectedDate(value));
        calGrid.appendChild(cell);
    }
}

function renderTable() {
    const tbody = document.getElementById("eventsBody");
    const eventCount = document.getElementById("eventCount");
    const openSpotCount = document.getElementById("openSpotCount");
    const locationCount = document.getElementById("locationCount");

    if (!tbody || !eventCount || !openSpotCount || !locationCount) return;

    const shown = selectedDate
        ? allEvents.filter((e) => String(e.date || "").trim() === selectedDate)
        : allEvents.slice();

    tbody.innerHTML = "";

    if (!shown || shown.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="empty-cell">${selectedDate ? "No events on this day yet." : "No activities available yet."}</td>
            </tr>
        `;
        eventCount.textContent = String(allEvents.length || 0);
        openSpotCount.textContent = String(calcTotalOpenSpots(allEvents));
        locationCount.textContent = String(calcLocationCount(allEvents));
        return;
    }

    shown.sort((a, b) => {
        const d = String(a.date || "").localeCompare(String(b.date || ""));
        if (d !== 0) return d;
        return String(a.time || "").localeCompare(String(b.time || ""));
    });

    shown.forEach(function(e) {
        const maxParticipants = Number(e.maxParticipants || 0);
        const currentParticipants = Number(e.currentParticipants || 0);
        const endTime = e.endTime ? ` - ${e.endTime}` : "";

        const row = document.createElement("tr");
        row.innerHTML = `
            <td><span class="activity-pill">${escapeHtml(e.activityType || "Activity")}</span></td>
            <td><span class="location-text">${escapeHtml(e.location || "N/A")}</span></td>
            <td>${escapeHtml(e.date || "N/A")}</td>
            <td>${escapeHtml((e.time || "N/A") + endTime)}</td>
            <td><span class="spots-pill">${currentParticipants}/${maxParticipants}</span></td>
        `;
        tbody.appendChild(row);
    });

    eventCount.textContent = String(allEvents.length || 0);
    openSpotCount.textContent = String(calcTotalOpenSpots(allEvents));
    locationCount.textContent = String(calcLocationCount(allEvents));
}

function calcTotalOpenSpots(events) {
    let total = 0;
    (events || []).forEach((e) => {
        const maxParticipants = Number(e.maxParticipants || 0);
        const currentParticipants = Number(e.currentParticipants || 0);
        total += Math.max(maxParticipants - currentParticipants, 0);
    });
    return total;
}

function calcLocationCount(events) {
    const locations = new Set();
    (events || []).forEach((e) => {
        if (e.location) locations.add(e.location);
    });
    return locations.size;
}

function showError(message) {
    const tbody = document.getElementById("eventsBody");
    if (!tbody) return;
    tbody.innerHTML = `
        <tr>
            <td colspan="5" class="error-cell">${escapeHtml(message)}</td>
        </tr>
    `;
}

function fetchAndRender() {
    fetch("events")
        .then(function(res) {
            if (!res.ok) {
                throw new Error("Could not load activities.");
            }
            return res.json();
        })
        .then(function(events) {
            allEvents = Array.isArray(events) ? events : [];
            renderCalendar();
            renderTable();
        })
        .catch(function(error) {
            showError(error.message);
        });
}

if (calPrev) {
    calPrev.addEventListener("click", () => {
        calendarMonth.setMonth(calendarMonth.getMonth() - 1);
        renderCalendar();
    });
}

if (calNext) {
    calNext.addEventListener("click", () => {
        calendarMonth.setMonth(calendarMonth.getMonth() + 1);
        renderCalendar();
    });
}

if (calToday) {
    calToday.addEventListener("click", () => {
        const now = new Date();
        calendarMonth = new Date(now.getFullYear(), now.getMonth(), 1);
        setSelectedDate(toDateInputValue(now));
    });
}

if (calAll) {
    calAll.addEventListener("click", () => setSelectedDate(null));
}

fetchAndRender();

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