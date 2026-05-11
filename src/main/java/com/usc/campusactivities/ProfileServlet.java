package com.usc.campusactivities;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ProfileServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObject jsonResponse = new JsonObject();
        HttpSession session = request.getSession(false);
        User sessionUser = session == null ? null : (User) session.getAttribute("user");

        if (sessionUser == null || sessionUser.getId() == 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Please log in to view your profile");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        EventDAO.finalizeAttendanceForEndedEvents();

        User user = UserDAO.getUserById(sessionUser.getId());
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "User not found");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        int eventsJoined = UserDAO.countEventsJoined(user.getId());
        int ratingCount = RatingDAO.getRatingCount(user.getId());

        JsonObject userObj = (JsonObject) new Gson().toJsonTree(user);
        userObj.remove("password");
        jsonResponse.addProperty("success", true);
        jsonResponse.add("user", userObj);
        jsonResponse.addProperty("eventsJoined", eventsJoined);
        jsonResponse.addProperty("ratingCount", ratingCount);
        List<ProfileActivityItem> activityHistory = EventDAO.getProfileActivityHistory(user.getId());
        jsonResponse.add("activityHistory", new Gson().toJsonTree(activityHistory));
        response.getWriter().write(jsonResponse.toString());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObject jsonResponse = new JsonObject();
        HttpSession session = request.getSession(false);
        User sessionUser = session == null ? null : (User) session.getAttribute("user");

        if (sessionUser == null || sessionUser.getId() == 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Please log in to update your profile");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String interests = request.getParameter("interests");
        String skillLevel = request.getParameter("skillLevel");
        String preferredLocations = request.getParameter("preferredLocations");
        String profileVisibility = request.getParameter("profileVisibility");
        String securityQuestion = request.getParameter("securityQuestion");
        String securityAnswer = request.getParameter("securityAnswer");

        if (skillLevel == null || skillLevel.trim().isEmpty()) {
            skillLevel = sessionUser.getSkillLevel() != null ? sessionUser.getSkillLevel() : "beginner";
        }
        if (profileVisibility == null || profileVisibility.trim().isEmpty()) {
            profileVisibility = "show_interests";
        }

        boolean updated = UserDAO.updateUserProfile(
            sessionUser.getId(),
            firstName != null ? firstName.trim() : "",
            lastName != null ? lastName.trim() : "",
            interests != null ? interests.trim() : "",
            skillLevel.trim(),
            preferredLocations != null ? preferredLocations.trim() : "",
            profileVisibility.trim()
        );

        // Update security question if both fields are provided
        if (securityQuestion != null && !securityQuestion.trim().isEmpty()
                && securityAnswer != null && !securityAnswer.trim().isEmpty()) {
            String hashedAnswer = PasswordUtil.hashPassword(securityAnswer.trim().toLowerCase());
            UserDAO.saveSecurityQuestion(sessionUser.getId(), securityQuestion.trim(), hashedAnswer);
        }

        if (updated) {
            User refreshed = UserDAO.getUserById(sessionUser.getId());
            if (refreshed != null) {
                session.setAttribute("user", refreshed);
                JsonObject userObj = (JsonObject) new Gson().toJsonTree(refreshed);
                userObj.remove("password");
                jsonResponse.add("user", userObj);
            }
            jsonResponse.addProperty("success", true);
            jsonResponse.addProperty("message", "Profile updated successfully");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Failed to update profile");
        }

        response.getWriter().write(jsonResponse.toString());
    }
}
