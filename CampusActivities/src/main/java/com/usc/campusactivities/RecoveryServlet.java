package com.usc.campusactivities;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonObject;

public class RecoveryServlet extends HttpServlet {

    // In-memory tokens for security-question resets: token -> {userId, expiryMillis}
    private static final ConcurrentHashMap<String, long[]> secTokens = new ConcurrentHashMap<>();

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObject json = new JsonObject();
        String action = request.getParameter("action");

        if ("getQuestion".equals(action)) {
            handleGetQuestion(request, response, json);
            return;
        }
        if ("verifyAnswer".equals(action)) {
            handleVerifyAnswer(request, response, json);
            return;
        }
        if ("resetWithSecurityToken".equals(action)) {
            handleResetWithSecurityToken(request, response, json);
            return;
        }
        String email = request.getParameter("email");
        if (email == null || !email.trim().toLowerCase().endsWith("@usc.edu")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "A valid USC email address is required");
            response.getWriter().write(json.toString());
            return;
        }

        email = email.trim().toLowerCase();
        User user = UserDAO.getUserByEmail(email);

        if ("username".equals(action)) {
            if (user == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                json.addProperty("success", false);
                json.addProperty("message", "No account found with that email address");
            } else {
                json.addProperty("success", true);
                json.addProperty("username", user.getUsername());
                json.addProperty("message", "Your username is: " + user.getUsername());
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "Invalid action");
        }

        response.getWriter().write(json.toString());
    }

    private void handleGetQuestion(HttpServletRequest request, HttpServletResponse response, JsonObject json) throws IOException {
        String email = request.getParameter("email");
        if (email == null || email.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "Email is required");
            response.getWriter().write(json.toString());
            return;
        }

        String[] data = UserDAO.getSecurityData(email.trim());
        if (data == null) {
            json.addProperty("success", false);
            json.addProperty("message", "No account found with that email, or no security question is set.");
        } else {
            json.addProperty("success", true);
            json.addProperty("question", data[1]);
        }
        response.getWriter().write(json.toString());
    }

    private void handleVerifyAnswer(HttpServletRequest request, HttpServletResponse response, JsonObject json) throws IOException {
        String email = request.getParameter("email");
        String answer = request.getParameter("answer");

        if (email == null || email.trim().isEmpty() || answer == null || answer.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "Email and answer are required");
            response.getWriter().write(json.toString());
            return;
        }

        String[] data = UserDAO.getSecurityData(email.trim());
        if (data == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            json.addProperty("success", false);
            json.addProperty("message", "No account found with that email, or no security question is set.");
            response.getWriter().write(json.toString());
            return;
        }

        // Normalize answer: trim + lowercase before checking
        String normalizedAnswer = answer.trim().toLowerCase();
        boolean correct = PasswordUtil.checkPassword(normalizedAnswer, data[2]);

        if (!correct) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            json.addProperty("success", false);
            json.addProperty("message", "Incorrect answer. Please try again.");
        } else {
            // Issue a short-lived in-memory token (5 minutes)
            String token = UUID.randomUUID().toString().replace("-", "");
            long expiry = System.currentTimeMillis() + 5L * 60 * 1000;
            secTokens.put(token, new long[]{ Long.parseLong(data[0]), expiry });
            json.addProperty("success", true);
            json.addProperty("resetToken", token);
        }
        response.getWriter().write(json.toString());
    }

    private void handleResetWithSecurityToken(HttpServletRequest request, HttpServletResponse response, JsonObject json) throws IOException {
        String token = request.getParameter("token");
        String newPassword = request.getParameter("newPassword");

        if (token == null || token.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "Reset token is required");
            response.getWriter().write(json.toString());
            return;
        }
        if (newPassword == null || newPassword.length() < 12) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "New password must be at least 12 characters");
            response.getWriter().write(json.toString());
            return;
        }

        long[] entry = secTokens.get(token.trim());
        if (entry == null || System.currentTimeMillis() > entry[1]) {
            secTokens.remove(token.trim());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "Session expired. Please start over.");
            response.getWriter().write(json.toString());
            return;
        }

        int userId = (int) entry[0];
        secTokens.remove(token.trim());

        boolean updated = UserDAO.updatePassword(userId, PasswordUtil.hashPassword(newPassword));
        if (updated) {
            json.addProperty("success", true);
            json.addProperty("message", "Password reset successfully. You can now log in.");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            json.addProperty("success", false);
            json.addProperty("message", "Failed to reset password. Please try again.");
        }
        response.getWriter().write(json.toString());
    }

}
