package com.usc.campusactivities;

import java.sql.*;
import java.util.*;

public class EventDAO {
    public static List<Event> getAllEvents() {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM events";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                events.add(new Event(rs.getInt("id"), rs.getString("activity_type"), rs.getString("location"),
                                     rs.getString("date"), rs.getString("time"), rs.getInt("max_participants"),
                                     rs.getInt("current_participants"), rs.getInt("creator_id")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return events;
    }

    public static boolean insertEvent(Event event) {
        String sql = "INSERT INTO events (activity_type, location, date, time, max_participants, current_participants, creator_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, event.getActivityType());
            stmt.setString(2, event.getLocation());
            stmt.setString(3, event.getDate());
            stmt.setString(4, event.getTime());
            stmt.setInt(5, event.getMaxParticipants());
            stmt.setInt(6, event.getCurrentParticipants());
            stmt.setInt(7, event.getCreatorId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Other methods
}