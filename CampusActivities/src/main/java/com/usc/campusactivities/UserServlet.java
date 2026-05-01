package com.usc.campusactivities;

import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
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

