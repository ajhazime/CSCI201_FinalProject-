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

document.getElementById("logoutLink").addEventListener("click", function(e) {
    e.preventDefault();

    fetch("/CampusActivities/logout")
        .then(function () {
            sessionStorage.removeItem("user");
            window.location.href = "login.html";
        })
        .catch(function () {
            sessionStorage.removeItem("user");
            window.location.href = "login.html";
        });
});

fetch("events")
    .then(function(res) {
        if (!res.ok) {
            throw new Error("Could not load activities.");
        }
        return res.json();
    })
    .then(function(events) {
        const tbody = document.getElementById("eventsBody");
        const eventCount = document.getElementById("eventCount");
        const openSpotCount = document.getElementById("openSpotCount");
        const locationCount = document.getElementById("locationCount");

        tbody.innerHTML = "";

        if (!events || events.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="empty-cell">No activities available yet.</td>
                </tr>
            `;

            eventCount.textContent = "0";
            openSpotCount.textContent = "0";
            locationCount.textContent = "0";
            return;
        }

        let totalOpenSpots = 0;
        const locations = new Set();

        events.forEach(function(e) {
            const maxParticipants = Number(e.maxParticipants || 0);
            const currentParticipants = Number(e.currentParticipants || 0);
            const openSpots = Math.max(maxParticipants - currentParticipants, 0);
            const endTime = e.endTime ? ` - ${e.endTime}` : "";

            totalOpenSpots += openSpots;

            if (e.location) {
                locations.add(e.location);
            }

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

        eventCount.textContent = events.length;
        openSpotCount.textContent = totalOpenSpots;
        locationCount.textContent = locations.size;
    })
    .catch(function(error) {
        const tbody = document.getElementById("eventsBody");

        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="error-cell">${escapeHtml(error.message)}</td>
            </tr>
        `;
    });

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