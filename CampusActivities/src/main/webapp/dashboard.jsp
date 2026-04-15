<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.usc.campusactivities.*" %>
<%
User user = (User) session.getAttribute("user");
if (user == null) {
    response.sendRedirect("login.jsp");
    return;
}
%>
<!DOCTYPE html>
<html>
<head>
    <title>Dashboard</title>
</head>
<body>
    <h1>Welcome <%= user.getUsername() %></h1>
    <a href="activities.jsp">Browse Activities</a>
    <a href="createEvent.jsp">Create Event</a>
    <a href="profile.jsp">Profile</a>
    <a href="logout">Logout</a>
</body>
</html>