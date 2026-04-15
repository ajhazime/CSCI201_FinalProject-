package com.usc.campusactivities;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

public class EventServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        String activityType = request.getParameter("activityType");
        String location = request.getParameter("location");
        String date = request.getParameter("date");
        String time = request.getParameter("time");
        int maxParticipants = Integer.parseInt(request.getParameter("maxParticipants"));

        Event event = new Event(0, activityType, location, date, time, maxParticipants, 0, user.getId());
        if (EventDAO.insertEvent(event)) {
            response.sendRedirect("activities.jsp");
        } else {
            request.setAttribute("error", "Failed to create event");
            request.getRequestDispatcher("createEvent.jsp").forward(request, response);
        }
    }
}