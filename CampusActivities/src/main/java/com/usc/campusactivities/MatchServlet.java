package com.usc.campusactivities;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.List;
import com.google.gson.*;

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
        List<MatchingEngine.EventMatch> matches = engine.generateMatches(user);

        // Build a JSON array where each entry contains the event fields plus matchScore.
        JsonArray results = new JsonArray();
        Gson gson = new Gson();

        for (MatchingEngine.EventMatch match : matches) {
            // Convert the event to a JsonObject so we can add the score alongside it.
            JsonObject entry = gson.toJsonTree(match.event).getAsJsonObject();
            entry.addProperty("matchScore", match.score);
            results.add(entry);
        }

        response.getWriter().write(results.toString());
    }
}
