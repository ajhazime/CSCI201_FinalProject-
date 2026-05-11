package com.usc.campusactivities;

import com.google.gson.*;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

public class AvailabilityServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("[]");
            return;
        }

        JsonArray result = new JsonArray();
        String sql = "SELECT dayOfWeek, startTime, endTime FROM user_availability WHERE userID = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObject slot = new JsonObject();
                    slot.addProperty("day", rs.getString("dayOfWeek"));
                    slot.addProperty("startTime", rs.getTime("startTime").toString().substring(0, 5));
                    slot.addProperty("endTime", rs.getTime("endTime").toString().substring(0, 5));
                    result.add(slot);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("[]");
            return;
        }
        response.getWriter().write(result.toString());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"message\":\"Not authenticated\"}");
            return;
        }

        JsonObject json = new JsonObject();
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) sb.append(line);
            JsonArray slots = JsonParser.parseString(sb.toString()).getAsJsonArray();

            try (Connection conn = DBUtil.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM user_availability WHERE userID = ?")) {
                    del.setInt(1, user.getId());
                    del.executeUpdate();
                }
                if (slots.size() > 0) {
                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO user_availability (userID, dayOfWeek, startTime, endTime) VALUES (?, ?, ?, ?)")) {
                        for (JsonElement el : slots) {
                            JsonObject slot = el.getAsJsonObject();
                            ins.setInt(1, user.getId());
                            ins.setString(2, slot.get("day").getAsString());
                            ins.setString(3, slot.get("startTime").getAsString());
                            ins.setString(4, slot.get("endTime").getAsString());
                            ins.addBatch();
                        }
                        ins.executeBatch();
                    }
                }
                conn.commit();
            }
            json.addProperty("success", true);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            json.addProperty("success", false);
            json.addProperty("message", e.getMessage());
        }
        response.getWriter().write(json.toString());
    }
}
