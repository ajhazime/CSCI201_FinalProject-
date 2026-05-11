package com.usc.campusactivities;

import java.sql.Timestamp;

/** Attendance penalty appeal from a participant to the event host. */
public class AttendanceAppeal {
    private int id;
    private int eventId;
    private int appellantId;
    private String appellantUsername;
    private String message;
    private String status;
    private Timestamp createdAt;
    private Timestamp reviewedAt;
    /** Event fields for host inbox */
    private String activityType;
    private String location;
    private String date;
    private String time;

    public AttendanceAppeal() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getAppellantId() {
        return appellantId;
    }

    public void setAppellantId(int appellantId) {
        this.appellantId = appellantId;
    }

    public String getAppellantUsername() {
        return appellantUsername;
    }

    public void setAppellantUsername(String appellantUsername) {
        this.appellantUsername = appellantUsername;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
