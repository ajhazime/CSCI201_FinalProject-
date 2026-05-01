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

function populateSelect(select, options) {
    select.innerHTML = "";
    options.forEach((option) => {
        const el = document.createElement("option");
        el.value = option;
        el.textContent = option;
        select.appendChild(el);
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
    errorEl.textContent = "";

    const activityType = activitySelect.value;
    const location = locationSelect.value;
    if (!activityType || !location) {
        errorEl.textContent = "Please choose an activity and a USC facility.";
        return;
    }

    const params = new URLSearchParams(new FormData(this));
    if (!params.get("time") && params.get("startTime")) {
        params.set("time", params.get("startTime"));
    }
    const res = await fetch("createEvent", { method: "POST", body: params });
    if (res.status === 401) {
        window.location.href = "login.html";
        return;
    }
    const data = await res.json();
    if (data.success) {
        window.location.href = "activities.html";
    } else {
        errorEl.textContent = data.message;
    }
});
