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

document.querySelectorAll(".suggestion").forEach((btn) => {
    btn.addEventListener("click", () => {
        const value = btn.getAttribute("data-activity");
        if (value) {
            activitySelect.value = value;
            updatePreview();
        }
    });
});

[activitySelect, locationSelect, dateInput, startTimeInput, endTimeInput, capacityInput].forEach((el) => {
    el.addEventListener("input", updatePreview);
    el.addEventListener("change", updatePreview);
});

updatePreview();

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
