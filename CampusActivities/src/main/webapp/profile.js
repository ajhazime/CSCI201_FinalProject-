const sessionUser = JSON.parse(sessionStorage.getItem("user"));

if (!sessionUser) {
    window.location.href = "login.html";
}

document.getElementById("sidebarUsername").textContent = sessionUser ? sessionUser.username : "User";
document.getElementById("sidebarInitials").textContent = sessionUser ? getInitials(sessionUser.username) : "U";

const params = new URLSearchParams(window.location.search);
const viewingUserId = params.get("userId");

if (viewingUserId) {
    fetch("/CampusActivities/api/users?id=" + viewingUserId)
        .then(function(response) {
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
        .then(function(data) {
            if (data) populateProfile(data);
        })
        .catch(function() {
            document.getElementById("profileUsername").textContent = "Error loading profile";
        });
} else {
    populateProfile(sessionUser);
}

function populateProfile(user) {
    const username = user.username || "User";
    const email = user.email || "No email available";
    const skillLevel = user.skill_level || user.skillLevel || "Student";
    const penalties = user.penalties !== undefined && user.penalties !== null ? user.penalties : 0;
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

    interestTags.innerHTML = interests.map(function (interest) {
        return `<span class="interest-tag">${escapeHtml(interest)}</span>`;
    }).join("");
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