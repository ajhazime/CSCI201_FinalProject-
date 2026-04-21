document.getElementById('loginForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const params = new URLSearchParams(new FormData(this));
    fetch('/CampusActivities/login', { method: 'POST', body: params })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                sessionStorage.setItem('user', JSON.stringify(data.user));
                window.location.href = 'dashboard.html';
            } else {
                document.getElementById('error').textContent = data.message;
            }
        })
        .catch(err => {
            document.getElementById('error').textContent = 'Error: ' + err.message;
        });
});
