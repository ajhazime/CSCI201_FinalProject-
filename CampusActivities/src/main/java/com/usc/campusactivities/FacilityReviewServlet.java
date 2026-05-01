package com.usc.campusactivities;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

@WebServlet("/facility-reviews")
public class FacilityReviewServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private Gson gson = new Gson();

    // Guests and logged-in users can view reviews
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObject json = new JsonObject();

        try {
            int facilityId = Integer.parseInt(request.getParameter("facilityId"));

            List<FacilityReview> reviews = FacilityReviewDAO.getReviews(facilityId);
            double averageRating = FacilityReviewDAO.getAverageRating(facilityId);

            json.addProperty("success", true);
            json.addProperty("facilityId", facilityId);
            json.addProperty("averageRating", averageRating);
            json.add("reviews", gson.toJsonTree(reviews));

        } catch (Exception e) {
            json.addProperty("success", false);
            json.addProperty("message", "Error fetching facility reviews");
        }

        response.getWriter().write(json.toString());
    }

    // Only logged-in users can leave reviews
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

            boolean success = FacilityReviewDAO.insertReview(
                    facilityId,
                    user.getId(),
                    rating,
                    review
            );

            json.addProperty("success", success);

            if (success) {
                json.addProperty("message", "Review submitted successfully");
                json.addProperty("averageRating", FacilityReviewDAO.getAverageRating(facilityId));
            } else {
                json.addProperty("message", "Could not submit review");
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "Error submitting review");
        }

        response.getWriter().write(json.toString());
    }
}