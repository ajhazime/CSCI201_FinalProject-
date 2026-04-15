<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Create Event</title>
</head>
<body>
    <h1>Create New Event</h1>
    <form action="createEvent" method="post">
        Activity Type: <input type="text" name="activityType" required><br>
        Location: <input type="text" name="location" required><br>
        Date: <input type="date" name="date" required><br>
        Time: <input type="time" name="time" required><br>
        Max Participants: <input type="number" name="maxParticipants" required><br>
        <input type="submit" value="Create">
    </form>
    <% if (request.getAttribute("error") != null) { %>
        <p style="color:red;"><%= request.getAttribute("error") %></p>
    <% } %>
</body>
</html>