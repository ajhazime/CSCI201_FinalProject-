package com.usc.campusactivities;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class EventDAO {
    private static final int LEAVE_PENALTY_WINDOW_HOURS = 24; // X hours hook

    private static final ThreadLocal<String> LAST_INSERT_EVENT_ERROR = new ThreadLocal<>();

    /** Cleared after read; used to surface DB errors on event creation failure. */
    public static String consumeLastInsertEventError() {
        String msg = LAST_INSERT_EVENT_ERROR.get();
        LAST_INSERT_EVENT_ERROR.remove();
        return msg;
    }

    public enum JoinEventStatus {
        SUCCESS,
        EVENT_NOT_FOUND,
        ALREADY_JOINED,
        TIME_CONFLICT,
        EVENT_FULL,
        /** Active no-show penalty blocks joins */
        EVENT_ACTION_BLOCKED,
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

    /**
     * Public events. When {@code viewerUserId >= 0}, each event includes whether that user is in {@code event_participants}.
     * Use {@code viewerUserId == -1} to skip membership (all {@code currentUserJoined} false).
     */
    public static List<Event> getAllEventsForViewer(int viewerUserId) {
        List<Event> events = new ArrayList<>();
        boolean withMembership = viewerUserId >= 0;
        String sql = withMembership
            ? "SELECT e.*, (ep.user_id IS NOT NULL) AS user_joined, ep.present AS participant_present FROM events e "
                + "LEFT JOIN event_participants ep ON e.id = ep.event_id AND ep.user_id = ? "
                + "WHERE e.is_public = true"
            : "SELECT * FROM events WHERE is_public = true";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (withMembership) {
                stmt.setInt(1, viewerUserId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Event ev = new Event(rs.getInt("id"), rs.getString("activity_type"), rs.getString("location"),
                        rs.getString("date"), rs.getString("time"), rs.getInt("max_participants"),
                        rs.getInt("current_participants"), rs.getInt("creator_id"));
                    ev.setEndTime(rs.getString("end_time"));
                    ev.setPublic(rs.getBoolean("is_public"));
                    if (withMembership) {
                        ev.setCurrentUserJoined(rs.getBoolean("user_joined"));
                        Object po = rs.getObject("participant_present");
                        if (ev.isCurrentUserJoined()) {
                            if (po == null) {
                                ev.setParticipantPresent(null);
                            } else {
                                ev.setParticipantPresent(((Number) po).intValue() != 0);
                            }
                        } else {
                            ev.setParticipantPresent(null);
                        }
                    } else {
                        ev.setCurrentUserJoined(false);
                        ev.setParticipantPresent(null);
                    }
                    events.add(ev);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return events;
    }

    public static List<Event> getAllEvents() {
        return getAllEventsForViewer(-1);
    }

    /** Events the user is participating in that have not ended yet (includes hosting). */
    public static List<Event> getUpcomingEventsForUser(int userId, int limit) {
        if (limit <= 0 || limit > 25) {
            limit = 8;
        }
        List<Event> out = new ArrayList<>();
        String sql = "SELECT e.id, e.activity_type, e.location, e.date, e.time, e.end_time, e.max_participants, "
            + "e.current_participants, e.is_public, e.creator_id "
            + "FROM events e "
            + "INNER JOIN event_participants ep ON e.id = ep.event_id AND ep.user_id = ? "
            + "WHERE TIMESTAMP(e.date, e.end_time) > NOW() "
            + "ORDER BY e.date ASC, e.time ASC LIMIT ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Event ev = new Event(
                        rs.getInt("id"),
                        rs.getString("activity_type"),
                        rs.getString("location"),
                        rs.getString("date"),
                        rs.getString("time"),
                        rs.getInt("max_participants"),
                        rs.getInt("current_participants"),
                        rs.getInt("creator_id"));
                    ev.setEndTime(rs.getString("end_time"));
                    ev.setPublic(rs.getBoolean("is_public"));
                    out.add(ev);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    /**
     * After scheduled end, participants who were not marked present receive a 12h event lockout.
     * Idempotent per event via {@code attendance_finalized}.
     */
    public static void finalizeAttendanceForEndedEvents() {
        List<Integer> eventIds = new ArrayList<>();
        String findSql = "SELECT id FROM events WHERE attendance_finalized = 0 AND TIMESTAMP(date, end_time) < NOW()";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(findSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                eventIds.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        for (int eventId : eventIds) {
            try (Connection conn = DBUtil.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    String penalizeSql = "SELECT user_id FROM event_participants WHERE event_id = ? "
                        + "AND UPPER(role) = 'PARTICIPANT' AND (present IS NULL OR present = 0)";
                    try (PreparedStatement ps = conn.prepareStatement(penalizeSql)) {
                        ps.setInt(1, eventId);
                        try (ResultSet prs = ps.executeQuery()) {
                            while (prs.next()) {
                                UserDAO.extendEventRestrictionForNoShow(prs.getInt("user_id"));
                            }
                        }
                    }
                    try (PreparedStatement fin = conn.prepareStatement(
                        "UPDATE events SET attendance_finalized = 1 WHERE id = ?")) {
                        fin.setInt(1, eventId);
                        fin.executeUpdate();
                    }
                    conn.commit();
                } catch (SQLException ex) {
                    conn.rollback();
                    ex.printStackTrace();
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean eventIsCurrentlyInProgress(Event e) {
        if (e == null || e.getDate() == null || e.getTime() == null || e.getEndTime() == null) {
            return false;
        }
        try {
            LocalDate d = LocalDate.parse(e.getDate());
            LocalTime start = LocalTime.parse(e.getTime());
            LocalTime end = LocalTime.parse(e.getEndTime());
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDt = LocalDateTime.of(d, start);
            LocalDateTime endDt = LocalDateTime.of(d, end);
            return !now.isBefore(startDt) && now.isBefore(endDt);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * @return null if event missing; empty list if valid host/in-progress but no participants
     */
    public static List<AttendanceParticipant> getAttendanceListForHost(int hostUserId, int eventId) {
        Event e = getEventById(eventId);
        if (e == null || e.getCreatorId() != hostUserId || !eventIsCurrentlyInProgress(e)) {
            return null;
        }
        List<AttendanceParticipant> out = new ArrayList<>();
        String sql = "SELECT ep.user_id, u.username, ep.present FROM event_participants ep "
            + "JOIN users u ON u.id = ep.user_id WHERE ep.event_id = ? AND UPPER(ep.role) = 'PARTICIPANT' "
            + "ORDER BY u.username ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Boolean presentVal = null;
                    Object o = rs.getObject("present");
                    if (o != null) {
                        presentVal = ((Number) o).intValue() != 0;
                    }
                    out.add(new AttendanceParticipant(rs.getInt("user_id"), rs.getString("username"), presentVal));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
        return out;
    }

    public static boolean saveAttendanceMarks(int hostUserId, int eventId, Map<Integer, Boolean> marks) {
        if (marks == null || marks.isEmpty()) {
            return false;
        }
        Event ev = getEventById(eventId);
        if (ev == null || ev.getCreatorId() != hostUserId || !eventIsCurrentlyInProgress(ev)) {
            return false;
        }
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql = "UPDATE event_participants SET present = ? WHERE event_id = ? AND user_id = ? AND UPPER(role) = 'PARTICIPANT'";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Map.Entry<Integer, Boolean> en : marks.entrySet()) {
                        ps.setBoolean(1, en.getValue());
                        ps.setInt(2, eventId);
                        ps.setInt(3, en.getKey());
                        if (ps.executeUpdate() != 1) {
                            conn.rollback();
                            return false;
                        }
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                ex.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
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

    public static int countFacilities() {
        String sql = "SELECT COUNT(*) FROM facilities";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean insertEventWithHost(Event event) {
        return insertEventWithHostAndInvites(event, null);
    }

    public static boolean insertEventWithHostAndInvites(Event event, List<Integer> inviteeIds) {
        LAST_INSERT_EVENT_ERROR.remove();
        String eventSql = "INSERT INTO events (activity_type, location, date, time, end_time, max_participants, current_participants, is_public, creator_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String participantSql = "INSERT INTO event_participants (event_id, user_id, role) VALUES (?, ?, ?)";
        String inviteSql = "INSERT INTO event_invites (event_id, inviter_id, invitee_id, status) VALUES (?, ?, ?, 'PENDING')";
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
                eventStmt.setBoolean(8, event.isPublic());
                eventStmt.setInt(9, event.getCreatorId());
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

                if (inviteeIds != null && !inviteeIds.isEmpty()) {
                    try (PreparedStatement inviteStmt = conn.prepareStatement(inviteSql)) {
                        for (Integer inviteeId : inviteeIds) {
                            if (inviteeId == null) continue;
                            if (inviteeId == event.getCreatorId()) continue;
                            inviteStmt.setInt(1, eventId);
                            inviteStmt.setInt(2, event.getCreatorId());
                            inviteStmt.setInt(3, inviteeId);
                            inviteStmt.addBatch();
                        }
                        inviteStmt.executeBatch();
                    }
                }

                conn.commit();
                event.setId(eventId);
                return true;
            } catch (SQLException e) {
                conn.rollback();
                LAST_INSERT_EVENT_ERROR.set(e.getMessage());
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LAST_INSERT_EVENT_ERROR.set(e.getMessage());
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
                    event.setPublic(rs.getBoolean("is_public"));
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
        if (UserDAO.isEventActionBlocked(userId)) {
            return JoinEventStatus.EVENT_ACTION_BLOCKED;
        }

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

    public static List<Event> getEventsByCreator(int userId) {
        return getEventsByCreator(userId, -1);
    }

    public static List<Event> getEventsByCreator(int userId, int excludeInviteeId) {
        List<Event> events = new ArrayList<>();
        String sql = excludeInviteeId > 0
            ? "SELECT id, activity_type, location, date, time, end_time, max_participants, current_participants, creator_id "
            + "FROM events WHERE creator_id = ? "
            + "AND id NOT IN (SELECT event_id FROM event_invites WHERE invitee_id = ?) "
            + "ORDER BY date ASC, time ASC"
            : "SELECT id, activity_type, location, date, time, end_time, max_participants, current_participants, creator_id "
            + "FROM events WHERE creator_id = ? ORDER BY date ASC, time ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            if (excludeInviteeId > 0) stmt.setInt(2, excludeInviteeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Event e = new Event(rs.getInt("id"), rs.getString("activity_type"), rs.getString("location"),
                            rs.getString("date"), rs.getString("time"), rs.getInt("max_participants"),
                            rs.getInt("current_participants"), rs.getInt("creator_id"));
                    e.setEndTime(rs.getString("end_time"));
                    events.add(e);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return events;
    }

    public static class PendingInvite {
        public int inviteId;
        public int eventId;
        public String inviterName;
        public String activityType;
        public String location;
        public String date;
    }

    public static List<PendingInvite> getPendingInvites(int userId) {
        List<PendingInvite> invites = new ArrayList<>();
        String sql = "SELECT ei.id AS invite_id, ei.event_id, u.username AS inviter_name, " +
                     "e.activity_type, e.location, e.date " +
                     "FROM event_invites ei " +
                     "JOIN events e ON ei.event_id = e.id " +
                     "JOIN users u ON ei.inviter_id = u.id " +
                     "WHERE ei.invitee_id = ? AND ei.status = 'PENDING' " +
                     "ORDER BY ei.created_at DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PendingInvite inv = new PendingInvite();
                    inv.inviteId     = rs.getInt("invite_id");
                    inv.eventId      = rs.getInt("event_id");
                    inv.inviterName  = rs.getString("inviter_name");
                    inv.activityType = rs.getString("activity_type");
                    inv.location     = rs.getString("location");
                    inv.date         = rs.getString("date");
                    invites.add(inv);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return invites;
    }

    public enum RespondInviteStatus { SUCCESS, INVITE_NOT_FOUND, EVENT_FULL, TIME_CONFLICT, PENALTY_BLOCKED, DB_ERROR }

    public static RespondInviteStatus respondToInvite(int inviteId, int userId, boolean accept) {
        String fetchSql  = "SELECT event_id FROM event_invites WHERE id = ? AND invitee_id = ? AND status = 'PENDING'";
        String updateSql = "UPDATE event_invites SET status = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection()) {
            int eventId;
            try (PreparedStatement ps = conn.prepareStatement(fetchSql)) {
                ps.setInt(1, inviteId);
                ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return RespondInviteStatus.INVITE_NOT_FOUND;
                    eventId = rs.getInt("event_id");
                }
            }
            if (!accept) {
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, "DECLINED");
                    ps.setInt(2, inviteId);
                    ps.executeUpdate();
                }
                return RespondInviteStatus.SUCCESS;
            }
            JoinEventStatus joinStatus = joinEvent(userId, eventId);
            if (joinStatus == JoinEventStatus.SUCCESS || joinStatus == JoinEventStatus.ALREADY_JOINED) {
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, "ACCEPTED");
                    ps.setInt(2, inviteId);
                    ps.executeUpdate();
                }
                return RespondInviteStatus.SUCCESS;
            } else if (joinStatus == JoinEventStatus.EVENT_FULL) {
                return RespondInviteStatus.EVENT_FULL;
            } else if (joinStatus == JoinEventStatus.TIME_CONFLICT) {
                return RespondInviteStatus.TIME_CONFLICT;
            } else if (joinStatus == JoinEventStatus.EVENT_ACTION_BLOCKED) {
                return RespondInviteStatus.PENALTY_BLOCKED;
            } else {
                return RespondInviteStatus.DB_ERROR;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return RespondInviteStatus.DB_ERROR;
        }
    }

    public enum SendInviteStatus { SUCCESS, ALREADY_INVITED, EVENT_NOT_FOUND, DB_ERROR }

    public static SendInviteStatus sendInvite(int eventId, int inviterId, int inviteeId) {
        String checkEventSql = "SELECT 1 FROM events WHERE id = ?";
        String checkDupSql   = "SELECT 1 FROM event_invites WHERE event_id = ? AND invitee_id = ?";
        String insertSql     = "INSERT INTO event_invites (event_id, inviter_id, invitee_id, status) VALUES (?, ?, ?, 'PENDING')";
        try (Connection conn = DBUtil.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(checkEventSql)) {
                ps.setInt(1, eventId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return SendInviteStatus.EVENT_NOT_FOUND;
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(checkDupSql)) {
                ps.setInt(1, eventId);
                ps.setInt(2, inviteeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return SendInviteStatus.ALREADY_INVITED;
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, eventId);
                ps.setInt(2, inviterId);
                ps.setInt(3, inviteeId);
                if (ps.executeUpdate() == 0) return SendInviteStatus.DB_ERROR;
            }
            return SendInviteStatus.SUCCESS;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return SendInviteStatus.DB_ERROR;
        }
    }

    // Other methods
}