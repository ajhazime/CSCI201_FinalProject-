package com.usc.campusactivities;

public class Event {
    private int id;
    private String activityType;
    private String location;
    private String date;
    private String time;
    private int maxParticipants;
    private int currentParticipants;
    private int creatorId;

    public Event() {}

    public Event(int id, String activityType, String location, String date, String time, int maxParticipants, int currentParticipants, int creatorId) {
        this.id = id;
        this.activityType = activityType;
        this.location = location;
        this.date = date;
        this.time = time;
        this.maxParticipants = maxParticipants;
        this.currentParticipants = currentParticipants;
        this.creatorId = creatorId;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }

    public int getCurrentParticipants() { return currentParticipants; }
    public void setCurrentParticipants(int currentParticipants) { this.currentParticipants = currentParticipants; }

    public int getCreatorId() { return creatorId; }
    public void setCreatorId(int creatorId) { this.creatorId = creatorId; }
}
