const sessionUser = JSON.parse(sessionStorage.getItem("user") || "null");

if (!sessionUser) {
    window.location.href = "login.html";
}

document.getElementById("sidebarUsername").textContent = sessionUser ? sessionUser.username : "User";
document.getElementById("sidebarInitials").textContent = sessionUser ? getInitials(sessionUser.username) : "U";

const params = new URLSearchParams(window.location.search);
const viewingUserId = params.get("userId");
const isOwnProfile = !viewingUserId || (sessionUser && String(viewingUserId) === String(sessionUser.id));

let profileUserId = null;
let selectedRating = 0;

if (isOwnProfile) {
    // Fetch own full profile from server
    fetch(typeof campusFitUrl === "function" ? campusFitUrl("api/profile") : "/CampusActivities/api/profile", {
        credentials: "same-origin"
    })
        .then(function(res) {
            if (res.status === 401) { window.location.href = "login.html"; return null; }
            return res.json();
        })
        .then(function(data) {
            if (!data || !data.success) return;
            profileUserId = data.user.id;
            populateProfile(data.user);
            document.getElementById("eventsJoinedCount").textContent = data.eventsJoined || 0;
            document.getElementById("ratingsReceivedCount").textContent = data.ratingCount || 0;
            document.getElementById("editProfileBtn").style.display = "inline-block";
            prefillEditForm(data.user);
            loadRatingDisplay(data.user.id);
        })
        .catch(function() {
            if (sessionUser) populateProfile(sessionUser);
        });
} else {
    // Viewing another user's profile
    profileUserId = parseInt(viewingUserId, 10);
    fetch((typeof campusFitUrl === "function" ? campusFitUrl("api/users") : "/CampusActivities/api/users") + "?id=" + viewingUserId, {
        credentials: "same-origin"
    })
        .then(function(res) {
            if (res.status === 404) { document.getElementById("profileUsername").textContent = "User not found"; return null; }
            return res.json();
        })
        .then(function(data) {
            if (data) {
                populateProfile(data.user || data);
                loadRatingDisplay(profileUserId);
                // Show rating section for other users (guests can't rate)
                if (sessionUser && sessionUser.id !== 0) {
                    document.getElementById("rateSection").style.display = "block";
                    loadExistingRating(profileUserId);
                }
            }
        })
        .catch(function() {
            document.getElementById("profileUsername").textContent = "Error loading profile";
        });
}

function loadRatingDisplay(userId) {
    fetch((typeof campusFitUrl === "function" ? campusFitUrl("api/ratings") : "/CampusActivities/api/ratings") + "?userId=" + userId, {
        credentials: "same-origin"
    })
        .then(function(res) { return res.json(); })
        .then(function(data) {
            if (!data || !data.success) return;
            renderAvgRating(data.avgRating, data.ratingCount);
            document.getElementById("ratingsReceivedCount").textContent = data.ratingCount || 0;
        })
        .catch(function() {});
}

function loadExistingRating(userId) {
    fetch((typeof campusFitUrl === "function" ? campusFitUrl("api/ratings") : "/CampusActivities/api/ratings") + "?userId=" + userId, {
        credentials: "same-origin"
    })
        .then(function(res) { return res.json(); })
        .then(function(data) {
            if (data && data.yourRating > 0) {
                setSelectedRating(data.yourRating);
            }
        })
        .catch(function() {});
}

function renderAvgRating(avg, count) {
    const starsEl = document.getElementById("avgRatingStars");
    const countEl = document.getElementById("ratingCountLabel");
    avg = parseFloat(avg) || 0;
    count = parseInt(count) || 0;

    let starsHtml = '<span class="avg-rating-val">' + (avg > 0 ? avg.toFixed(1) : "—") + '</span>';
    for (let i = 1; i <= 5; i++) {
        const filled = i <= Math.round(avg) ? " filled" : "";
        starsHtml += '<span class="star' + filled + '">&#9733;</span>';
    }
    starsEl.innerHTML = starsHtml;
    countEl.textContent = count > 0
        ? count + " rating" + (count === 1 ? "" : "s")
        : "No ratings yet.";
}

// Interactive star picker for rating submission
document.getElementById("rateStars").addEventListener("mouseover", function(e) {
    if (!e.target.classList.contains("star")) return;
    const val = parseInt(e.target.dataset.val);
    highlightStars(val);
});

document.getElementById("rateStars").addEventListener("mouseout", function() {
    highlightStars(selectedRating);
});

document.getElementById("rateStars").addEventListener("click", function(e) {
    if (!e.target.classList.contains("star")) return;
    setSelectedRating(parseInt(e.target.dataset.val));
});

function setSelectedRating(val) {
    selectedRating = val;
    highlightStars(val);
}

function highlightStars(val) {
    document.querySelectorAll("#rateStars .star").forEach(function(star) {
        star.classList.toggle("filled", parseInt(star.dataset.val) <= val);
    });
}

function submitRating() {
    if (selectedRating === 0) {
        showRateMsg("Please select a star rating first.", "error");
        return;
    }
    const btn = document.getElementById("rateSubmitBtn");
    btn.disabled = true;

    const params = new URLSearchParams({ rateeId: profileUserId, score: selectedRating });
    fetch(typeof campusFitUrl === "function" ? campusFitUrl("api/ratings") : "/CampusActivities/api/ratings", {
        method: "POST",
        body: params,
        credentials: "same-origin"
    })
        .then(function(res) { return res.json(); })
        .then(function(data) {
            if (data.success) {
                showRateMsg("Rating submitted!", "success");
                renderAvgRating(data.newAvgRating, data.ratingCount);
                document.getElementById("ratingsReceivedCount").textContent = data.ratingCount || 0;
            } else {
                showRateMsg(data.message || "Failed to submit rating.", "error");
                btn.disabled = false;
            }
        })
        .catch(function() {
            showRateMsg("Connection error. Please try again.", "error");
            btn.disabled = false;
        });
}

function showRateMsg(text, type) {
    const el = document.getElementById("rateMsg");
    el.textContent = text;
    el.className = "rate-msg " + type;
}

function populateProfile(user) {
    const username = user.username || "User";
    const email = user.email || "No email available";
    const skillLevel = user.skill_level || user.skillLevel || "Student";
    const penalties = user.penalties !== undefined && user.penalties !== null ? user.penalties : 0;
    const interests = parseInterests(user.interests);
    const avgRating = parseFloat(user.avgRating) || 0;

    document.getElementById("profileUsername").textContent = username;
    document.getElementById("infoUsername").textContent = username;
    document.getElementById("profileEmail").textContent = email;
    document.getElementById("infoEmail").textContent = email;
    document.getElementById("profileSkillLevel").textContent = skillLevel;
    document.getElementById("infoSkillLevel").textContent = skillLevel;
    document.getElementById("infoPenalties").textContent = penalties;
    document.getElementById("profileInitials").textContent = getInitials(username);

    renderInterests(interests);
    renderPreferredLocations(user.preferredLocations);

    const statusBox = document.getElementById("statusBox");
    if (penalties > 0) {
        statusBox.innerHTML = `
            <div class="status-check" style="background:#e05252;">!</div>
            <div>
                <strong>Active penalties: ${penalties}</strong>
                <p>This account has penalty points on record.</p>
            </div>`;
    }
}

const KNOWN_FACILITIES = ["Lyon Center", "USC Village Fitness Center", "Uytengsu Aquatics Center", "HSC Fitness Center", "PED South Gym"];

function toggleLocDropdown() {
    const panel = document.getElementById("locDropdownPanel");
    const arrow = document.getElementById("locDropdownArrow");
    if (!panel) return;
    const open = panel.classList.toggle("open");
    if (arrow) arrow.textContent = open ? "▲" : "▼";
}

function updateLocDropdownLabel() {
    const selected = [];
    document.querySelectorAll(".loc-cb").forEach(function(cb) {
        if (cb.checked) selected.push(cb.value);
    });
    const otherCheck = document.getElementById("otherLocationCheck");
    const otherText = document.getElementById("otherLocationText");
    if (otherCheck && otherCheck.checked && otherText && otherText.value.trim()) {
        selected.push("Other");
    }
    const label = document.getElementById("locDropdownLabel");
    if (label) label.textContent = selected.length > 0 ? selected.join(", ") : "Choose locations";
}

document.addEventListener("click", function(e) {
    const wrapper = document.querySelector(".loc-dropdown-wrapper");
    if (wrapper && !wrapper.contains(e.target)) {
        const panel = document.getElementById("locDropdownPanel");
        const arrow = document.getElementById("locDropdownArrow");
        if (panel) panel.classList.remove("open");
        if (arrow) arrow.textContent = "▼";
    }
});

document.querySelectorAll(".loc-cb").forEach(function(cb) {
    cb.addEventListener("change", updateLocDropdownLabel);
});

const otherCheckEl = document.getElementById("otherLocationCheck");
if (otherCheckEl) {
    otherCheckEl.addEventListener("change", function() {
        const otherText = document.getElementById("otherLocationText");
        otherText.style.display = this.checked ? "block" : "none";
        if (!this.checked) otherText.value = "";
        updateLocDropdownLabel();
    });
}

const otherTextEl = document.getElementById("otherLocationText");
if (otherTextEl) otherTextEl.addEventListener("input", updateLocDropdownLabel);

function prefillEditForm(user) {
    document.getElementById("editInterests").value = user.interests || "";
    const skillLevel = user.skill_level || user.skillLevel || "beginner";
    const select = document.getElementById("editSkillLevel");
    for (let opt of select.options) {
        if (opt.value === skillLevel) { opt.selected = true; break; }
    }

    const existing = (user.preferredLocations || "").split(",").map(function(l) { return l.trim(); }).filter(Boolean);
    const otherValues = [];

    document.querySelectorAll(".loc-cb").forEach(function(cb) {
        cb.checked = existing.includes(cb.value);
    });

    existing.forEach(function(loc) {
        if (!KNOWN_FACILITIES.includes(loc)) otherValues.push(loc);
    });

    const otherCheck = document.getElementById("otherLocationCheck");
    const otherText = document.getElementById("otherLocationText");
    if (otherValues.length > 0) {
        otherCheck.checked = true;
        otherText.style.display = "block";
        otherText.value = otherValues.join(", ");
    } else {
        otherCheck.checked = false;
        otherText.style.display = "none";
        otherText.value = "";
    }
    updateLocDropdownLabel();
}

function getSelectedLocations() {
    const selected = [];
    document.querySelectorAll(".loc-cb").forEach(function(cb) {
        if (cb.checked) selected.push(cb.value);
    });
    const otherCheck = document.getElementById("otherLocationCheck");
    const otherText = document.getElementById("otherLocationText");
    if (otherCheck && otherCheck.checked && otherText && otherText.value.trim()) {
        otherText.value.trim().split(",").forEach(function(v) {
            const t = v.trim();
            if (t) selected.push(t);
        });
    }
    return selected.join(", ");
}

function toggleEditForm() {
    const form = document.getElementById("editProfileForm");
    form.classList.toggle("open");
    document.getElementById("editMsg").className = "edit-msg";
    document.getElementById("editSecurityAnswer").value = "";
    document.getElementById("editSecurityQuestion").value = "";
}

function submitEditProfile(event) {
    event.preventDefault();
    const msgEl = document.getElementById("editMsg");
    msgEl.className = "edit-msg";

    const body = new URLSearchParams({
        interests: document.getElementById("editInterests").value.trim(),
        skillLevel: document.getElementById("editSkillLevel").value,
        preferredLocations: getSelectedLocations(),
        securityQuestion: document.getElementById("editSecurityQuestion").value,
        securityAnswer: document.getElementById("editSecurityAnswer").value.trim()
    });

    fetch(typeof campusFitUrl === "function" ? campusFitUrl("api/profile") : "/CampusActivities/api/profile", {
        method: "POST",
        body: body,
        credentials: "same-origin"
    })
        .then(function(res) { return res.json(); })
        .then(function(data) {
            if (data.success) {
                msgEl.textContent = "Profile updated!";
                msgEl.className = "edit-msg success";
                if (data.user) {
                    sessionStorage.setItem("user", JSON.stringify(data.user));
                    populateProfile(data.user);
                    prefillEditForm(data.user);
                }
                setTimeout(function() { toggleEditForm(); }, 1500);
            } else {
                msgEl.textContent = data.message || "Failed to update profile.";
                msgEl.className = "edit-msg error";
            }
        })
        .catch(function() {
            msgEl.textContent = "Connection error. Please try again.";
            msgEl.className = "edit-msg error";
        });
}

function parseInterests(interests) {
    if (!interests) return [];
    if (Array.isArray(interests)) return interests;
    return String(interests).split(",").map(function(i) { return i.trim(); }).filter(function(i) { return i.length > 0; });
}

function renderInterests(interests) {
    const el = document.getElementById("interestTags");
    if (!interests || interests.length === 0) {
        el.innerHTML = `<span class="empty-text">No interests added yet.</span>`;
        return;
    }
    el.innerHTML = interests.map(function(i) { return `<span class="interest-tag">${escapeHtml(i)}</span>`; }).join("");
}

function renderPreferredLocations(locString) {
    const el = document.getElementById("preferredLocationTags");
    if (!el) return;
    const locs = (locString || "").split(",").map(function(l) { return l.trim(); }).filter(Boolean);
    if (locs.length === 0) {
        el.innerHTML = `<span class="empty-text">No preferred locations set.</span>`;
        return;
    }
    el.innerHTML = locs.map(function(l) { return `<span class="interest-tag">${escapeHtml(l)}</span>`; }).join("");
}

function getInitials(name) {
    const parts = String(name).trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;").replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&#039;");
}
