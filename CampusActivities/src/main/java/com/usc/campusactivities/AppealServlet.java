package com.usc.campusactivities;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class AppealServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EventDAO.finalizeAttendanceForEndedEvents();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObject json = new JsonObject();
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null || user.getId() == 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            json.addProperty("success", false);
            json.addProperty("message", "Please log in.");
            response.getWriter().write(json.toString());
            return;
        }

        String forHost = request.getParameter("forHost");
        if (!"true".equalsIgnoreCase(forHost)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "Unsupported query.");
            response.getWriter().write(json.toString());
            return;
        }

        List<AttendanceAppeal> appeals = AttendanceAppealDAO.getPendingAppealsForHost(user.getId());
        json.addProperty("success", true);
        json.add("appeals", new Gson().toJsonTree(appeals));
        response.getWriter().write(json.toString());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EventDAO.finalizeAttendanceForEndedEvents();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObject json = new JsonObject();
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null || user.getId() == 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            json.addProperty("success", false);
            json.addProperty("message", "Please log in.");
            response.getWriter().write(json.toString());
            return;
        }

        String path = request.getServletPath();
        if (path != null && path.endsWith("/review")) {
            handleReview(request, response, user, json);
            return;
        }

        int eventId;
        try {
            eventId = Integer.parseInt(request.getParameter("eventId"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "Invalid event.");
            response.getWriter().write(json.toString());
            return;
        }

        String message = request.getParameter("message");
        AttendanceAppealDAO.SubmitAppealResult result =
            AttendanceAppealDAO.submitAppeal(user.getId(), eventId, message);

        switch (result) {
            case SUCCESS:
                json.addProperty("success", true);
                json.addProperty("message", "Appeal submitted to the host.");
                break;
            case EVENT_NOT_FOUND:
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                json.addProperty("success", false);
                json.addProperty("message", "Event not found.");
                break;
            case NOT_ELIGIBLE:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json.addProperty("success", false);
                json.addProperty("message", "You cannot appeal this event (check attendance rules or add a message).");
                break;
            case PENDING_EXISTS:
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                json.addProperty("success", false);
                json.addProperty("message", "You already have a pending appeal for this event.");
                break;
            case GRANTED_EXISTS:
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                json.addProperty("success", false);
                json.addProperty("message", "This appeal was already resolved in your favor.");
                break;
            default:
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                json.addProperty("success", false);
                json.addProperty("message", "Could not submit appeal.");
        }
        response.getWriter().write(json.toString());
    }

    private void handleReview(HttpServletRequest request, HttpServletResponse response, User host, JsonObject json)
            throws IOException {
        int appealId;
        try {
            appealId = Integer.parseInt(request.getParameter("appealId"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "Invalid appeal.");
            response.getWriter().write(json.toString());
            return;
        }

        String decision = request.getParameter("decision");
        boolean markPresent = "present".equalsIgnoreCase(decision);

        AttendanceAppealDAO.ReviewAppealResult result =
            AttendanceAppealDAO.reviewAppeal(appealId, host.getId(), markPresent);

        switch (result) {
            case SUCCESS:
                json.addProperty("success", true);
                json.addProperty("message", markPresent ? "Attendance updated." : "Appeal denied.");
                break;
            case NOT_FOUND:
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                json.addProperty("success", false);
                json.addProperty("message", "Appeal not found or you are not the host.");
                break;
            case NOT_PENDING:
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                json.addProperty("success", false);
                json.addProperty("message", "This appeal was already reviewed.");
                break;
            default:
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                json.addProperty("success", false);
                json.addProperty("message", "Could not save review.");
        }
        response.getWriter().write(json.toString());
    }
}
