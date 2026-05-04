package com.usc.campusactivities;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public class Event {
    private int id;
    private String eventName;
    private String activityType;
    private String location;
    private String date;
    private String time;
    private String endTime;
    private int maxParticipants;
    private int currentParticipants;
    private int creatorId;
    private boolean isPublic = true;
    /** Set when listing events for the current viewer (GET /events). */
    private boolean currentUserJoined;
    /** Present flag from event_participants for the viewer; null if not joined or unset. */
    private Boolean participantPresent;

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

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }

    public int getCurrentParticipants() { return currentParticipants; }
    public void setCurrentParticipants(int currentParticipants) { this.currentParticipants = currentParticipants; }

    public int getCreatorId() { return creatorId; }
    public void setCreatorId(int creatorId) { this.creatorId = creatorId; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public boolean isCurrentUserJoined() { return currentUserJoined; }
    public void setCurrentUserJoined(boolean currentUserJoined) { this.currentUserJoined = currentUserJoined; }

    public Boolean getParticipantPresent() { return participantPresent; }
    public void setParticipantPresent(Boolean participantPresent) { this.participantPresent = participantPresent; }

    public boolean isFull() {
        return currentParticipants >= maxParticipants;
    }

    public int remainingSpots() {
        return Math.max(maxParticipants - currentParticipants, 0);
    }

    public static boolean hasTimeConflict(List<Event> userEvents, Event newEvent) {
        if (userEvents == null || newEvent == null || newEvent.getDate() == null
                || newEvent.getTime() == null || newEvent.getEndTime() == null) {
            return false;
        }

        try {
            LocalDate newDate = LocalDate.parse(newEvent.getDate());
            LocalTime newStart = LocalTime.parse(newEvent.getTime());
            LocalTime newEnd = LocalTime.parse(newEvent.getEndTime());

            for (Event existing : userEvents) {
                if (existing == null || existing.getDate() == null
                        || existing.getTime() == null || existing.getEndTime() == null) {
                    continue;
                }

                LocalDate existingDate = LocalDate.pa