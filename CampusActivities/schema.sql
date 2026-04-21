DROP DATABASE IF EXISTS campusactivities;
CREATE DATABASE campusactivities;
USE campusactivities;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL,
    interests TEXT,
    skill_level VARCHAR(20),
    penalties INT DEFAULT 0
);

CREATE TABLE events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    activity_type VARCHAR(50) NOT NULL,
    location VARCHAR(100) NOT NULL,
    date DATE NOT NULL,
    time TIME NOT NULL,
    max_participants INT NOT NULL,
    current_participants INT DEFAULT 0,
    creator_id INT,
    FOREIGN KEY (creator_id) REFERENCES users(id)
);

CREATE TABLE facilities (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    rating DOUBLE DEFAULT 0
);

-- Add more tables as needed for reviews, participants, etc.