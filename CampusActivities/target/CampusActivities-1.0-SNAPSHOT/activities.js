fetch('events')
    .then(res => res.json())
    .then(events => {
        const tbody = document.getElementById('eventsBody');
        events.forEach(e => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${e.activityType}</td>
                <td>${e.location}</td>
                <td>${e.date}</td>
                <td>${e.time}</td>
                <td>${e.currentParticipants}/${e.maxParticipants}</td>
            `;
            tbody.appendChild(row);
        });
    });
