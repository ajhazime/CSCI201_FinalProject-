package com.usc.campusactivities;

import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private static final ThreadLocal<String> LAST_INSERT_USER_ERROR = new ThreadLocal<>();

    public static String consumeInsertUserError() {
        String msg = LAST_INSERT_USER_ERROR.get();
        LAST_INSERT_USER_ERROR.remove();
        return msg;
    }

    private static void attachRestrictionUntil(ResultSet rs, User user) {
        try {
            Timestamp ts = rs.getTimestamp("event_restriction_until");
            if (ts != null) {
                ZonedDateTime zdt = ts.toInstant().atZone(ZoneId.systemDefault());
                user.setEventRestrictionUntil(zdt.toOffsetDateTime().toString());
            }
        } catch (SQLException ignored) {
        }
    }

    public static boolean isEventActionBlocked(int userId) {
        String sql = "SELECT 1 FROM users WHERE id = ? AND event_restriction_until IS NOT NULL AND event_restriction_until > NOW()";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void extendEventRestrictionForNoShow(int userId) {
        String sql = "UPDATE users SET penalties = penalties + 1, "
            + "event_restriction_until = DATE_ADD(GREATEST(COALESCE(event_restriction_until, '1970-01-01 00:00:00'), NOW()), INTERVAL 12 HOUR) "
            + "WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static User mapUser(ResultSet rs) throws SQLException {
        User u = new User(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("password"),
            rs.getString("email"),
            rs.getString("interests"),
            rs.getString("skill_level"),
            rs.getInt("penalties")
        );
        u.setFirstName(rs.getString("firstName"));
        u.setLastName(rs.getString("lastName"));
        u.setPenaltyTracked(rs.getBoolean("penaltyTracked"));
        u.setAvgRating(rs.getDouble("avgRating"));
        u.setPreferredLocations(rs.getString("preferredLocations"));
        return u;
    }

    public static User getUserByUsername(String username) {
        String sql = "SELECT id, username, password, email, interests, skill_level, penalties, "
                   + "firstName, lastName, penaltyTracked, avgRating, preferredLocations "
                   + "FROM users WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static User getUserById(int userId) {
        String sql = "SELECT id, username, password, email, interests, skill_level, penalties, "
                   + "firstName, lastName, penaltyTracked, avgRating, preferredLocations, event_restriction_until "
                   + "FROM users WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User u = mapUser(rs);
                    attachRestrictionUntil(rs, u);
                    return u;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static User getUserByEmail(String email) {
        String sql = "SELECT id, username, password, email, interests, skill_level, penalties, "
                   + "firstName, lastName, penaltyTracked, avgRating, preferredLocations "
                   + "FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean insertUser(User user) {
        LAST_INSERT_USER_ERROR.remove();
        String sql = "INSERT INTO users (username, password, email, interests, skill_level, penalties) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername().trim());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getEmail().trim());
            stmt.setString(4, user.getInterests());
            stmt.setString(5, user.getSkillLevel());
            stmt.setInt(6, user.getPenalties());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            if (e.getErrorCode() == 1062) {
                LAST_INSERT_USER_ERROR.set("That username or email is already registered.");
            } else {
                LAST_INSERT_USER_ERROR.set(e.getMessage());
            }
        }
        return false;
    }

    public static boolean updatePassword(int userId, String hashedPassword) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashedPassword);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean updateUserProfile(int userId, String firstName, String lastName,
            String interests, String skillLevel, String preferredLocations, String profileVisibility) {
        String sql = "UPDATE users SET firstName = ?, lastName = ?, interests = ?, "
                   + "skill_level = ?, preferredLocations = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, interests);
            stmt.setString(4, skillLevel);
            stmt.setString(5, preferredLocations);
            stmt.setInt(6, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int countEventsJoined(int userId) {
        String sql = "SELECT COUNT(*) FROM event_participants WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean updateAvgRating(int userId) {
        String sql = "UPDATE users SET avgRating = "
                   + "(SELECT COALESCE(AVG(score), 0) FROM user_ratings WHERE ratee_id = ?) "
                   + "WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Returns [userId, question, answerHash] for the account with that email,
     * or null if the account doesn't exist or has no security question set.
     */
    public static String[] getSecurityData(String email) {
        String sql = "SELECT id, security_question, security_answer_hash FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String question = rs.getString("security_question");
                    String hash = rs.getString("security_answer_hash");
                    if (question == null || question.trim().isEmpty()) return null;
                    return new String[]{ String.valueOf(rs.getInt("id")), question, hash };
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean saveSecurityQuestion(int userId, String question, String hashedAnswer) {
        String sql = "UPDATE users SET security_question = ?, security_answer_hash = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, question);
            stmt.setString(2, hashedAnswer);
            stmt.setInt(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static List<User> searchUsers(String query, int limit, int excludeUserId) {
        List<User> users = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase();
        if (limit <= 0 || limit > 50) limit = 20;

        String sql = "SELECT id, username, email, interests, skill_level, penalties, "
                   + "firstName, lastName, penaltyTracked, avgRating, preferredLocations, '' AS password "
                   + "FROM users "
                   + "WHERE id <> ? AND (LOWER(username) LIKE ? OR LOWER(email) LIKE ?) "
                   + "ORDER BY username ASC LIMIT ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, excludeUserId);
            stmt.setString(2, "%" + q + "%");
            stmt.setString(3, "%" + q + "%");
            stmt.setInt(4, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public static List<User> suggestInviteUsers(int currentUserId, int limit) {
        if (limit <= 0 || limit > 50) limit = 10;

        User me = getUserById(currentUserId);
        String mySkill = me == null ? null : me.getSkillLevel();
        String firstInterest = null;
        if (me != null && me.getInterests() != null && !me.getInterests().trim().isEmpty()) {
            String[] parts = me.getInterests().split(",");
            if (parts.length > 0) firstInterest = parts[0].trim().toLowerCase();
        }

        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, email, interests, skill_level, penalties, "
                   + "firstName, lastName, penaltyTracked, avgRating, preferredLocations, '' AS password "
                   + "FROM users WHERE id <> ? "
                   + "ORDER BY "
                   + "  CASE WHEN ? IS NOT NULL AND LOWER(skill_level) = LOWER(?) THEN 0 ELSE 1 END, "
                   + "  CASE WHEN ? IS NOT NULL AND LOWER(interests) LIKE ? THEN 0 ELSE 1 END, "
                   + "  username ASC LIMIT ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentUserId);
            stmt.setString(2, mySkill);
            stmt.setString(3, mySkill);
            stmt.setString(4, firstInterest);
            stmt.setString(5, firstInterest == null ? null : "%" + firstInterest + "%");
            stmt.setInt(6, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Reverts one no-show penalty when a host confirms attendance on appeal.
     * When no penalty points remain, clears {@code event_restriction_until} so the
     * active event lockout and penalty display both go away.
     */
    public static void revertNoShowPenaltyAfterAppeal(int userId) {
        String sql = "UPDATE users SET penalties = GREATEST(penalties - 1, 0), "
            + "event_restriction_until = IF(GREATEST(penalties - 1, 0) = 0, NULL, event_restriction_until) "
            + "WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
