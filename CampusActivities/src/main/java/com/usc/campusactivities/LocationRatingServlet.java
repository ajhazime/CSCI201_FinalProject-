package com.usc.campusactivities;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class LocationRatingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Gson gson = new Gson();

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonArray facilitiesArray = new JsonArray();
        String sql = "SELECT id, name, description, rating FROM facilities";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int facilityId = rs.getInt("id");
                List<FacilityReview> reviews = FacilityReviewDAO.getReviews(facilityId);

                JsonObject facilityJson = new JsonObject();
                facilityJson.addProperty("id", facilityId);
                facilityJson.addProperty("name", rs.getString("name"));
                facilityJson.addProperty("averageRating", rs.getDouble("rating"));
                facilityJson.addProperty("reviewCount", reviews.size());
                facilityJson.add("reviews", gson.toJsonTree(reviews));

                facilitiesArray.add(facilityJson);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        response.getWriter().write(facilitiesArray.toString());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObject json = new JsonObject();
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            json.addProperty("success", false);
            json.addProperty("message", "User not logged in");
            response.getWriter().write(json.toString());
            return;
        }

        try {
            int facilityId = Integer.parseInt(request.getParameter("facilityId"));
            int rating = Integer.parseInt(request.getParameter("rating"));
            String review = request.getParameter("review");

            if (rating < 1 || rating > 5) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json.addProperty("success", false);
                json.addProperty("message", "Rating must be between 1 and 5");
                response.getWriter().write(json.toString());
                return;
            }

            boolean success = FacilityReviewDAO.insertReview(facilityId, user.getId(), rating, review);
            json.addProperty("success", success);
            if (!success) {
                json.addProperty("message", "Could not submit review");
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "Invalid parameters");
        }

        response.getWriter().write(json.toString());
    }
}
