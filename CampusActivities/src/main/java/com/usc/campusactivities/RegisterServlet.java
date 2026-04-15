package com.usc.campusactivities;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

public class RegisterServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String email = request.getParameter("email");
        String interests = request.getParameter("interests");
        String skillLevel = request.getParameter("skillLevel");

        if (password.length() < 12) {
            request.setAttribute("error", "Password must be at least 12 characters");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        if (!email.endsWith("@usc.edu")) {
            request.setAttribute("error", "Must use USC email");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        User user = new User(0, username, password, email, interests, skillLevel, 0);
        if (UserDAO.insertUser(user)) {
            response.sendRedirect("login.jsp");
        } else {
            request.setAttribute("error", "Registration failed");
            request.getRequestDispatcher("register.jsp").forward(request, response);
        }
    }
}