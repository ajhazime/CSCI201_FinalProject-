const user = JSON.parse(sessionStorage.getItem('user'));

if (!user) {
    window.location.href = 'login.html';
} else {
    const username = user.username || 'User';

    document.getElementById('username').textContent = username;

    const sidebarUsername = document.getElementById('sidebarUsername');
    if (sidebarUsername) {
        sidebarUsername.textContent = username;
    }

    const profileNameLarge = document.getElementById('profileNameLarge');
    if (profileNameLarge) {
        profileNameLarge.textContent = username;
    }

    const initials = getInitials(username);

    const sidebarInitials = document.getElementById('sidebarInitials');
    if (sidebarInitials) {
        sidebarInitials.textContent = initials;
    }

    const profileInitials = document.getElementById('profileInitials');
    if (profileInitials) {
        profileInitials.textContent = initials;
    }
}

function getInitials(name) {
    const parts = String(name).trim().split(/\s+/);

    if (parts.length === 1) {
        return parts[0].substring(0, 2).toUpperCase();
    }

    return (parts[0][0] + parts[1][0]).toUpperCase();
}