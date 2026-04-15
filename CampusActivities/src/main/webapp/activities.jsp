<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.usc.campusactivities.*, java.util.*" %>
<!DOCTYPE html>
<html>
<head>
    <title>Activities</title>
</head>
<body>
    <h1>Campus Activities</h1>
    <table border="1">
        <tr><th>Activity</th><th>Location</th><th>Date</th><th>Time</th><th>Spots</th></tr>
        <%
        List<Event> events = EventDAO.getAllEvents();
        for (Event e : events) {
        %>
            <tr>
                <td><%= e.getActivityType() %></td>
                <td><%= e.getLocation() %></td>
                <td><%= e.getDate() %></td>
                <td><%= e.getTime() %></td>
                <td><%= e.getCurrentParticipants() %>/<%= e.getMaxParticipants() %></td>
            </tr>
        <%
        }
        %>
    </table>
    <a href="createEvent.jsp">Create Event</a>
</body>
</html>