package com.usc.campusactivities;

import java.sql.*;
import java.time.*;
import java.util.*;

public class MatchingEngine {

    // Pairs a matched user with their computed score for sorting and JSON response.
    public static class UserMatch {
        public final User user;
        public final int score;

        public UserMatch(User user, int score) {
            this.user = user;
            this.score = score;
        }
    }

    private static final Map<String, Integer> SKILL_RANK = new HashMap<>();
    static {
        SKILL_RANK.put("beginner", 0);
        SKILL_RANK.put("intermediate", 1);
        SKILL_RANK.put("competitive", 2);
    }

    /**
     * Interest overlap — 40 points max, hard filter on zero overlap.
     *
     * Counts how many of currentUser's interests appear in otherUser's interest list.
     * Score = (matchingCount / currentUserInterestCount) * 40.
     * Returns -1 if there is zero overlap (hard filter).
     */
    public int comparePreferences(User currentUser, User otherUser) {
        if (currentUser.getInterests() == null || currentUser.getInterests().isBlank()) return -1;
        if (otherUser.getInterests() == null || otherUser.getInterests().isBlank()) return -1;

        Set<String> otherInterests = new HashSet<>();
        for (String i : otherUser.getInterests().split(",")) {
            otherInterests.add(i.trim().toLowerCase());
        }

        String[] currentInterests = currentUser.getInterests().split(",");
        int matchCount = 0;
        for (String i : currentInterests) {
            if (otherInterests.contains(i.trim().toLowerCase())) matchCount++;
        }

        if (matchCount == 0) return -1;

        double fraction = (double) matchCount / currentInterests.length;
        return (int) Math.round(fraction * 40);
    }

    /**
     * Availability overlap — 30 points max.
     *
     * Queries user_availability for both users and finds time overlap across
     * matching days of the week.
     * Score = (totalOverlapMinutes / totalCurrentUserAvailableMinutes) * 30.
     * Returns 0 if either user has no availability set.
     */
    public int compareAvailability(User currentUser, User otherUser) {
        Map<String, List<long[]>> currentSlots = fetchAvailability(currentUser.getId());
        Map<String, List<long[]>> otherSlots   = fetchAvailability(otherUser.getId());

        if (currentSlots.isEmpty() || otherSlots.isEmpty()) return 0;

        long totalCurrentMinutes = 0;
        long totalOverlapMinutes = 0;

        for (Map.Entry<String, List<long[]>> entry : currentSlots.entrySet()) {
            String day = entry.getKey();
            List<long[]> currentDaySlots = entry.getValue();
            List<long[]> otherDaySlots   = otherSlots.getOrDefault(day, Collections.emptyList());

            for (long[] cSlot : currentDaySlots) {
                totalCurrentMinutes += cSlot[1] - cSlot[0];

                for (long[] oSlot : otherDaySlots) {
                    long overlapStart = Math.max(cSlot[0], oSlot[0]);
                    long overlapEnd   = Math.min(cSlot[1], oSlot[1]);
                    if (overlapStart < overlapEnd) {
                        totalOverlapMinutes += overlapEnd - overlapStart;
                    }
                }
            }
        }

        if (totalCurrentMinutes == 0) return 0;

        double fraction = Math.min(1.0, (double) totalOverlapMinutes / totalCurrentMinutes);
        return (int) Math.round(fraction * 30);
    }

    /**
     * Skill level proximity — 20 points max.
     *
     * 20 points for exact match, 10 for one level apart, 0 for two levels apart.
     */
    public int compareSkillLevel(User currentUser, User otherUser) {
        Integer currentRank = SKILL_RANK.get(normalize(currentUser.getSkillLevel()));
        Integer otherRank   = SKILL_RANK.get(normalize(otherUser.getSkillLevel()));

        if (currentRank == null || otherRank == null) return 0;

        int diff = Math.abs(currentRank - otherRank);
        if (diff == 0) return 20;
        if (diff == 1) return 10;
        return 0;
    }

    /**
     * Location preference — 10 points max.
     *
     * Awards 10 points if at least one preferred location appears in both users'
     * comma-separated preferredLocations strings.
     */
    public int compareLocation(User currentUser, User otherUser) {
        if (currentUser.getPreferredLocations() == null || currentUser.getPreferredLocations().isBlank()) return 0;
        if (otherUser.getPreferredLocations() == null || otherUser.getPreferredLocations().isBlank()) return 0;

        Set<String> otherLocations = new HashSet<>();
        for (String loc : otherUser.getPreferredLocations().split(",")) {
            otherLocations.add(loc.trim().toLowerCase());
        }

        for (String loc : currentUser.getPreferredLocations().split(",")) {
            if (otherLocations.contains(loc.trim().toLowerCase())) return 10;
        }
        return 0;
    }

    /**
     * Rating deduction — returns 0–10 points to subtract.
     *
     * Below 3.0 → subtract 10 pts; below 4.0 → subtract 5 pts; 4.0+ → no deduction.
     */
    public int getRatingDeduction(User otherUser) {
        double rating = otherUser.getAvgRating();
        if (rating < 3.0) return 10;
        if (rating < 4.0) return 5;
        return 0;
    }

    /**
     * Computes the total match score for one candidate user, applying all hard filters.
     *
     * Returns -1 if the candidate should be excluded entirely (zero interest overlap).
     * Otherwise returns a value in the range 0–100.
     */
    public int scoreUser(User currentUser, User otherUser) {
        int preferenceScore = comparePreferences(currentUser, otherUser);
        if (preferenceScore == -1) return -1;

        int total = preferenceScore
                + compareAvailability(currentUser, otherUser)
                + compareSkillLevel(currentUser, otherUser)
                + compareLocation(currentUser, otherUser)
                - getRatingDeduction(otherUser);

        return Math.max(total, 0);
    }

    /**
     * Fetches all eligible candidate users, scores each one, and returns a ranked list.
     *
     * Hard filters applied here:
     *   - The candidate is the current user themselves
     *   - The candidate has an active penalty (penaltyTracked = true)
     *   - Zero interest overlap with currentUser (handled in scoreUser)
     *   - Total score below 30
     */
    public List<UserMatch> generateMatches(User currentUser) {
        if (currentUser.getPenaltyTracked()) return Collections.emptyList();

        List<User> candidates = fetchAllOtherUsers(currentUser);
        List<UserMatch> results = new ArrayList<>();

        for (User candidate : candidates) {
            int score = scoreUser(currentUser, candidate);
            if (score >= 30) {
                results.add(new UserMatch(candidate, score));
            }
        }

        results.sort((a, b) -> Integer.compare(b.score, a.score));
        return results;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Queries all users except the current user and those with active penalties.
     */
    private List<User> fetchAllOtherUsers(User currentUser) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT userID, username, firstName, lastName, interests, skillLevel, "
                   + "       avgRating, preferredLocations, penaltyTracked "
                   + "FROM users "
                   + "WHERE userID != ? AND penaltyTracked = false";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUser.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("userID"));
                    u.setUsername(rs.getString("username"));
                    u.setFirstName(rs.getString("firstName"));
                    u.setLastName(rs.getString("lastName"));
                    u.setInterests(rs.getString("interests"));
                    u.setSkillLevel(rs.getString("skillLevel"));
                    u.setAvgRating(rs.getDouble("avgRating"));
                    u.setPreferredLocations(rs.getString("preferredLocations"));
                    u.setPenaltyTracked(rs.getBoolean("penaltyTracked"));
                    users.add(u);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return users;
    }

    /**
     * Returns a map of dayOfWeek → list of [startMinutes, endMinutes] slots
     * for the given user, where minutes are measured from midnight.
     */
    private Map<String, List<long[]>> fetchAvailability(int userId) {
        Map<String, List<long[]>> slots = new HashMap<>();
        String sql = "SELECT dayOfWeek, startTime, endTime FROM user_availability WHERE userID = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String day = rs.getString("dayOfWeek");
                    long start = rs.getTime("startTime").toLocalTime().toSecondOfDay() / 60;
                    long end   = rs.getTime("endTime").toLocalTime().toSecondOfDay() / 60;
                    slots.computeIfAbsent(day, k -> new ArrayList<>()).add(new long[]{start, end});
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return slots;
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
