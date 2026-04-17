const user = JSON.parse(sessionStorage.getItem('user'));
if (!user) {
    window.location.href = 'login.html';
} else {
    document.getElementById('username').textContent = user.username;
}

document.getElementById('logoutLink').addEventListener('click', async function(e) {
    e.preventDefault();
    await fetch('logout');
    sessionStorage.removeItem('user');
    window.location.href = 'login.html';
});
