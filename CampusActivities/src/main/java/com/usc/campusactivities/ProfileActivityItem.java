package com.usc.campusactivities;

/**
 * One row for the profile activity history list (hosted or joined events).
 */
public class ProfileActivityItem {
    private int eventId;
    private String activityType;
    private String location;
    private String date;
    private String time;
    private String endTime;
    private int creatorId;
    private String role;
    /** For participants: true/false/null (null = not marked). Host rows typically null. */
    private Boolean present;
    private boolean eventEnded;
    private boolean attendanceFinalized;
    private Integer appealId;
    /** PENDING, GRANTED, DENIED, or null */
    private String appealStatus;
    private boolean canAppeal;

    public ProfileActivityItem() {}

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
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

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getPresent() {
        return present;
    }

    public void setPresent(Boolean present) {
        this.present = present;
    }

    public boolean isEventEnded() {
        return eventEnded;
    }

    public void setEventEnded(boolean eventEnded) {
        this.eventEnded = eventEnded;
    }

    public boolean isAttendanceFinalized() {
        return attendanceFinalized;
    }

    public void setAttendanceFinalized(boolean attendanceFinalized) {
        this.attendanceFinalized = attendanceFinalized;
    }

    public Integer getAppealId() {
        return appealId;
    }

    public void setAppealId(Integer appealId) {
        this.appealId = appealId;
    }

    public String getAppealStatus() {
        return appealStatus;
    }

    public void setAppealStatus(String appealStatus) {
        this.appealStatus = appealStatus;
    }

    public boolean isCanAppeal() {
        return canAppeal;
    }

    public void setCanAppeal(boolean canAppeal) {
        this.canAppeal = canAppeal;
    }
}
