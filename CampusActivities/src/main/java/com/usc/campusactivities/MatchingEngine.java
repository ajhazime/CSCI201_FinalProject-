package com.usc.campusactivities;

import java.sql.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class MatchingEngine {

    // Pairs an event with its computed match score for sorting and JSON response.
    public static class EventMatch {
        public final Event event;
        public final int score;

        public EventMatch(Event event, int score) {
            this.event = event;
            this.score = score;
        }
    }

    private static final Map<String, Integer> SKILL_RANK = new HashMap<>();
    static {
        SKILL_RANK.put("beginner", 0);
        SKILL_RANK.put("intermediate", 1);
        SKILL_RANK.put("advanced", 2);
    }

    /**
     * Activity/Interest match — 40 points max, hard filter on zero overlap.
     *
     * Compares the user's comma-separated interests against the event's activityType.
     * Returns -1 (hard filter) if no overlap exists at all, 40 if the event's
     * activityType is found in the user's interest list.
     */
    public int comparePreferences(User user, Event event) {
        if (user.getInterests() == null || user.getInterests().isBlank()) return -1;
        if (event.getActivityType() == null) return -1;

        String eventActivity = event.getActivityType().toLowerCase().trim();
        Set<String> interests = new HashSet<>();
        for (String interest : user.getInterests().split(",")) {
            interests.add(interest.trim().toLowerCase());
        }

        // Hard filter: zero overlap means this event is irrelevant to the user.
        return interests.contains(eventActivity) ? 40 : -1;
    }

    /**
     * Availability overlap — 30 points max.
     *
     * Score = (overlapMinutes / eventDurationMinutes) * 30, capped at 30.
     * Overlap is summed across all of the user's availability slots on the
     * event's day of week, so a user with two adjacent slots isn't penalised.
     */
    public int compareAvailability(User user, Event event) {
        if (event.getDate() == null || event.getTime() == null || event.getEndTime() == null) return 0;

        LocalDate eventDate;
        try {
            eventDate = LocalDate.parse(event.getDate()); // expects YYYY-MM-DD
        } catch (Exception e) {
            return 0;
        }

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("[H:mm][HH:mm]");
        LocalTime eventStart, eventEnd;
        try {
            eventStart = LocalTime.parse(event.getTime(), timeFmt);
            eventEnd   = LocalTime.parse(event.getEndTime(), timeFmt);
        } catch (Exception e) {
            return 0;
        }

        long eventDuration = Duration.between(eventStart, eventEnd).toMinutes();
        if (eventDuration <= 0) return 0;

        // Map Java DayOfWeek to the full English name stored in user_availability.
        String dayName = eventDate.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT startTime, endTime FROM user_availability "
                       + "WHERE userID = ? AND dayOfWeek = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, user.getId());
                ps.setString(2, dayName);
                try (ResultSet rs = ps.executeQuery()) {
                    long totalOverlap = 0;
                    while (rs.next()) {
                        LocalTime slotStart = rs.getTime("startTime").toLocalTime();
                        LocalTime slotEnd   = rs.getTime("endTime").toLocalTime();

                        // Overlap = intersection of [eventStart, eventEnd] and [slotStart, slotEnd].
                        LocalTime overlapStart = eventStart.isAfter(slotStart) ? eventStart : slotStart;
                        LocalTime overlapEnd   = eventEnd.isBefore(slotEnd)   ? eventEnd   : slotEnd;

                        if (overlapStart.isBefore(overlapEnd)) {
                            totalOverlap += Duration.between(overlapStart, overlapEnd).toMinutes();
                        }
                    }

                    // Clamp fraction to 1.0 in case slots overlap each other.
                    double fraction = Math.min(1.0, (double) totalOverlap / eventDuration);
                    return (int) Math.round(fraction * 30);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Skill level proximity — 20 points max.
     *
     * 20 points for exact match, 10 for one level apart (beginner↔intermediate
     * or intermediate↔advanced), 0 for two levels apart (beginner↔advanced).
     */
    public int compareSkillLevel(User user, Event event) {
        Integer userRank  = SKILL_RANK.get(normalize(user.getSkillLevel()));
        Integer eventRank = SKILL_RANK.get(normalize(event.getSkillLevel()));

        if (userRank == null || eventRank == null) return 0;

        int diff = Math.abs(userRank - eventRank);
        if (diff == 0) return 20;
        if (diff == 1) return 10;
        return 0;
    }

    /**
     * Location preference — 10 points max.
     *
     * Awards bonus points when the event's location name contains a keyword
     * that matches one of the user's interests, suggesting the venue suits their
     * preferred activity type (e.g., a basketball fan at a "Basketball Court").
     */
    public int compareLocation(User user, Event event) {
        if (user.getInterests() == null || event.getLocation() == null) return 0;

        String locationLower = event.getLocation().toLowerCase();
        for (String interest : user.getInterests().split(",")) {
            if (locationLower.contains(interest.trim().toLowerCase())) {
                return 10;
            }
        }
        return 0;
    }

    /**
     * Rating deduction — returns 0–10 points to subtract.
     *
     * Queries the host's avgRating from the users table. Hosts with low ratings
     * are penalized to protect users from poor experiences.
     * Below 3.0 → subtract 10 pts; below 4.0 → subtract 5 pts; 4.0+ → no deduction.
     */
    public int getRatingDeduction(int hostId) {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT avgRating FROM users WHERE userID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, hostId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        double rating = rs.getDouble("avgRating");
                        if (rating < 3.0) return 10;
                        if (rating < 4.0) return 5;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Computes the total match score for one event, applying all hard filters.
     *
     * Returns -1 if the event should be excluded entirely (hard-filtered).
     * Otherwise returns a value in the range 0–100 (before the 30-point cutoff
     * that generateMatches enforces).
     */
    public int scoreEvent(User user, Event event) {
        // Hard filter: interest overlap (comparePreferences returns -1 if no match).
        int preferenceScore = comparePreferences(user, event);
        if (preferenceScore == -1) return -1;

        int total = preferenceScore
                + compareAvailability(user, event)
                + compareSkillLevel(user, event)
                + compareLocation(user, event)
                - getRatingDeduction(event.getCreatorId());

        return Math.max(total, 0);
    }

    /**
     * Fetches all eligible events, scores each one, and returns a ranked list.
     *
     * Hard filters applied here:
     *   - User is already registered for the event
     *   - Event is full (currentParticipants >= maxParticipants)
     *   - User has an active penalty (penalties > 0)
     *   - Event start time has already passed
     *   - Activity type has zero overlap with user interests (handled in scoreEvent)
     *   - Total score below 30
     */
    public List<EventMatch> generateMatches(User user) {
        // Active penalty is a hard stop — no matches for penalised users.
        if (user.getPenalties() > 0) return Collections.emptyList();

        List<Event> candidates = fetchEligibleEvents(user);
        List<EventMatch> results = new ArrayList<>();

        for (Event event : candidates) {
            int score = scoreEvent(user, event);
            if (score >= 30) {
                results.add(new EventMatch(event, score));
            }
        }

        // Sort descending by score so the best matches appear first.
        results.sort((a, b) -> Integer.compare(b.score, a.score));
        return results;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Queries events that pass the structural hard filters: not full, in the
     * future, and not already registered by this user. Activity and score
     * filters are applied afterwards in generateMatches / scoreEvent.
     */
    private List<Event> fetchEligibleEvents(User user) {
        List<Event> events = new ArrayList<>();
        String sql =
            "SELECT e.eventID, e.activityType, l.name AS location, e.date, e.time, e.endTime, "
          + "       e.maxParticipants, e.currentParticipants, e.hostID, "
          + "       e.skillLevel, e.eventName "
          + "FROM events e "
          + "JOIN locations l ON e.locationID = l.locationID "
          + "WHERE e.currentParticipants < e.maxParticipants "
          + "  AND TIMESTAMP(e.date, e.time) > NOW() "
          + "  AND e.eventID NOT IN ("
          + "      SELECT eventID FROM registrations WHERE userID = ?"
          + "  )";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Event e = new Event();
                    e.setId(rs.getInt("eventID"));
                    e.setActivityType(rs.getString("activityType"));
                    e.setLocation(rs.getString("location"));
                    e.setDate(rs.getString("date"));
                    e.setTime(rs.getString("time"));
                    e.setMaxParticipants(rs.getInt("maxParticipants"));
                    e.setCurrentParticipants(rs.getInt("currentParticipants"));
                    e.setCreatorId(rs.getInt("hostID"));
                    e.setEndTime(rs.getString("endTime"));
                    e.setSkillLevel(rs.getString("skillLevel"));
                    e.setEventName(rs.getString("eventName"));
                    events.add(e);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return events;
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
