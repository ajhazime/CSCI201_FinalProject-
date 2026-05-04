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

        if ("true".equalsIgnoreCase(request.getParameter("me"))) {
            User fresh = UserDAO.getUserById(currentUser.getId());
            if (fresh == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{}");
                return;
            }
            fresh.setPassword("");
            response.getWriter().write(new Gson().toJson(fresh));
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
                    target.setPassword("");
                    target.setEventRestrictionUntil(null);
                    response.getWriter().write(new Gson().toJson(target));
                }
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{}");
            }
            return;
        }

        String query = request.getParameter("query");
        int li