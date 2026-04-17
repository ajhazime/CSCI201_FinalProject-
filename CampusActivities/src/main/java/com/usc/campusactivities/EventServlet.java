package com.usc.campusactivities;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import com.google.gson.JsonObject;

public class EventServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        
        JsonObject jsonResponse = new JsonObject();
        
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "User not authenticated");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        String activityType = request.getParameter("activityType");
        String location = request.getParameter("location");
        String date = request.getParameter("date");
        String time = request.getParameter("time");
        int maxParticipants = Integer.parseInt(request.getParameter("maxParticipants"));

        Event event = new Event(0, activityType, location, date, time, maxParticipants, 0, user.getId());
        if (EventDAO.insertEvent(event)) {
            jsonResponse.addProperty("success", true);
            jsonResponse.addProperty("message", "Event created successfully");
            jsonResponse.add("event", new com.google.gson.Gson().toJsonTree(event));
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Failed to create event");
        }
        
        response.getWriter().write(jsonResponse.toString());
    }
}