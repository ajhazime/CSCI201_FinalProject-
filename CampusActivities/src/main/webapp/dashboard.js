const user = JSON.parse(sessionStorage.getItem('user'));
if (!user) {
    window.location.href = 'login.html';
} else {
    document.getElementById('username').textContent = user.username;
}

document.getElementById('logoutLink').addEventListener('click', function(e) {
    e.preventDefault();
    fetch('/CampusActivities/logout')
        .then(() => {
            sessionStorage.removeItem('user');
            window.location.href = 'login.html';
        });
});
