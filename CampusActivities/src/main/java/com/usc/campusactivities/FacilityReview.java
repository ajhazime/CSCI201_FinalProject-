package com.usc.campusactivities;

public class FacilityReview {
    private int id;
    private int facilityId;
    private int userId;
    private int rating;
    private String review;
    private String createdAt;
    private String username;

    public FacilityReview() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getFacilityId() { return facilityId; }
    public void setFacilityId(int facilityId) { this.facilityId = facilityId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}