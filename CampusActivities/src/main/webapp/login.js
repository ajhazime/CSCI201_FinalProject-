document.getElementById('loginForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    const params = new URLSearchParams(new FormData(this));
    const res = await fetch('login', { method: 'POST', body: params });
    const data = await res.json();
    if (data.success) {
        sessionStorage.setItem('user', JSON.stringify(data.user));
        window.location.href = 'dashboard.html';
    } else {
        document.getElementById('error').textContent = data.message;
    }
});
