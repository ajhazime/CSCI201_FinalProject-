package com.usc.campusactivities;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class EventServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EventDAO.finalizeAttendanceForEndedEvents();

        if ("/createEvent".equals(request.getServletPath())) {
            response.sendRedirect(request.getContextPath() + "/createEvent.html");
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String path = request.getServletPath();

        if ("/myEvents".equals(path)) {
            HttpSession session = request.getSession(false);
            User user = session == null ? null : (User) session.getAttribute("user");
            if (user == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("[]");
                return;
            }
            int excludeInviteeId = -1;
            try { excludeInviteeId = Integer.parseInt(request.getParameter("inviteeId")); } catch (Exception ignored) {}
            response.getWriter().write(new Gson().toJson(EventDAO.getEventsByCreator(user.getId(), excludeInviteeId)));
            return;
        }

        if ("/myInvites".equals(path)) {
            HttpSession session = request.getSession(false);
            User user = session == null ? null : (User) session.getAttribute("user");
            if (user == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("[]");
                return;
            }
            response.getWriter().write(new Gson().toJson(EventDAO.getPendingInvites(user.getId())));
            return;
        }

        if ("/upcomingEvents".equals(path)) {
            HttpSession upSession = request.getSession(false);
            User upUser = upSession == null ? null : (User) upSession.getAttribute("user");
            if (upUser == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("[]");
                return;
            }
            int limit = 8;
            try { limit = Integer.parseInt(request.getParameter("limit")); } catch (Exception ignored) {}
            response.getWriter().write(new Gson().toJson(EventDAO.getUpcomingEventsForUser(upUser.getId(), limit)));
            return;
        }

        if ("/eventAttendance".equals(path)) {
            eventAttendanceGet(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        User viewer = session == null ? null : (User) session.getAttribute("user");
        int viewerId = viewer == null ? -1 : viewer.getId();
        List<Event> events =
            viewer != null && viewerId > 0
                ? EventDAO.getCalendarEventsForViewer(viewerId)
                : EventDAO.getAllEventsForViewer(-1);

        response.getWriter().write(new Gson().toJson(events));
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EventDAO.finalizeAttendanceForEndedEvents();

        String servletPath = request.getServletPath();
        if ("/markAttendance".equals(servletPath)) {
            markAttendancePost(request, response);
            return;
        }
        if ("/sendInvite".equals(servletPath)) {
            sendInvite(request, response);
            return;
        }
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
        if ("/respondInvite".equals(servletPath)) {
            respondInvite(request, response);
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

        if (UserDAO.isEventActionBlocked(user.getId())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "You cannot create events while a no-show penalty is active.");
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
            int facilityCount = EventDAO.countFacilities();
            jsonResponse.addProperty(
                "message",
                "Location is not approved: '" + location + "'. (facilities rows: " + facilityCount + "). "
                    + "Make sure you're running schema.sql against the same DB as DBUtil ("
                    + DBUtil.class.getName() + ")."
            );
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        // Host is added to participant list immediately.
        Event event = new Event(0, activityType, location, date, startTime, maxParticipants, 1, user.getId());
        event.setEndTime(endTime);
        event.setPublic(request.getParameter("isPublic") != null);

        List<Integer> inviteeIds = parseInviteeIds(request.getParameter("inviteeIds"));

        if (UserDAO.isGuestAccount(user)) {
            if (!inviteeIds.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty(
                    "message",
                    "Invites are only available for registered USC accounts. Create an account (not guest) to invite others.");
                response.getWriter().write(jsonResponse.toString());
                return;
            }
        } else {
            try {
                inviteeIds = validateInviteeListForRegisteredHost(user.getId(), inviteeIds);
            } catch (IllegalArgumentException ex) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", ex.getMessage());
                response.getWriter().write(jsonResponse.toString());
                return;
            }
        }

        if (EventDAO.insertEventWithHostAndInvites(event, inviteeIds)) {
            jsonResponse.addProperty("success", true);
            jsonResponse.addProperty("message", "Event created successfully");
            jsonResponse.add("event", new com.google.gson.Gson().toJsonTree(event));
            jsonResponse.addProperty("invitesCreated", inviteeIds == null ? 0 : inviteeIds.size());
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.addProperty("success", false);
            String detail = EventDAO.consumeLastInsertEventError();
            jsonResponse.addProperty(
                "message",
                detail != null && !detail.isEmpty()
                    ? "Could not create event: " + detail
                    : "Failed to create event");
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
            case EVENT_ACTION_BLOCKED:
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "You cannot join events while a no-show penalty is active.");
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

    private void sendInvite(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
        if (UserDAO.isGuestAccount(user)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Invites are only available for registered USC accounts.");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        int eventId, inviteeId;
        try {
            eventId   = Integer.parseInt(request.getParameter("eventId"));
            inviteeId = Integer.parseInt(request.getParameter("inviteeId"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "eventId and inviteeId must be valid integers");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        User invitee = UserDAO.getUserById(inviteeId);
        if (invitee == null || UserDAO.isGuestAccount(invitee)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "You can only invite registered USC accounts.");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        EventDAO.SendInviteStatus status = EventDAO.sendInvite(eventId, user.getId(), inviteeId);
        switch (status) {
            case SUCCESS:
                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("message", "Invite sent successfully");
                break;
            case ALREADY_INVITED:
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "User has already been invited to this event");
                break;
            case EVENT_NOT_FOUND:
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Event not found");
                break;
            default:
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Failed to send invite");
                break;
        }
        response.getWriter().write(jsonResponse.toString());
    }

    private void respondInvite(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObject jsonResponse = new JsonObject();
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null || user.getId() <= 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "User not authenticated");
            response.getWriter().write(jsonResponse.toString());
            return;
        }
        if (UserDAO.isGuestAccount(user)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Guest accounts cannot accept or decline invitations. Please register.");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        int inviteId;
        try {
            inviteId = Integer.parseInt(request.getParameter("inviteId"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "inviteId must be a valid integer");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        String action = request.getParameter("action");
        if ("decline".equalsIgnoreCase(action)) {
            EventDAO.InviteRespondStatus st = EventDAO.declineInvite(inviteId, user.getId());
            switch (st) {
                case DECLINED_OK:
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Invitation declined");
                    break;
                case INVITE_NOT_FOUND:
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Invite not found");
                    break;
                default:
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Failed to decline invitation");
                    break;
            }
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        EventDAO.InviteRespondStatus acceptResult = EventDAO.acceptInvite(inviteId, user.getId());
        switch (acceptResult) {
            case ACCEPTED_OK:
                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("message", "Invitation accepted — you are joined to the event");
                break;
            case INVITE_NOT_FOUND:
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Invite not found");
                break;
            case NOT_PENDING:
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "This invitation was already used or withdrawn");
                break;
            case JOIN_FAILED_TIME_CONFLICT:
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Cannot accept — overlaps with another event you joined");
                break;
            case JOIN_FAILED_EVENT_FULL:
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Cannot accept — event is full");
                break;
            case JOIN_FAILED_EVENT_ACTION_BLOCKED:
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty(
                    "message",
                    "Cannot accept — you have an active event restriction (same as joining).");
                break;
            default:
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Failed to accept invitation");
                break;
        }

        response.getWriter().write(jsonResponse.toString());
    }

    private void eventAttendanceGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
            jsonResponse.addProperty("message", "eventId is required");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        Event ev = EventDAO.getEventById(eventId);
        if (ev == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Event not found");
            response.getWriter().write(jsonResponse.toString());
            return;
        }
        if (ev.getCreatorId() != user.getId()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Only the host can take attendance");
            response.getWriter().write(jsonResponse.toString());
            return;
        }
        if (!EventDAO.eventIsCurrentlyInProgress(ev)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Attendance can only be taken while the event is in progress");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        List<AttendanceParticipant> list = EventDAO.getAttendanceListForHost(user.getId(), eventId);
        if (list == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Could not load participants");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        jsonResponse.addProperty("success", true);
        jsonResponse.add("participants", new Gson().toJsonTree(list));
        response.getWriter().write(jsonResponse.toString());
    }

    private void markAttendancePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

        StringBuilder sb = new StringBuilder();
        try (java.io.BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        JsonObject body;
        try {
            body = new Gson().fromJson(sb.toString(), JsonObject.class);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Invalid JSON body");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        if (body == null || !body.has("eventId") || !body.has("marks")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "eventId and marks array required");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        int eventId = body.get("eventId").getAsInt();
        JsonArray marks = body.getAsJsonArray("marks");
        Map<Integer, Boolean> map = new HashMap<>();
        for (JsonElement el : marks) {
            JsonObject m = el.getAsJsonObject();
            map.put(m.get("userId").getAsInt(), m.get("present").getAsBoolean());
        }

        if (map.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "marks cannot be empty");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        if (EventDAO.saveAttendanceMarks(user.getId(), eventId, map)) {
            jsonResponse.addProperty("success", true);
            jsonResponse.addProperty("message", "Attendance saved");
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Could not save attendance (wrong event, not host, or not in progress)");
        }
        response.getWriter().write(jsonResponse.toString());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private List<Integer> parseInviteeIds(String raw) {
        List<Integer> ids = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return ids;
        }
        for (String part : raw.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) continue;
            try {
                ids.add(Integer.parseInt(s));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    /**
     * De-duplicate IDs, skip host, validate each invitee exists and is a registered (non-guest) account.
     */
    private static List<Integer> validateInviteeListForRegisteredHost(int hostUserId, List<Integer> rawIds) {
        if (rawIds == null || rawIds.isEmpty()) {
            return rawIds != null ? rawIds : new ArrayList<>();
        }
        LinkedHashSet<Integer> unique = new LinkedHashSet<>();
        for (Integer id : rawIds) {
            if (id == null) {
                continue;
            }
            if (id == hostUserId) {
                continue;
            }
            unique.add(id);
        }
        List<Integer> out = new ArrayList<>();
        for (Integer id : unique) {
            User invitee = UserDAO.getUserById(id);
            if (invitee == null) {
                throw new IllegalArgumentException("Invalid invitee user id: " + id);
            }
            if (UserDAO.isGuestAccount(invitee)) {
                throw new IllegalArgumentException("The guest account cannot be invited. Use a registered USC account.");
            }
            out.add(id);
        }
        return out;
    }
}