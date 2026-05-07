package com.usc.campusactivities;

import java.sql.*;

public class RatingDAO {

    public static boolean upsertRating(int raterId, int rateeId, int score) {
        String sql = "INSERT INTO user_ratings (rater_id, ratee_id, score) VALUES (?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE score = VALUES(score), created_at = CURRENT_TIMESTAMP";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, raterId);
            stmt.setInt(2, rateeId);
            stmt.setInt(3, score);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int getRatingCount(int rateeId) {
        String sql = "SELECT COUNT(*) FROM user_ratings WHERE ratee_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, rateeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getExistingRating(int raterId, int rateeId) {
        String sql = "SELECT score FROM user_ratings WHERE rater_id = ? AND ratee_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, raterId);
            stmt.setInt(2, rateeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("score");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
