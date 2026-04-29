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

                LocalDate existingDate = LocalDate.parse(existing.getDate());
                if (!existingDate.equals(newDate)) {
                    continue;
                }

                LocalTime existingStart = LocalTime.parse(existing.getTime());
                LocalTime existingEnd = LocalTime.parse(existing.getEndTime());

                // Overlap rule: (startA < endB) AND (endA > startB)
                if (newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart)) {
                    return true;
                }
            }
        } catch (DateTimeParseException e) {
            return false;
        }

        return false;
    }
}
