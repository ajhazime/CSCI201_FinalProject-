(function cleanupAfterSignOut() {
    try {
        var params = new URLSearchParams(window.location.search);
        if (params.has('signedOut')) {
            sessionStorage.removeItem('user');
            localStorage.removeItem('user');
            params.delete('signedOut');
            var qs = params.toString();
            var url = window.location.pathname + (qs ? '?' + qs : '');
            window.history.replaceState({}, document.title, url);
        }
    } catch (e) {
        /* ignore */
    }
})();

document.getElementById('loginForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const params = new URLSearchParams(new FormData(this));
    fetch(campusFitUrl('login'), {
        method: 'POST',
        body: params,
        credentials: 'same-origin'
    })
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
