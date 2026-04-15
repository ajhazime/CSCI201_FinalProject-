package com.usc.campusactivities;

public class User {
    private int id;
    private String username;
    private String password;
    private String email;
    private String interests; // comma separated
    private String skillLevel;
    private int penalties;

    public User() {}

    public User(int id, String username, String password, String email, String interests, String skillLevel, int penalties) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.interests = interests;
        this.skillLevel = skillLevel;
        this.penalties = penalties;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }

    public String getSkillLevel() { return skillLevel; }
    public void setSkillLevel(String skillLevel) { this.skillLevel = skillLevel; }

    public int getPenalties() { return penalties; }
    public void setPenalties(int penalties) { this.penalties = penalties; }
}