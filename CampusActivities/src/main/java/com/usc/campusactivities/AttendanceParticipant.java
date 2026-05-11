package com.usc.campusactivities;

/**
 * Participant row for host attendance UI (non-host participants only).
 */
public class AttendanceParticipant {
    private int userId;
    private String username;
    /** null = not marked yet, true = present, false = absent */
    private Boolean present;

    public AttendanceParticipant() {}

    public AttendanceParticipant(int userId, String username, Boolean present) {
        this.userId = userId;
        this.username = username;
        this.present = present;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Boolean getPresent() {
        return present;
    }

    public void setPresent(Boolean present) {
        this.present = present;
    }
}
