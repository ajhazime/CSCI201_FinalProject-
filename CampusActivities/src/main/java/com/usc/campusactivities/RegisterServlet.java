package com.usc.campusactivities;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class RegisterServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        JsonObject jsonResponse = new JsonObject();
        
        try {
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            String email = request.getParameter("email");
            String interests = request.getParameter("interests");
            String skillLevel = request.getParameter("skillLevel");

            System.out.println("=== Register Request ===");
            System.out.println("Username: " + username);
            System.out.println("Password length: " + (password != null ? password.length() : "null"));
            System.out.println("Email: " + email);
            System.out.println("Interests: " + interests);
            System.out.println("Skill Level: " + skillLevel);

            // Validate required fields are not null
            if (username == null || username.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Username is required");
                response.getWriter().write(jsonResponse.toString());
                return;
            }

            if (password == null || password.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Password is required");
                response.getWriter().write(jsonResponse.toString());
                return;
            }

            if (email == null || email.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Email is required");
                response.getWriter().write(jsonResponse.toString());
                return;
            }

            if (password.length() < 12) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Password must be at least 12 characters (you entered " + password.length() + ")");
                response.getWriter().write(jsonResponse.toString());
                return;
            }

            if (!email.endsWith("@usc.edu")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Must use USC email (@usc.edu)");
                response.getWriter().write(jsonResponse.toString());
                return;
            }

            String trimUser = username.trim();
            if (UserDAO.getUserByUsername(trimUser) != null) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "That username is already registered.");
                response.getWriter().write(jsonResponse.toString());
                return;
            }

            User user = new User(0, trimUser, password, email.trim(), interests != null ? interests : "", skillLevel != null ? skillLevel : "beginner", 0);

            System.out.println("Attempting to insert user: " + trimUser);
            if (UserDAO.insertUser(user)) {
                System.out.println("User inserted successfully");
                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("message", "Registration successful");
            } else {
                System.out.println("Failed to insert user");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                jsonResponse.addProperty("success", false);
                String detail = UserDAO.consumeInsertUserError();
                jsonResponse.addProperty(
                    "message",
                    detail != null && !detail.isEmpty()
                        ? detail
                        : "Registration failed — could not save to database.");
            }
            
            response.getWriter().write(jsonResponse.toString());
        } catch (Exception e) {
            System.out.println("Exception in RegisterServlet: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Server error: " + e.getMessage());
            response.getWriter().write(jsonResponse.toString());
        }
    }
}