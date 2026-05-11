package com.usc.campusactivities;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import com.google.gson.JsonObject;

public class RatingServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JsonObject json = new JsonObject();

        HttpSession session = request.getSession(false);
        User sessionUser = session == null ? null : (User) session.getAttribute("user");
        if (sessionUser == null || sessionUser.getId() == 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            json.addProperty("success", false);
            json.addProperty("message", "Please log in to rate users");
            response.getWriter().write(json.toString());
            return;
        }

        String rateeIdStr = request.getParameter("rateeId");
        String scoreStr = request.getParameter("score");
        if (rateeIdStr == null || scoreStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "rateeId and score are required");
            response.getWriter().write(json.toString());
            return;
        }

        int rateeId, score;
        try {
            rateeId = Integer.parseInt(rateeIdStr);
            score = Integer.parseInt(scoreStr);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "Invalid rateeId or score");
            response.getWriter().write(json.toString());
            return;
        }

        if (rateeId == sessionUser.getId()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "You cannot rate yourself");
            response.getWriter().write(json.toString());
            return;
        }

        if (score < 1 || score > 5) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "Score must be between 1 and 5");
            response.getWriter().write(json.toString());
            return;
        }

        User ratee = UserDAO.getUserById(rateeId);
        if (ratee == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            json.addProperty("success", false);
            json.addProperty("message", "User not found");
            response.getWriter().write(json.toString());
            return;
        }

        boolean saved = RatingDAO.upsertRating(sessionUser.getId(), rateeId, score);
        if (saved) {
            UserDAO.updateAvgRating(rateeId);
            User updated = UserDAO.getUserById(rateeId);
            json.addProperty("success", true);
            json.addProperty("message", "Rating submitted");
            json.addProperty("newAvgRating", updated != null ? updated.getAvgRating() : 0.0);
            json.addProperty("ratingCount", RatingDAO.getRatingCount(rateeId));
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            json.addProperty("success", false);
            json.addProperty("message", "Failed to save rating");
        }
        response.getWriter().write(json.toString());
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JsonObject json = new JsonObject();

        String userIdStr = request.getParameter("userId");
        if (userIdStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "userId is required");
            response.getWriter().write(json.toString());
            return;
        }

        int userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "Invalid userId");
            response.getWriter().write(json.toString());
            return;
        }

        User user = UserDAO.getUserById(userId);
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            json.addProperty("success", false);
            json.addProperty("message", "User not found");
            response.getWriter().write(json.toString());
            return;
        }

        int count = RatingDAO.getRatingCount(userId);

        HttpSession session = request.getSession(false);
        User sessionUser = session == null ? null : (User) session.getAttribute("user");
        int existingScore = 0;
        if (sessionUser != null && sessionUser.getId() != 0) {
            existingScore = RatingDAO.getExistingRating(sessionUser.getId(), userId);
        }

        json.addProperty("success", true);
        json.addProperty("avgRating", user.getAvgRating());
        json.addProperty("ratingCount", count);
        json.addProperty("yourRating", existingScore);
        response.getWriter().write(json.toString());
    }
}
