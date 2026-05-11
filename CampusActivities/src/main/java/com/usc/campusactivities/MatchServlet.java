package com.usc.campusactivities;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class MatchServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JsonObject err = new JsonObject();
            err.addProperty("success", false);
            err.addProperty("message", "User not authenticated");
            response.getWriter().write(err.toString());
            return;
        }

        MatchingEngine engine = new MatchingEngine();
        List<MatchingEngine.UserMatch> matches = engine.generateMatches(user);

        JsonArray results = new JsonArray();

        for (MatchingEngine.UserMatch match : matches) {
            JsonObject entry = new JsonObject();
            entry.addProperty("userID", match.user.getId());
            entry.addProperty("username", match.user.getUsername());
            entry.addProperty("skillLevel", match.user.getSkillLevel());
            entry.addProperty("interests", match.user.getInterests());
            entry.addProperty("avgRating", match.user.getAvgRating());
            entry.addProperty("matchScore", Math.round(match.score / 100.0 * 100));
            results.add(entry);
        }

        response.getWriter().write(results.toString());
    }
}
