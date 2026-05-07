// Tab switching
function activateTab(tabName) {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
    const tab = document.querySelector('.tab[data-tab="' + tabName + '"]');
    const panel = document.getElementById('panel-' + tabName);
    if (tab) tab.classList.add('active');
    if (panel) panel.classList.add('active');
}

document.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', function() {
        activateTab(this.dataset.tab);
    });
});

// Open specific tab if URL has ?tab=xxx
const urlTab = new URLSearchParams(window.location.search).get('tab');
if (urlTab) activateTab(urlTab);

// Find username
document.getElementById('usernameForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const btn = document.getElementById('usernamBtn');
    const msgEl = document.getElementById('usernameMsg');
    const resultEl = document.getElementById('usernameResult');
    msgEl.className = 'msg';
    resultEl.style.display = 'none';
    btn.disabled = true;
    btn.textContent = 'Searching...';

    const email = this.querySelector('[name="email"]').value.trim();
    const params = new URLSearchParams({ action: 'username', email });

    fetch('/CampusActivities/api/recovery', { method: 'POST', body: params })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                document.getElementById('foundUsername').textContent = data.username;
                resultEl.style.display = 'block';
            } else {
                msgEl.textContent = data.message;
                msgEl.className = 'msg error';
            }
        })
        .catch(() => {
            msgEl.textContent = 'Connection error. Please try again.';
            msgEl.className = 'msg error';
        })
        .finally(() => {
            btn.disabled = false;
            btn.textContent = 'Find My Username';
        });
});

// ── Forgot Password: 3-step security-question flow ──────────────────────────

let forgotEmail = '';
let forgotResetToken = '';

function forgotReset() {
    forgotEmail = '';
    forgotResetToken = '';
    document.getElementById('forgot-step1').style.display = '';
    document.getElementById('forgot-step2').style.display = 'none';
    document.getElementById('forgot-step3').style.display = 'none';
    document.getElementById('forgotStep1Msg').className = 'msg';
    document.getElementById('forgotEmailForm').reset();
}

// Step 1 — find account by email
document.getElementById('forgotEmailForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const btn = document.getElementById('forgotEmailBtn');
    const msgEl = document.getElementById('forgotStep1Msg');
    msgEl.className = 'msg';
    btn.disabled = true;
    btn.textContent = 'Looking up...';

    forgotEmail = this.querySelector('[name="email"]').value.trim();
    const params = new URLSearchParams({ action: 'getQuestion', email: forgotEmail });

    const url = typeof campusFitUrl === 'function' ? campusFitUrl('api/recovery') : '/CampusActivities/api/recovery';
    fetch(url, { method: 'POST', body: params })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                document.getElementById('forgotQuestion').textContent = data.question;
                document.getElementById('forgot-step1').style.display = 'none';
                document.getElementById('forgot-step2').style.display = '';
            } else {
                msgEl.textContent = data.message;
                msgEl.className = 'msg error';
            }
        })
        .catch(() => {
            msgEl.textContent = 'Connection error. Please try again.';
            msgEl.className = 'msg error';
        })
        .finally(() => {
            btn.disabled = false;
            btn.textContent = 'Find My Account';
        });
});

// Step 2 — verify answer
document.getElementById('forgotAnswerForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const btn = document.getElementById('forgotAnswerBtn');
    const msgEl = document.getElementById('forgotStep2Msg');
    msgEl.className = 'msg';
    btn.disabled = true;
    btn.textContent = 'Verifying...';

    const answer = this.querySelector('[name="answer"]').value.trim();
    const params = new URLSearchParams({ action: 'verifyAnswer', email: forgotEmail, answer });

    const url = typeof campusFitUrl === 'function' ? campusFitUrl('api/recovery') : '/CampusActivities/api/recovery';
    fetch(url, { method: 'POST', body: params })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                forgotResetToken = data.resetToken;
                document.getElementById('forgot-step2').style.display = 'none';
                document.getElementById('forgot-step3').style.display = '';
            } else {
                msgEl.textContent = data.message;
                msgEl.className = 'msg error';
            }
        })
        .catch(() => {
            msgEl.textContent = 'Connection error. Please try again.';
            msgEl.className = 'msg error';
        })
        .finally(() => {
            btn.disabled = false;
            btn.textContent = 'Verify Answer';
        });
});

// Step 3 — set new password
document.getElementById('forgotPasswordForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const btn = document.getElementById('forgotPasswordBtn');
    const msgEl = document.getElementById('forgotStep3Msg');
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
    btn.textContent = 'Saving...';

    const params = new URLSearchParams({ action: 'resetWithSecurityToken', token: forgotResetToken, newPassword });

    const url = typeof campusFitUrl === 'function' ? campusFitUrl('api/recovery') : '/CampusActivities/api/recovery';
    fetch(url, { method: 'POST', body: params })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                msgEl.textContent = data.message;
                msgEl.className = 'msg success';
                setTimeout(() => { window.location.href = 'login.html'; }, 2000);
            } else {
                msgEl.textContent = data.message;
                msgEl.className = 'msg error';
                // Token may be expired — send user back to step 1
                if (data.message.includes('expired')) {
                    setTimeout(forgotReset, 2000);
                }
            }
        })
        .catch(() => {
            msgEl.textContent = 'Connection error. Please try again.';
            msgEl.className = 'msg error';
        })
        .finally(() => {
            btn.disabled = false;
            btn.textContent = 'Set New Password';
        });
});
