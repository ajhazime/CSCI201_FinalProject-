package com.usc.campusactivities;

import com.google.gson.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.util.List;

public class StatsServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{}");
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("eventsJoined", countEventsJoined(user.getId()));
        json.addProperty("matchesFound", countMatches(user));
        json.addProperty("reviewsCount", countReviews(user.getId()));
        response.getWriter().write(json.toString());
    }

    private int countEventsJoined(int userId) {
        String sql = "SELECT COUNT(*) FROM event_participants WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int countMatches(User user) {
        try {
            List<MatchingEngine.UserMatch> matches = new MatchingEngine().generateMatches(user);
            return matches.size();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private int countReviews(int userId) {
        String sql = "SELECT COUNT(*) FROM facility_reviews WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            // Table may not exist yet — return 0 silently
        }
        return 0;
    }
}
