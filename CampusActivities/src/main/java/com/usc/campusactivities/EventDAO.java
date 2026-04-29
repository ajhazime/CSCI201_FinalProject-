package com.usc.campusactivities;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class EventDAO {
    private static final int LEAVE_PENALTY_WINDOW_HOURS = 24; // X hours hook

    public enum JoinEventStatus {
        SUCCESS,
        EVENT_NOT_FOUND,
        ALREADY_JOINED,
        TIME_CONFLICT,
        EVENT_FULL,
        NOT_PARTICIPANT,
        EVENT_CANCELLED,
        DB_ERROR
    }

    public enum CancelEventStatus {
        SUCCESS,
        EVENT_NOT_FOUND,
        FORBIDDEN,
        DB_ERROR
    }

    public static class LeaveEventResult {
        private final JoinEventStatus status;
        private final boolean penaltyApplied;

        public LeaveEventResult(JoinEventStatus status, boolean penaltyApplied) {
            this.status = status;
            this.penaltyApplied = penaltyApplied;
        }

        public JoinEventStatus getStatus() {
            return status;
        }

        public boolean isPenaltyApplied() {
            return penaltyApplied;
        }
    }

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

    public static Event getEventById(int eventId) {
        String sql = "SELECT * FROM events WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Event event = new Event(
                        rs.getInt("id"),
                        rs.getString("activity_type"),
                        rs.getString("location"),
                        rs.getString("date"),
                        rs.getString("time"),
                        rs.getInt("max_participants"),
                        rs.getInt("current_participants"),
                        rs.getInt("creator_id")
                    );
                    event.setEndTime(rs.getString("end_time"));
                    return event;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Overlap rule: (startA < endB) AND (endA > startB)
    public static boolean checkTimeConflict(int userId, Event newEvent) {
        try (Connection conn = DBUtil.getConnection()) {
            return checkTimeConflict(conn, userId, newEvent, -1);
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

    public static JoinEventStatus joinEvent(int userId, int eventId) {
        String eventSql = "SELECT id, date, time, end_time, max_participants, current_participants, creator_id, activity_type, location "
                        + "FROM events WHERE id = ? FOR UPDATE";
        String alreadyJoinedSql = "SELECT 1 FROM event_participants WHERE event_id = ? AND user_id = ? LIMIT 1";
        String insertParticipantSql = "INSERT INTO event_participants (event_id, user_id, role) VALUES (?, ?, 'PARTICIPANT')";
        String incrementSql = "UPDATE events SET current_participants = current_participants + 1 WHERE id = ?";

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement eventStmt = conn.prepareStatement(eventSql)) {
                eventStmt.setInt(1, eventId);
                try (ResultSet rs = eventStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return JoinEventStatus.EVENT_NOT_FOUND;
                    }

                    Event newEvent = new Event(
                        rs.getInt("id"),
                        rs.getString("activity_type"),
                        rs.getString("location"),
                        rs.getString("date"),
                        rs.getString("time"),
                        rs.getInt("max_participants"),
                        rs.getInt("current_participants"),
                        rs.getInt("creator_id")
                    );
                    newEvent.setEndTime(rs.getString("end_time"));

                    try (PreparedStatement alreadyJoinedStmt = conn.prepareStatement(alreadyJoinedSql)) {
                        alreadyJoinedStmt.setInt(1, eventId);
                        alreadyJoinedStmt.setInt(2, userId);
                        try (ResultSet joinedRs = alreadyJoinedStmt.executeQuery()) {
                            if (joinedRs.next()) {
                                conn.rollback();
                                return JoinEventStatus.ALREADY_JOINED;
                            }
                        }
                    }

                    if (checkTimeConflict(conn, userId, newEvent, eventId)) {
                        conn.rollback();
                        return JoinEventStatus.TIME_CONFLICT;
                    }

                    if (newEvent.getCurrentParticipants() >= newEvent.getMaxParticipants()) {
                        conn.rollback();
                        return JoinEventStatus.EVENT_FULL;
                    }

                    try (PreparedStatement insertStmt = conn.prepareStatement(insertParticipantSql);
                         PreparedStatement incrementStmt = conn.prepareStatement(incrementSql)) {
                        insertStmt.setInt(1, eventId);
                        insertStmt.setInt(2, userId);
                        if (insertStmt.executeUpdate() == 0) {
                            conn.rollback();
                            return JoinEventStatus.DB_ERROR;
                        }

                        incrementStmt.setInt(1, eventId);
                        if (incrementStmt.executeUpdate() == 0) {
                            conn.rollback();
                            return JoinEventStatus.DB_ERROR;
                        }
                    }
                }
            }
            conn.commit();
            return JoinEventStatus.SUCCESS;
        } catch (SQLException e) {
            e.printStackTrace();
            return JoinEventStatus.DB_ERROR;
        }
    }

    public static LeaveEventResult leaveEvent(int userId, int eventId) {
        String eventSql = "SELECT id, date, time, current_participants FROM events WHERE id = ? FOR UPDATE";
        String membershipSql = "SELECT role FROM event_participants WHERE event_id = ? AND user_id = ? LIMIT 1";
        String deleteSql = "DELETE FROM event_participants WHERE event_id = ? AND user_id = ?";
        String decrementSql = "UPDATE events SET current_participants = GREATEST(current_participants - 1, 0) WHERE id = ?";
        String deleteEventSql = "DELETE FROM events WHERE id = ?";

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            String eventDate = null;
            String eventStartTime = null;
            try (PreparedStatement eventStmt = conn.prepareStatement(eventSql)) {
                eventStmt.setInt(1, eventId);
                try (ResultSet eventRs = eventStmt.executeQuery()) {
                    if (!eventRs.next()) {
                        conn.rollback();
                        return new LeaveEventResult(JoinEventStatus.EVENT_NOT_FOUND, false);
                    }
                    eventDate = eventRs.getString("date");
                    eventStartTime = eventRs.getString("time");
                }

                try (PreparedStatement membershipStmt = conn.prepareStatement(membershipSql)) {
                    membershipStmt.setInt(1, eventId);
                    membershipStmt.setInt(2, userId);
                    try (ResultSet memberRs = membershipStmt.executeQuery()) {
                        if (!memberRs.next()) {
                            conn.rollback();
                            return new LeaveEventResult(JoinEventStatus.NOT_PARTICIPANT, false);
                        }
                        String role = memberRs.getString("role");
                        if ("HOST".equalsIgnoreCase(role)) {
                            try (PreparedStatement deleteEventStmt = conn.prepareStatement(deleteEventSql)) {
                                deleteEventStmt.setInt(1, eventId);
                                if (deleteEventStmt.executeUpdate() == 0) {
                                    conn.rollback();
                                    return new LeaveEventResult(JoinEventStatus.DB_ERROR, false);
                                }
                            }
                            conn.commit();
                            return new LeaveEventResult(JoinEventStatus.EVENT_CANCELLED, false);
                        }
                    }
                }

                boolean penaltyApplied = isWithinPenaltyWindow(eventDate, eventStartTime, LEAVE_PENALTY_WINDOW_HOURS);
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
                     PreparedStatement decrementStmt = conn.prepareStatement(decrementSql)) {
                    deleteStmt.setInt(1, eventId);
                    deleteStmt.setInt(2, userId);
                    if (deleteStmt.executeUpdate() == 0) {
                        conn.rollback();
                        return new LeaveEventResult(JoinEventStatus.DB_ERROR, false);
                    }

                    decrementStmt.setInt(1, eventId);
                    if (decrementStmt.executeUpdate() == 0) {
                        conn.rollback();
                        return new LeaveEventResult(JoinEventStatus.DB_ERROR, false);
                    }
                }

                conn.commit();
                return new LeaveEventResult(JoinEventStatus.SUCCESS, penaltyApplied);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new LeaveEventResult(JoinEventStatus.DB_ERROR, false);
        }
    }

    public static CancelEventStatus cancelEvent(int eventId, int requestingUserId) {
        String lockEventSql = "SELECT creator_id FROM events WHERE id = ? FOR UPDATE";
        String participantSql = "SELECT user_id FROM event_participants WHERE event_id = ?";
        String deleteEventSql = "DELETE FROM events WHERE id = ?";

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement lockStmt = conn.prepareStatement(lockEventSql)) {
                lockStmt.setInt(1, eventId);
                try (ResultSet rs = lockStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return CancelEventStatus.EVENT_NOT_FOUND;
                    }
                    int hostId = rs.getInt("creator_id");
                    if (hostId != requestingUserId) {
                        conn.rollback();
                        return CancelEventStatus.FORBIDDEN;
                    }
                }
            }

            List<Integer> participantIds = new ArrayList<>();
            try (PreparedStatement participantStmt = conn.prepareStatement(participantSql)) {
                participantStmt.setInt(1, eventId);
                try (ResultSet participants = participantStmt.executeQuery()) {
                    while (participants.next()) {
                        participantIds.add(participants.getInt("user_id"));
                    }
                }
            }

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteEventSql)) {
                deleteStmt.setInt(1, eventId);
                if (deleteStmt.executeUpdate() == 0) {
                    conn.rollback();
                    return CancelEventStatus.DB_ERROR;
                }
            }

            conn.commit();
            for (Integer participantId : participantIds) {
                System.out.println("Notification: event " + eventId + " cancelled; notifying user " + participantId);
            }
            return CancelEventStatus.SUCCESS;
        } catch (SQLException e) {
            e.printStackTrace();
            return CancelEventStatus.DB_ERROR;
        }
    }

    private static boolean checkTimeConflict(Connection conn, int userId, Event newEvent, int targetEventId) throws SQLException {
        String sql = "SELECT 1 "
                   + "FROM event_participants ep "
                   + "JOIN events e ON e.id = ep.event_id "
                   + "WHERE ep.user_id = ? "
                   + "AND e.id <> ? "
                   + "AND e.date = ? "
                   + "AND (? < e.end_time) "
                   + "AND (? > e.time) "
                   + "LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, targetEventId);
            stmt.setDate(3, java.sql.Date.valueOf(newEvent.getDate()));
            stmt.setTime(4, Time.valueOf(newEvent.getTime()));
            stmt.setTime(5, Time.valueOf(newEvent.getEndTime()));

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean isWithinPenaltyWindow(String date, String startTime, int windowHours) {
        try {
            LocalDate eventDate = LocalDate.parse(date);
            LocalTime eventStart = LocalTime.parse(startTime);
            LocalDateTime eventStartDateTime = LocalDateTime.of(eventDate, eventStart);
            LocalDateTime now = LocalDateTime.now();

            return now.isBefore(eventStartDateTime) && !now.isBefore(eventStartDateTime.minusHours(windowHours));
        } catch (Exception e) {
            return false;
        }
    }

    // Other methods
}