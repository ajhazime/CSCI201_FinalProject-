document.addEventListener("DOMContentLoaded", function () {
    const status  = document.getElementById("status");
    const results = document.getElementById("results");

    fetch("api/matches")
        .then(function (response) {
            if (response.status === 401) {
                window.location.href = "login.html";
                return;
            }
            return response.json();
        })
        .then(function (data) {
            if (!data) return;

            if (data.length === 0) {
                status.textContent = "No matches found yet — try adding more interests to your profile.";
                return;
            }

            status.textContent = data.length + (data.length === 1 ? " match found" : " matches found");
            results.innerHTML = ""; // clear any existing placeholder content

            data.forEach(function (match) {
                results.appendChild(buildMatchCard(match));
            });
        })
        .catch(function () {
            status.textContent = "Error loading matches. Please try again.";
        });
});

/**
 * Build a single match card DOM node.
 * Card layout: [avatar] [name + meta + actions] [score badge]
 */
function buildMatchCard(match) {
    const card = document.createElement("div");
    card.className = "match-card";

    // Avatar with the user's initials
    const avatar = document.createElement("div");
    avatar.className = "match-avatar";
    avatar.textContent = getInitials(match.username);
    avatar.style.backgroundColor = getAvatarColor(match.username);

    // Middle: name + interests/skill + rating + action buttons
    const info = document.createElement("div");
    info.className = "match-info";

    const name = document.createElement("div");
    name.className = "match-name";
    name.textContent = match.username;

    const meta = document.createElement("div");
    meta.className = "match-meta";
    meta.textContent = formatInterests(match.interests) + " · " + capitalize(match.skillLevel);

    const rating = document.createElement("div");
    rating.className = "match-rating";
    rating.textContent = "★ " + (Number(match.avgRating) || 0).toFixed(1);

    // Actions row — buttons that wire up to other features later
    const actions = document.createElement("div");
    actions.className = "match-actions";

    const viewBtn = document.createElement("button");
    viewBtn.className = "card-action card-action-secondary";
    viewBtn.textContent = "View Profile";
    viewBtn.addEventListener("click", function () {
        window.location.href = "profile.html?userId=" + match.userID;
    });

    const inviteBtn = document.createElement("button");
    inviteBtn.className = "card-action card-action-primary";
    inviteBtn.textContent = "Invite to Event";
    inviteBtn.addEventListener("click", function () {
        openInviteModal(match.userID, match.username);
    });

    actions.appendChild(viewBtn);
    actions.appendChild(inviteBtn);

    info.appendChild(name);
    info.appendChild(meta);
    info.appendChild(rating);
    info.appendChild(actions);

    // Right: match score
    const score = document.createElement("div");
    score.className = "match-score";
    score.textContent = match.matchScore + "%";

    card.appendChild(avatar);
    card.appendChild(info);
    card.appendChild(score);

    return card;
}

// ----- helpers ---------------------------------------------------------

function getInitials(name) {
    if (!name) return "?";
    const parts = name.replace(/[._-]/g, " ").split(/\s+/).filter(Boolean);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

function getAvatarColor(seed) {
    // Deterministic color per user so the same person always shows the same avatar color
    const colors = ["#992233", "#1a73e8", "#34a853", "#9333ea", "#ea580c", "#0891b2", "#be185d"];
    let hash = 0;
    for (let i = 0; i < seed.length; i++) {
        hash = seed.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
}

function formatInterests(interests) {
    if (!interests) return "No interests listed";
    return interests.split(",")
        .map(function (s) { return capitalize(s.trim()); })
        .filter(Boolean)
        .join(", ");
}

function capitalize(str) {
    if (!str) return "";
    return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
}

function openInviteModal(inviteeId, inviteeName) {
    const existing = document.getElementById("inviteModal");
    if (existing) existing.remove();

    const modal = document.createElement("div");
    modal.id = "inviteModal";
    modal.className = "invite-overlay";
    modal.addEventListener("click", function(e) { if (e.target === modal) modal.remove(); });

    const box = document.createElement("div");
    box.className = "invite-box";

    const title = document.createElement("h3");
    title.className = "invite-title";
    title.textContent = "Invite " + inviteeName + " to an event";

    const list = document.createElement("div");
    list.id = "inviteEventList";
    list.textContent = "Loading your events...";

    const footer = document.createElement("div");
    footer.className = "invite-footer";

    const closeBtn = document.createElement("button");
    closeBtn.className = "card-action card-action-secondary";
    closeBtn.textContent = "Cancel";
    closeBtn.addEventListener("click", function() { modal.remove(); });

    footer.appendChild(closeBtn);
    box.appendChild(title);
    box.appendChild(list);
    box.appendChild(footer);
    modal.appendChild(box);
    document.body.appendChild(modal);

    fetch("/CampusActivities/myEvents?inviteeId=" + inviteeId)
        .then(function(res) {
            if (res.status === 401) { window.location.href = "login.html"; return; }
            return res.json();
        })
        .then(function(events) {
            list.innerHTML = "";
            if (!events || events.length === 0) {
                list.textContent = "No events to invite to.";
                return;
            }
            events.forEach(function(event) {
                const row = document.createElement("div");
                row.className = "invite-event-row";

                const info = document.createElement("span");
                info.className = "invite-event-info";
                info.textContent = capitalize(event.activityType) + " · " + event.location + " · " + event.date;

                const btn = document.createElement("button");
                btn.className = "card-action card-action-primary";
                btn.textContent = "Invite";
                btn.addEventListener("click", function() { sendInvite(event.id, inviteeId, modal); });

                row.appendChild(info);
                row.appendChild(btn);
                list.appendChild(row);
            });
        })
        .catch(function() {
            list.textContent = "Failed to load events.";
        });
}

function sendInvite(eventId, inviteeId, modal) {
    const params = new URLSearchParams({ eventId: eventId, inviteeId: inviteeId });
    fetch("/CampusActivities/sendInvite", { method: "POST", body: params })
        .then(function(res) { return res.json(); })
        .then(function(data) {
            alert(data.message);
            if (data.success) modal.remove();
        })
        .catch(function() {
            alert("Failed to send invite.");
        });
}