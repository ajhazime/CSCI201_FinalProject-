package com.usc.campusactivities;

import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private static final ThreadLocal<String> LAST_INSERT_USER_ERROR = new ThreadLocal<>();

    /** Cleared after read. */
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
            // Column missing on older DBs without migration
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

    /**
     * Extends the no-event window by 12 hours from the later of "now" or the current restriction end,
     * and increments accumulated penalty points by 1.
     */
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

    public static User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User u = new User(rs.getInt("id"), rs.getString("username"), rs.getString("password"),
                    rs.getString("email"), rs.getString("interests"), rs.getString("skill_level"), rs.getInt("penalties"));
                attachRestrictionUntil(rs, u);
                return u;
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

    public static List<User> searchUsers(String query, int limit, int excludeUserId) {
        List<User> users = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase();
        if (limit <= 0 || limit > 50) {
            limit = 20;
        }

        String sql = "SELECT id, username, email, interests, skill_level, penalties "
                   + "FROM users "
                   + "WHERE id <> ? AND (LOWER(username) LIKE ? OR LOWER(email) LIKE ?) "
                   + "ORDER BY username ASC "
                   + "LIMIT ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, excludeUserId);
            stmt.setString(2, "%" + q + "%");
            stmt.setString(3, "%" + q + "%");
            stmt.setInt(4, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        "",
                        rs.getString("email"),
                        rs.getString("interests"),
                        rs.getString("skill_level"),
                        rs.getInt("penalties")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }

    public static User getUserById(int userId) {
        String sql = "SELECT id, username, password, email, interests, skill_level, penalties, event_restriction_until "
                   + "FROM users WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User u = new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                   