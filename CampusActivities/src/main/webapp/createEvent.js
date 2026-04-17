document.getElementById('createEventForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    const params = new URLSearchParams(new FormData(this));
    const res = await fetch('createEvent', { method: 'POST', body: params });
    if (res.status === 401) {
        window.location.href = 'login.html';
        return;
    }
    const data = await res.json();
    if (data.success) {
        window.location.href = 'activities.html';
    } else {
        document.getElementById('error').textContent = data.message;
    }
});
