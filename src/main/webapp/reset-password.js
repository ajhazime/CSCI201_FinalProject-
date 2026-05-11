const token = new URLSearchParams(window.location.search).get('token');

if (!token) {
    document.getElementById('resetForm').style.display = 'none';
    document.getElementById('invalidToken').style.display = 'block';
}

document.getElementById('resetForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const btn = document.getElementById('resetBtn');
    const msgEl = document.getElementById('resetMsg');
    msgEl.className = 'msg';

    const newPassword = this.querySelector('[name="newPassword"]').value;
    const confirmPassword = this.querySelector('[name="confirmPassword"]').value;

    if (newPassword.length < 12) {
        msgEl.textContent = 'Password must be at least 12 characters.';
        msgEl.className = 'msg error';
        return;
    }

    if (newPassword !== confirmPassword) {
        msgEl.textContent = 'Passwords do not match.';
        msgEl.className = 'msg error';
        return;
    }

    btn.disabled = true;
    btn.textContent = 'Resetting...';

    const params = new URLSearchParams({ action: 'resetWithToken', token, newPassword });

    fetch('/CampusActivities/api/recovery', { method: 'POST', body: params })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                msgEl.textContent = data.message;
                msgEl.className = 'msg success';
                document.getElementById('resetForm').style.display = 'none';
                setTimeout(() => { window.location.href = 'login.html'; }, 2500);
            } else {
                msgEl.textContent = data.message;
                msgEl.className = 'msg error';
                if (data.message && data.message.includes('invalid or has expired')) {
                    document.getElementById('resetForm').style.display = 'none';
                    document.getElementById('invalidToken').style.display = 'block';
                }
                btn.disabled = false;
                btn.textContent = 'Set New Password';
            }
        })
        .catch(() => {
            msgEl.textContent = 'Connection error. Please try again.';
            msgEl.className = 'msg error';
            btn.disabled = false;
            btn.textContent = 'Set New Password';
        });
});
