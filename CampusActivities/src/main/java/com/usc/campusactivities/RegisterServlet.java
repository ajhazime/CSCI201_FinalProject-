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
        
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String email = request.getParameter("email");
        String interests = request.getParameter("interests");
        String skillLevel = request.getParameter("skillLevel");

        JsonObject jsonResponse = new JsonObject();

        if (password.length() < 12) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Password must be at least 12 characters");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        if (!email.endsWith("@usc.edu")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Must use USC email");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        User user = new User(0, username, password, email, interests, skillLevel, 0);
        if (UserDAO.insertUser(user)) {
            jsonResponse.addProperty("success", true);
            jsonResponse.addProperty("message", "Registration successful");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Registration failed");
        }
        
        response.getWriter().write(jsonResponse.toString());
    }
}