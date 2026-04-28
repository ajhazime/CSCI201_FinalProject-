document.addEventListener("DOMContentLoaded", function () {
    const status  = document.getElementById("status");
    const results = document.getElementById("results");

    fetch("/api/matches")
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
                status.textContent = "No matches found.";
                return;
            }

            status.textContent = data.length + " match(es) found:";

            // TODO! : Render user cards here
            results.textContent = JSON.stringify(data, null, 2);
        })
        .catch(function () {
            status.textContent = "Error loading matches. Please try again.";
        });
});
