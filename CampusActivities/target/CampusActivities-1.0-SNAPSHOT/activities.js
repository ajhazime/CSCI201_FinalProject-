fetch('events')
    .then(res => res.json())
    .then(events => {
        const tbody = document.getElementById('eventsBody');
        const emptyState = document.getElementById('emptyState');
        if (!events || events.length === 0) {
            if (emptyState) {
                emptyState.style.display = 'block';
            }
            return;
        }
        events.forEach(e => {
            const row = document.createElement('tr');
            const endTime = e.endTime ? ` - ${e.endTime}` : '';
            row.innerHTML = `
                <td>${e.activityType}</td>
                <td>${e.location}</td>
                <td>${e.date}</td>
                <td>${e.time}${endTime}</td>
                <td>${e.currentParticipants}/${e.maxParticipants}</td>
            `;
            tbody.appendChild(row);
        });
    });
