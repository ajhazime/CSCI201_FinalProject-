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
        if ("/createEvent".equals(request.getServletPath())) {
            response.sendRedirect(request.getContextPath() + "/createEvent.html");
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        List<Event> events = EventDAO.getAllEvents();
        response.getWriter().write(new Gson().toJson(events));
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String servletPath = request.getServletPath();
        if ("/joinEvent".equals(servletPath)) {
            joinEvent(request, response);
            return;
        }
        if ("/leaveEvent".equals(servletPath)) {
            leaveEvent(request, response);
            return;
        }
        if ("/cancelEvent".equals(servletPath)) {
            cancelEvent(request, response);
            return;
        }

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

    private void joinEvent(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObject jsonResponse = new JsonObject();
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "User not authenticated");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        int eventId;
        try {
            eventId = Integer.parseInt(request.getParameter("eventId"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "eventId must be a valid integer");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        EventDAO.JoinEventStatus status = EventDAO.joinEvent(user.getId(), eventId);
        switch (status) {
            case SUCCESS:
                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("message", "Joined event successfully");
                break;
            case EVENT_NOT_FOUND:
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Event not found");
                break;
            case ALREADY_JOINED:
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "User already joined this event");
                break;
            case TIME_CONFLICT:
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Cannot join overlapping event");
                break;
            case EVENT_FULL:
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Event is full");
                break;
            default:
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Failed to join event");
                break;
        }

        response.getWriter().write(jsonResponse.toString());
    }

    private void leaveEvent(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObject jsonResponse = new JsonObject();
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "User not authenticated");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        int eventId;
        try {
            eventId = Integer.parseInt(request.getParameter("eventId"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "eventId must be a valid integer");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        EventDAO.LeaveEventResult result = EventDAO.leaveEvent(user.getId(), eventId);
        EventDAO.JoinEventStatus status = result.getStatus();
        switch (status) {
            case SUCCESS:
                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("message", "Left event successfully");
                jsonResponse.addProperty("penaltyApplied", result.isPenaltyApplied());
                break;
            case EVENT_NOT_FOUND:
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Event not found");
                jsonResponse.addProperty("penaltyApplied", false);
                break;
            case NOT_PARTICIPANT:
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "User is not a participant in this event");
                jsonResponse.addProperty("penaltyApplied", false);
                break;
            case EVENT_CANCELLED:
                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("message", "Host left, so the event was cancelled");
                jsonResponse.addProperty("penaltyApplied", false);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Failed to leave event");
                jsonResponse.addProperty("penaltyApplied", false);
                break;
        }

        response.getWriter().write(jsonResponse.toString());
    }

    private void cancelEvent(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObject jsonResponse = new JsonObject();
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "User not authenticated");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        int eventId;
        try {
            eventId = Integer.parseInt(request.getParameter("eventId"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "eventId must be a valid integer");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        EventDAO.CancelEventStatus status = EventDAO.cancelEvent(eventId, user.getId());
        switch (status) {
            case SUCCESS:
                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("message", "Event cancelled successfully");
                break;
            case EVENT_NOT_FOUND:
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Event not found");
                break;
            case FORBIDDEN:
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Only the host can cancel this event");
                break;
            default:
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Failed to cancel event");
                break;
        }

        response.getWriter().write(jsonResponse.toString());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}