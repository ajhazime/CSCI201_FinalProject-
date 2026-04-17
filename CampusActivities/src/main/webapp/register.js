document.getElementById('registerForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    const params = new URLSearchParams(new FormData(this));
    const res = await fetch('register', { method: 'POST', body: params });
    const data = await res.json();
    if (data.success) {
        window.location.href = 'login.html';
    } else {
        document.getElementById('error').textContent = data.message;
    }
});
