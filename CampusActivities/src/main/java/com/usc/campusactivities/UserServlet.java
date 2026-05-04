package com.usc.campusactivities;

import com.google.gson.Gson;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

public class UserServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User currentUser = session == null ? null : (User) session.getAttribute("user");
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("[]");
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam != null) {
            try {
                User target = UserDAO.getUserById(Integer.parseInt(idParam));
                if (target == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{}");
                } else {
                    response.getWriter().write(new Gson().toJson(target));
                }
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{}");
            }
            return;
        }

        String query = request.getParameter("query");
        int limit = 20;
        try {
            if (request.getParameter("limit") != null) {
                limit = Integer.parseInt(request.getParameter("limit"));
            }
        } catch (NumberFormatException ignored) {
        }

        List<User> users;
        if (query == null || query.trim().isEmpty()) {
            users = UserDAO.suggestInviteUsers(currentUser.getId(), limit);
        } else {
            users = UserDAO.searchUsers(query, limit, currentUser.getId());
        }
        response.getWriter().write(new Gson().toJson(users));
    }
}

