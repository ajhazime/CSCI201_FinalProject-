(function () {
    var sessionUser = JSON.parse(sessionStorage.getItem("user") || "null");
    if (!sessionUser) {
        window.location.href = "login.html";
        return;
    }

    var params = new URLSearchParams(window.location.search);
    var eventId = parseInt(params.get("eventId"), 10);
    if (!eventId) {
        document.getElementById("appealEventSummary").textContent = "Missing event. Return to your profile and choose Appeal again.";
        document.getElementById("appealSubmitBtn").disabled = true;
        return;
    }

    var meta = null;
    try {
        meta = JSON.parse(sessionStorage.getItem("appealEventMeta") || "null");
    } catch (e) {
        meta = null;
    }
    if (meta && meta.eventId === eventId) {
        var line =
            (meta.activityType || "Event") +
            " · " +
            (meta.location || "") +
            " · " +
            formatScheduleAppeal(meta.date, meta.time, meta.endTime);
        document.getElementById("appealEventSummary").textContent = line;
    } else {
        document.getElementById("appealEventSummary").textContent = "Event #" + eventId + " (details will appear on your profile after submission).";
    }

    function formatScheduleAppeal(dateStr, timeStr, endTimeStr) {
        if (!dateStr) {
            return "";
        }
        try {
            var timePart = timeStr || "00:00:00";
            var start = new Date(dateStr + "T" + timePart);
            var startFormatted = start.toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
            if (!endTimeStr) {
                return startFormatted;
            }
            var endShort = String(endTimeStr).substring(0, 5);
            return startFormatted + " – ends " + endShort;
        } catch (e) {
            return dateStr + " " + (timeStr || "");
        }
    }

    function appealsPostUrl() {
        return typeof campusFitUrl === "function" ? campusFitUrl("api/appeals") : "/CampusActivities/api/appeals";
    }

    function profileUrl() {
        return typeof campusFitUrl === "function" ? campusFitUrl("profile.html") : "/CampusActivities/profile.html";
    }

    document.getElementById("appealBackBtn").addEventListener("click", function () {
        window.location.href = profileUrl();
    });

    document.getElementById("appealForm").addEventListener("submit", function (e) {
        e.preventDefault();
        var msg = document.getElementById("appealMessage").value.trim();
        var msgEl = document.getElementById("appealFormMsg");
        msgEl.className = "appeal-msg";
        msgEl.textContent = "";
        if (!msg) {
            msgEl.textContent = "Please enter a message.";
            msgEl.className = "appeal-msg err";
            return;
        }
        var btn = document.getElementById("appealSubmitBtn");
        btn.disabled = true;
        var body = new URLSearchParams({ eventId: String(eventId), message: msg });
        fetch(appealsPostUrl(), {
            method: "POST",
            body: body,
            credentials: "same-origin"
        })
            .then(function (res) {
                return res.json().then(function (data) {
                    return { ok: res.ok, data: data };
                });
            })
            .then(function (result) {
                if (result.data && result.data.success) {
                    msgEl.textContent = result.data.message || "Sent. Redirecting…";
                    msgEl.className = "appeal-msg ok";
                    try {
                        sessionStorage.removeItem("appealEventMeta");
                    } catch (err) {
                        /* ignore */
                    }
                    setTimeout(function () {
                        window.location.href = profileUrl();
                    }, 1200);
                } else {
                    msgEl.textContent = (result.data && result.data.message) || "Could not send appeal.";
                    msgEl.className = "appeal-msg err";
                    btn.disabled = false;
                }
            })
            .catch(function () {
                msgEl.textContent = "Connection error.";
                msgEl.className = "appeal-msg err";
                btn.disabled = false;
            });
    });
})();
