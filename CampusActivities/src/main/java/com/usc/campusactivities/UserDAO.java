package com.usc.campusactivities;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    public static User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("username"), rs.getString("password"),
                                rs.getString("email"), rs.getString("interests"), rs.getString("skill_level"), rs.getInt("penalties"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean insertUser(User user) {
        String sql = "INSERT INTO users (username, password, email, interests, skill_level, penalties) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getInterests());
            stmt.setString(5, user.getSkillLevel());
            stmt.setInt(6, user.getPenalties());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
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
        String sql = "SELECT id, username, password, email, interests, skill_level, penalties "
                   + "FROM users WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("interests"),
                        rs.getString("skill_level"),
                        rs.getInt("penalties")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<User> suggestInviteUsers(int currentUserId, int limit) {
        if (limit <= 0 || limit > 50) {
            limit = 10;
        }

        User me = getUserById(currentUserId);
        String mySkill = me == null ? null : me.getSkillLevel();
        String firstInterest = null;
        if (me != null && me.getInterests() != null && !me.getInterests().trim().isEmpty()) {
            String[] parts = me.getInterests().split(",");
            if (parts.length > 0) {
                firstInterest = parts[0].trim().toLowerCase();
            }
        }

        List<User> users = new ArrayList<>();

        String sql = "SELECT id, username, email, interests, skill_level, penalties "
                   + "FROM users "
                   + "WHERE id <> ? "
                   + "ORDER BY "
                   + "  CASE WHEN ? IS NOT NULL AND LOWER(skill_level) = LOWER(?) THEN 0 ELSE 1 END, "
                   + "  CASE WHEN ? IS NOT NULL AND LOWER(interests) LIKE ? THEN 0 ELSE 1 END, "
                   + "  username ASC "
                   + "LIMIT ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentUserId);
            stmt.setString(2, mySkill);
            stmt.setString(3, mySkill);
            stmt.setString(4, firstInterest);
            stmt.setString(5, firstInterest == null ? null : "%" + firstInterest + "%");
            stmt.setInt(6, limit);

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

    // Other methods as needed
}