package com.usc.campusactivities;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FacilityReviewDAO {

    public static boolean insertReview(int facilityId, int userId, int rating, String review) {
        String sql = "INSERT INTO facility_reviews (facility_id, user_id, rating, review) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, facilityId);
            stmt.setInt(2, userId);
            stmt.setInt(3, rating);
            stmt.setString(4, review);

            boolean success = stmt.executeUpdate() > 0;

            if (success) {
                updateFacilityAverage(facilityId);
            }

            return success;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static double getAverageRating(int facilityId) {
        String sql = "SELECT AVG(rating) FROM facility_reviews WHERE facility_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, facilityId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static List<FacilityReview> getReviews(int facilityId) {
        List<FacilityReview> reviews = new ArrayList<>();

        String sql = "SELECT fr.*, u.username FROM facility_reviews fr " +
                     "JOIN users u ON fr.user_id = u.id " +
                     "WHERE fr.facility_id = ? " +
                     "ORDER BY fr.created_at DESC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, facilityId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    FacilityReview r = new FacilityReview();

                    r.setId(rs.getInt("id"));
                    r.setFacilityId(rs.getInt("facility_id"));
                    r.setUserId(rs.getInt("user_id"));
                    r.setRating(rs.getInt("rating"));
                    r.setReview(rs.getString("review"));
                    r.setCreatedAt(rs.getString("created_at"));
                    r.setUsername(rs.getString("username"));

                    reviews.add(r);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return reviews;
    }

    private static void updateFacilityAverage(int facilityId) {
        double avg = getAverageRating(facilityId);

        String sql = "UPDATE facilities SET rating = ? WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, avg);
            stmt.setInt(2, facilityId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}