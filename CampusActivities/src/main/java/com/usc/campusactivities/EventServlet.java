package com.usc.campusactivities;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class EventServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        List<Event> events = EventDAO.getAllEvents();
        response.getWriter().write(new Gson().toJson(events));
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        JsonObject jsonResponse = new JsonObject();

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "User not authenticated");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        String activityType = request.getParameter("activityType");
        String location = request.getParameter("location");
        String date = request.getParameter("date");
        String startTime = request.getParameter("startTime");
        String endTime = request.getParameter("endTime");
        if (startTime == null || startTime.isBlank()) {
            startTime = request.getParameter("time");
        }

        if (isBlank(activityType) || isBlank(location) || isBlank(date) || isBlank(startTime) || isBlank(endTime)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Missing required event fields");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        int maxParticipants;
        try {
            maxParticipants = Integer.parseInt(request.getParameter("maxParticipants"));
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "maxParticipants must be a valid integer");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        if (maxParticipants <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "maxParticipants must be greater than 0");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        try {
            LocalTime parsedStart = LocalTime.parse(startTime);
            LocalTime parsedEnd = LocalTime.parse(endTime);
            if (!parsedStart.isBefore(parsedEnd)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "startTime must be before endTime");
                response.getWriter().write(jsonResponse.toString());
                return;
            }
        } catch (DateTimeParseException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "startTime and endTime must use HH:mm[:ss] format");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        if (!EventDAO.isApprovedLocation(location)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Location is not approved");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        // Host is added to participant list immediately.
        Event event = new Event(0, activityType, location, date, startTime, maxParticipants, 1, user.getId());
        event.setEndTime(endTime);

        if (EventDAO.insertEventWithHost(event)) {
            jsonResponse.addProperty("success", true);
            jsonResponse.addProperty("message", "Event created successfully");
            jsonResponse.add("event", new com.google.gson.Gson().toJsonTree(event));
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Failed to create event");
        }
        
        response.getWriter().write(jsonResponse.toString());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}