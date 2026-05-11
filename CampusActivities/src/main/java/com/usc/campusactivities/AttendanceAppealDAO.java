package com.usc.campusactivities;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttendanceAppealDAO {

    public enum SubmitAppealResult {
        SUCCESS,
        UNAUTHORIZED,
        EVENT_NOT_FOUND,
        NOT_ELIGIBLE,
        PENDING_EXISTS,
        GRANTED_EXISTS,
        DB_ERROR
    }

    public enum ReviewAppealResult {
        SUCCESS,
        UNAUTHORIZED,
        NOT_FOUND,
        NOT_PENDING,
        DB_ERROR
    }

    public static SubmitAppealResult submitAppeal(int appellantId, int eventId, String message) {
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.isEmpty()) {
            return SubmitAppealResult.NOT_ELIGIBLE;
        }

        Event ev = EventDAO.getEventById(eventId);
        if (ev == null) {
            return SubmitAppealResult.EVENT_NOT_FOUND;
        }

        String roleSql = "SELECT role, present FROM event_participants WHERE event_id = ? AND user_id = ?";
        String roleVal = null;
        Boolean presentVal = null;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(roleSql)) {
            ps.setInt(1, eventId);
            ps.setInt(2, appellantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return SubmitAppealResult.NOT_ELIGIBLE;
                }
                roleVal = rs.getString("role");
                Object po = rs.getObject("present");
                if (po != null) {
                    presentVal = ((Number) po).intValue() != 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return SubmitAppealResult.DB_ERROR;
        }

        if (roleVal == null || !"PARTICIPANT".equalsIgnoreCase(roleVal)) {
            return SubmitAppealResult.NOT_ELIGIBLE;
        }

        if (Boolean.TRUE.equals(presentVal)) {
            return SubmitAppealResult.NOT_ELIGIBLE;
        }

        if (!eventEnded(ev)) {
            return SubmitAppealResult.NOT_ELIGIBLE;
        }

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT attendance_finalized FROM events WHERE id = ?")) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !rs.getBoolean("attendance_finalized")) {
                    return SubmitAppealResult.NOT_ELIGIBLE;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return SubmitAppealResult.DB_ERROR;
        }

        Integer existingId = null;
        String existingStatus = null;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT id, status FROM attendance_appeals WHERE event_id = ? AND appellant_id = ?")) {
            ps.setInt(1, eventId);
            ps.setInt(2, appellantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    existingId = rs.getInt("id");
                    existingStatus = rs.getString("status");
                }
            }
            if ("PENDING".equalsIgnoreCase(existingStatus)) {
                return SubmitAppealResult.PENDING_EXISTS;
            }
            if ("GRANTED".equalsIgnoreCase(existingStatus)) {
                return SubmitAppealResult.GRANTED_EXISTS;
            }
            if (existingId != null && "DENIED".equalsIgnoreCase(existingStatus)) {
                try (PreparedStatement up = conn.prepareStatement(
                    "UPDATE attendance_appeals SET status = 'PENDING', message = ?, created_at = CURRENT_TIMESTAMP, "
                        + "reviewed_at = NULL WHERE id = ?")) {
                    up.setString(1, trimmed);
                    up.setInt(2, existingId);
                    return up.executeUpdate() == 1 ? SubmitAppealResult.SUCCESS : SubmitAppealResult.DB_ERROR;
                }
            }
            try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO attendance_appeals (event_id, appellant_id, message, status) VALUES (?, ?, ?, 'PENDING')")) {
                ins.setInt(1, eventId);
                ins.setInt(2, appellantId);
                ins.setString(3, trimmed);
                return ins.executeUpdate() == 1 ? SubmitAppealResult.SUCCESS : SubmitAppealResult.DB_ERROR;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return SubmitAppealResult.DB_ERROR;
        }
    }

    private static boolean eventEnded(Event ev) {
        if (ev == null || ev.getDate() == null || ev.getEndTime() == null) {
            return false;
        }
        try {
            return java.time.LocalDateTime.now().isAfter(
                java.time.LocalDateTime.of(
                    java.time.LocalDate.parse(ev.getDate()),
                    java.time.LocalTime.parse(ev.getEndTime())));
        } catch (Exception ex) {
            return false;
        }
    }

    public static List<AttendanceAppeal> getPendingAppealsForHost(int hostUserId) {
        List<AttendanceAppeal> out = new ArrayList<>();
        String sql = "SELECT a.id, a.event_id, a.appellant_id, a.message, a.status, a.created_at, a.reviewed_at, "
            + "u.username AS appellant_username, e.activity_type, e.location, e.date, e.time "
            + "FROM attendance_appeals a "
            + "JOIN events e ON e.id = a.event_id "
            + "JOIN users u ON u.id = a.appellant_id "
            + "WHERE e.creator_id = ? AND UPPER(a.status) = 'PENDING' "
            + "ORDER BY a.created_at ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, hostUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AttendanceAppeal a = new AttendanceAppeal();
                    a.setId(rs.getInt("id"));
                    a.setEventId(rs.getInt("event_id"));
                    a.setAppellantId(rs.getInt("appellant_id"));
                    a.setMessage(rs.getString("message"));
                    a.setStatus(rs.getString("status"));
                    a.setCreatedAt(rs.getTimestamp("created_at"));
                    a.setReviewedAt(rs.getTimestamp("reviewed_at"));
                    a.setAppellantUsername(rs.getString("appellant_username"));
                    a.setActivityType(rs.getString("activity_type"));
                    a.setLocation(rs.getString("location"));
                    a.setDate(rs.getString("date"));
                    a.setTime(rs.getString("time"));
                    out.add(a);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public static ReviewAppealResult reviewAppeal(int appealId, int hostUserId, boolean markPresent) {
        String lockSql = "SELECT a.id, a.event_id, a.appellant_id, a.status "
            + "FROM attendance_appeals a JOIN events e ON e.id = a.event_id "
            + "WHERE a.id = ? AND e.creator_id = ? FOR UPDATE";
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int eventId;
                int appellantId;
                String status;
                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setInt(1, appealId);
                    ps.setInt(2, hostUserId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return ReviewAppealResult.NOT_FOUND;
                        }
                        eventId = rs.getInt("event_id");
                        appellantId = rs.getInt("appellant_id");
                        status = rs.getString("status");
                    }
                }
                if (!"PENDING".equalsIgnoreCase(status)) {
                    conn.rollback();
                    return ReviewAppealResult.NOT_PENDING;
                }

                String newStatus = markPresent ? "GRANTED" : "DENIED";
                try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE attendance_appeals SET status = ?, reviewed_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, appealId);
                    if (ps.executeUpdate() != 1) {
                        conn.rollback();
                        return ReviewAppealResult.DB_ERROR;
                    }
                }

                if (markPresent) {
                    try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE event_participants SET present = 1 WHERE event_id = ? AND user_id = ? AND UPPER(role) = 'PARTICIPANT'")) {
                        ps.setInt(1, eventId);
                        ps.setInt(2, appellantId);
                        if (ps.executeUpdate() != 1) {
                            conn.rollback();
                            return ReviewAppealResult.DB_ERROR;
                        }
                    }
                    UserDAO.revertNoShowPenaltyAfterAppeal(appellantId);
                }

                conn.commit();
                return ReviewAppealResult.SUCCESS;
            } catch (SQLException ex) {
                conn.rollback();
                ex.printStackTrace();
                return ReviewAppealResult.DB_ERROR;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return ReviewAppealResult.DB_ERROR;
        }
    }
}
