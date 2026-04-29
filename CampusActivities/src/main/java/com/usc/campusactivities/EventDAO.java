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
                events.get(events.size() - 1).setEndTime(rs.getString("end_time"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return events;
    }

    public static boolean isApprovedLocation(String location) {
        String sql = "SELECT COUNT(*) FROM facilities WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, location);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean insertEventWithHost(Event event) {
        String eventSql = "INSERT INTO events (activity_type, location, date, time, end_time, max_participants, current_participants, creator_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String participantSql = "INSERT INTO event_participants (event_id, user_id, role) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement eventStmt = conn.prepareStatement(eventSql, Statement.RETURN_GENERATED_KEYS)) {
                eventStmt.setString(1, event.getActivityType());
                eventStmt.setString(2, event.getLocation());
                eventStmt.setString(3, event.getDate());
                eventStmt.setString(4, event.getTime());
                eventStmt.setString(5, event.getEndTime());
                eventStmt.setInt(6, event.getMaxParticipants());
                eventStmt.setInt(7, event.getCurrentParticipants());
                eventStmt.setInt(8, event.getCreatorId());
                if (eventStmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                int eventId;
                try (ResultSet rs = eventStmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    eventId = rs.getInt(1);
                }

                try (PreparedStatement participantStmt = conn.prepareStatement(participantSql)) {
                    participantStmt.setInt(1, eventId);
                    participantStmt.setInt(2, event.getCreatorId());
                    participantStmt.setString(3, "HOST");
                    if (participantStmt.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                conn.commit();
                event.setId(eventId);
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Other methods
}