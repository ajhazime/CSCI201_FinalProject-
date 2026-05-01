const defaultFacilities = [
    {
        id: 1,
        name: "Lyon Center",
        displayName: "Lyon Center",
        address: "1026 W 34th St, Los Angeles, CA 90089",
        shortAddress: "1026 W 34th St",
        hours: "Hours unavailable",
        coords: [34.02493, -118.28711],
        tags: ["Basketball", "Cardio", "Track", "Weight room", "Racquetball"],
        openSpots: null,
        capacity: null,
        occupancyPercent: null,
        averageRating: 0,
        reviewCount: 0,
        reviews: []
    },
    {
        id: 2,
        name: "Village Gym",
        displayName: "Village Gym",
        address: "USC Village, Los Angeles, CA 90089",
        shortAddress: "USC Village",
        hours: "Hours unavailable",
        coords: [34.02590, -118.28595],
        tags: ["Strength", "Weights"],
        openSpots: null,
        capacity: null,
        occupancyPercent: null,
        averageRating: 0,
        reviewCount: 0,
        reviews: []
    },
    {
        id: 3,
        name: "Lyon Pool",
        displayName: "Lyon Pool",
        address: "1026 W 34th St, Los Angeles, CA 90089",
        shortAddress: "Lyon Center",
        hours: "Hours unavailable",
        coords: [34.02442, -118.28745],
        tags: ["Swimming", "8 lanes"],
        openSpots: null,
        capacity: null,
        occupancyPercent: null,
        averageRating: 0,
        reviewCount: 0,
        reviews: []
    },
    {
        id: 4,
        name: "Galen Center",
        displayName: "Galen Center",
        address: "3400 S Figueroa St, Los Angeles, CA 90089",
        shortAddress: "3400 S Figueroa St",
        hours: "Hours unavailable",
        coords: [34.02132, -118.28092],
        tags: ["Basketball", "Volleyball"],
        openSpots: null,
        capacity: null,
        occupancyPercent: null,
        averageRating: 0,
        reviewCount: 0,
        reviews: []
    }
];

let facilities = [];
let selectedFacilityId = 1;
let selectedRating = 0;
let map;
let markers = [];

document.addEventListener("DOMContentLoaded", function () {
    setupUserInfo();
    initMap();
    loadFacilities();
});

function setupUserInfo() {
    const user = JSON.parse(sessionStorage.getItem("user"));
    const nameEl = document.getElementById("sidebarName");
    const initialsEl = document.getElementById("profileInitials");

    if (user && user.username) {
        nameEl.textContent = user.username;
        initialsEl.textContent = getInitials(user.username);
    } else {
        nameEl.textContent = "Guest User";
        initialsEl.textContent = "GU";
    }
}

function initMap() {
    map = L.map("map").setView([34.0246, -118.2848], 16);

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution: "&copy; OpenStreetMap contributors"
    }).addTo(map);
}

async function loadFacilities() {
    facilities = defaultFacilities.map(function (facility) {
        return { ...facility };
    });

    try {
        const response = await fetch("api/locations");

        if (response.ok) {
            const backendFacilities = await response.json();
            mergeBackendData(backendFacilities);
        }
    } catch (error) {
        console.log("Backend not loaded. Showing default facility data.");
    }

    renderFacilityList();
    renderDetailPanel();
    renderMapMarkers();

    setTimeout(function () {
        map.invalidateSize();
    }, 200);
}

function mergeBackendData(backendFacilities) {
    if (!backendFacilities || backendFacilities.length === 0) {
        return;
    }

    backendFacilities.forEach(function (backendFacility) {
        const match = facilities.find(function (facility) {
            return facility.name.toLowerCase() === backendFacility.name.toLowerCase()
                || facility.displayName.toLowerCase() === backendFacility.name.toLowerCase();
        });

        if (match) {
            match.id = backendFacility.id;

            if (backendFacility.averageRating !== undefined && backendFacility.averageRating !== null) {
                match.averageRating = Number(backendFacility.averageRating || 0);
            }

            if (backendFacility.reviewCount !== undefined && backendFacility.reviewCount !== null) {
                match.reviewCount = Number(backendFacility.reviewCount || 0);
            }

            match.reviews = backendFacility.reviews || [];
        }
    });

    if (facilities.length > 0) {
        selectedFacilityId = facilities[0].id;
    }
}

function renderFacilityList() {
    const listContainer = document.getElementById("facilityList");

    listContainer.innerHTML = facilities.map(function (facility) {
        const selectedClass = facility.id === selectedFacilityId ? "selected" : "";

        return `
            <div class="facility-item ${selectedClass}" data-id="${facility.id}">
                <div class="facility-item-top">
                    <div>
                        <h3>${escapeHtml(facility.displayName)}</h3>

                        <div class="facility-address">
                            ${escapeHtml(facility.address)} · ${escapeHtml(facility.hours)}
                        </div>

                        <div class="stars-row">
                            <div class="star-text">${getStars(facility.averageRating)}</div>
                            <div class="rating-number">${facility.averageRating.toFixed(1)}</div>
                        </div>

                        <div class="facility-tags">
                            ${facility.tags.map(tag => `<span class="tag">${escapeHtml(tag)}</span>`).join("")}
                        </div>
                    </div>

                    <span class="open-pill">
                        Open spots: ${formatAvailability(facility.openSpots)}
                    </span>
                </div>
            </div>
        `;
    }).join("");

    document.querySelectorAll(".facility-item").forEach(function (item) {
        item.addEventListener("click", function () {
            selectedFacilityId = Number(item.getAttribute("data-id"));
            renderFacilityList();
            renderDetailPanel();
            highlightSelectedMarker();
        });
    });
}

function renderDetailPanel() {
    const detailPanel = document.getElementById("detailPanel");

    const facility = facilities.find(function (facility) {
        return facility.id === selectedFacilityId;
    });

    if (!facility) {
        detailPanel.innerHTML = `<div class="loading-card">No facility selected.</div>`;
        return;
    }

    detailPanel.innerHTML = `
        <div class="detail-header">
            <div>
                <h3>${escapeHtml(facility.displayName)}</h3>
                <div class="detail-subtitle">${escapeHtml(facility.shortAddress)}</div>

                <div class="facility-tags" style="margin-top: 14px;">
                    ${facility.tags.map(tag => `<span class="tag">${escapeHtml(tag)}</span>`).join("")}
                </div>
            </div>

            <div style="display:flex; gap:12px; align-items:center; flex-wrap:wrap;">
                <span class="open-pill">Open spots: ${formatAvailability(facility.openSpots)}</span>
                <a href="activities.html" class="primary-button">View events here</a>
            </div>
        </div>

        <div class="detail-meta-grid">
            <div class="metric-card">
                <div class="metric-value">${formatAvailability(facility.capacity)}</div>
                <div class="metric-label">Total capacity</div>
            </div>

            <div class="metric-card">
                <div class="metric-value">${formatAvailability(facility.openSpots)}</div>
                <div class="metric-label">Spots open now</div>
            </div>
        </div>

        <div class="occupancy-wrap">
            <div class="occupancy-label-row">
                <span>Occupancy</span>
                <span>${formatOccupancy(facility.occupancyPercent)}</span>
            </div>
            <div class="occupancy-bar">
                <div class="occupancy-fill" style="width:${facility.occupancyPercent || 0}%"></div>
            </div>
        </div>

        <div class="detail-section-title">Ratings</div>
        <div class="metric-card" style="text-align:left;">
            <div class="star-text" style="font-size:24px;">${getStars(facility.averageRating)}</div>
            <div style="margin-top:6px; color:#555; font-size:15px;">
                Average rating: <strong>${facility.averageRating.toFixed(1)} / 5</strong>
            </div>
            <div style="margin-top:4px; color:#7a7a7a; font-size:14px;">
                ${facility.reviewCount} review${facility.reviewCount === 1 ? "" : "s"}
            </div>
        </div>

        <div class="detail-section-title">Leave a review</div>
        <form id="reviewForm" class="review-form">
            <label>Your rating</label>
            <div id="starPicker" class="star-picker">
                <span class="star-btn" data-value="1">★</span>
                <span class="star-btn" data-value="2">★</span>
                <span class="star-btn" data-value="3">★</span>
                <span class="star-btn" data-value="4">★</span>
                <span class="star-btn" data-value="5">★</span>
            </div>

            <label for="reviewText">Your review</label>
            <textarea id="reviewText" placeholder="Write a short review of this facility..."></textarea>

            <button type="submit" class="submit-review-btn">Submit review</button>
        </form>

        <div class="detail-section-title">Student reviews</div>
        <div class="reviews-list">
            ${renderReviews(facility.reviews)}
        </div>
    `;

    bindReviewForm();
}

function renderReviews(reviews) {
    if (!reviews || reviews.length === 0) {
        return `
            <div class="review-item">
                <div class="review-text">No reviews yet. Be the first to review this facility.</div>
            </div>
        `;
    }

    return reviews.map(function (review) {
        return `
            <div class="review-item">
                <div class="review-item-top">
                    <div>
                        <div class="review-name">${escapeHtml(review.reviewerName || "Anonymous")}</div>
                        <div class="star-text" style="font-size:18px;">${getStars(review.rating)}</div>
                    </div>
                    <div class="review-date">${escapeHtml(formatDate(review.createdAt))}</div>
                </div>
                <div class="review-text">${escapeHtml(review.reviewText || "No written review.")}</div>
            </div>
        `;
    }).join("");
}

function bindReviewForm() {
    selectedRating = 0;

    const starButtons = document.querySelectorAll(".star-btn");
    const form = document.getElementById("reviewForm");
    const reviewText = document.getElementById("reviewText");

    starButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            selectedRating = Number(button.getAttribute("data-value"));
            paintSelectedStars();
        });
    });

    form.addEventListener("submit", async function (event) {
        event.preventDefault();

        const facility = facilities.find(function (facility) {
            return facility.id === selectedFacilityId;
        });

        const text = reviewText.value.trim();

        if (!facility) {
            showMessage("No facility selected.", "error");
            return;
        }

        if (!selectedRating || selectedRating < 1 || selectedRating > 5) {
            showMessage("Please select a star rating before submitting.", "error");
            return;
        }

        if (text.length > 500) {
            showMessage("Please keep your review under 500 characters.", "error");
            return;
        }

        const formData = new URLSearchParams();
        formData.append("facilityId", facility.id);
        formData.append("rating", selectedRating);
        formData.append("reviewText", text);

        try {
            const response = await fetch("api/locations", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: formData.toString()
            });

            const result = await response.json();

            if (!response.ok || !result.success) {
                throw new Error(result.message || "Could not submit review.");
            }

            showMessage("Review submitted successfully.", "success");
            await loadFacilities();
        } catch (error) {
            showMessage(error.message, "error");
        }
    });
}

function paintSelectedStars() {
    const starButtons = document.querySelectorAll(".star-btn");

    starButtons.forEach(function (button) {
        const value = Number(button.getAttribute("data-value"));

        if (value <= selectedRating) {
            button.classList.add("active");
        } else {
            button.classList.remove("active");
        }
    });
}

function renderMapMarkers() {
    markers.forEach(function (marker) {
        map.removeLayer(marker);
    });

    markers = [];

    facilities.forEach(function (facility) {
        const isSelected = facility.id === selectedFacilityId;

        const marker = L.circleMarker(facility.coords, {
            radius: isSelected ? 11 : 8,
            color: isSelected ? "#9d2235" : "#666666",
            fillColor: isSelected ? "#9d2235" : "#999999",
            fillOpacity: 0.95,
            weight: 3
        }).addTo(map);

        marker.bindPopup(`
            <strong>${escapeHtml(facility.displayName)}</strong><br>
            ${escapeHtml(facility.address)}<br>
            Rating: ${facility.averageRating.toFixed(1)} / 5
        `);

        marker.on("click", function () {
            selectedFacilityId = facility.id;
            renderFacilityList();
            renderDetailPanel();
            highlightSelectedMarker();
        });

        marker.facilityId = facility.id;
        markers.push(marker);
    });

    const bounds = L.latLngBounds(facilities.map(function (facility) {
        return facility.coords;
    }));

    map.fitBounds(bounds, {
        padding: [40, 40],
        maxZoom: 17
    });

    highlightSelectedMarker();
}

function highlightSelectedMarker() {
    markers.forEach(function (marker) {
        const isSelected = marker.facilityId === selectedFacilityId;

        marker.setStyle({
            radius: isSelected ? 11 : 8,
            color: isSelected ? "#9d2235" : "#666666",
            fillColor: isSelected ? "#9d2235" : "#999999",
            fillOpacity: 0.95,
            weight: 3
        });

        if (isSelected) {
            marker.openPopup();
        }
    });
}

function getStars(rating) {
    const rounded = Math.round(Number(rating || 0));
    let stars = "";

    for (let i = 1; i <= 5; i++) {
        stars += i <= rounded ? "★" : "☆";
    }

    return stars;
}

function showMessage(message, type) {
    const messageBox = document.getElementById("messageBox");

    messageBox.textContent = message;
    messageBox.className = "message-box " + type;

    setTimeout(function () {
        messageBox.textContent = "";
        messageBox.className = "message-box";
    }, 3500);
}

function formatDate(value) {
    if (!value) {
        return "";
    }

    const date = new Date(value);

    if (isNaN(date.getTime())) {
        return value;
    }

    return date.toLocaleString();
}

function formatAvailability(value) {
    if (value === null || value === undefined || value === "") {
        return "N/A";
    }

    return value;
}

function formatOccupancy(value) {
    if (value === null || value === undefined || value === "") {
        return "N/A";
    }

    return value + "%";
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function getInitials(name) {
    const parts = String(name).trim().split(/\s+/);

    if (parts.length === 1) {
        return parts[0].substring(0, 2).toUpperCase();
    }

    return (parts[0][0] + parts[1][0]).toUpperCase();
}