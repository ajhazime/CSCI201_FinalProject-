package com.usc.campusactivities;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class LoginServlet extends HttpServlet {
    private static final String GUEST_USERNAME = "guest";
    private static final String GUEST_PASSWORD = "guest12345678";

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Gson gson = new Gson();
        JsonObject jsonResponse = new JsonObject();

        try {
            String username = request.getParameter("username");
            String password = request.getParameter("password");

            if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Username and password are required");
                response.getWriter().write(jsonResponse.toString());
                return;
            }

            String userKey = username.trim();
            User user = null;

            if (GUEST_USERNAME.equalsIgnoreCase(userKey) && GUEST_PASSWORD.equals(password)) {
                user = UserDAO.getUserByUsername(GUEST_USERNAME);
                if (user == null) {
                    User seed = new User(0, GUEST_USERNAME, GUEST_PASSWORD, "guest@usc.edu", "fitness,wellness", "beginner", 0);
                    UserDAO.insertUser(seed);
                    user = UserDAO.getUserByUsername(GUEST_USERNAME);
                }
                if (user != null && user.getPassword().equals(password)) {
                    HttpSession session = request.getSession();
                    session.setAttribute("user", user);
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Guest login successful");
                    jsonResponse.add("user", gson.toJsonTree(user));
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Guest login is not available (database seed missing).");
                }
            } else {
                user = UserDAO.getUserByUsername(userKey);
                if (user != null && user.getPassword().equals(password)) {
                    HttpSession session = request.getSession();
                    session.setAttribute("user", user);
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Login successful");
                    jsonResponse.add("user", gson.toJsonTree(user));
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Invalid credentials");
                }
            }

            response.getWriter().write(jsonResponse.toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Server error: " + e.getMessage());
            e.printStackTrace();
            response.getWriter().write(jsonResponse.toString());
        }
    }
}
