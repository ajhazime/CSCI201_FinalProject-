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
    penalties INT DEFAULT 0,
    firstName VARCHAR(50),
    lastName VARCHAR(50),
    avgRating DOUBLE DEFAULT 0,
    preferredLocations TEXT,                                                                                            
    penaltyTracked BOOLEAN DEFAULT false
  );                

 CREATE TABLE events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    activity_type VARCHAR(50) NOT NULL,
    location VARCHAR(100) NOT NULL,
    date DATE NOT NULL,                                                                                                 
    time TIME NOT NULL,
    end_time TIME NOT NULL,                                                                                             
    max_participants INT NOT NULL,
    current_participants INT DEFAULT 0,
    is_public BOOLEAN DEFAULT true,
    creator_id INT,
    INDEX idx_events_date_time (date, time, end_time),
    FOREIGN KEY (creator_id) REFERENCES users(id)                                                                       
  );

CREATE TABLE facilities (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    rating DOUBLE DEFAULT 0
);

CREATE TABLE event_participants (
    id INT AUTO_INCREMENT PRIMARY KEY,
    event_id INT NOT NULL,
    user_id INT NOT NULL,
    role VARCHAR(20) DEFAULT 'PARTICIPANT',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uniq_event_user (event_id, user_id),
    INDEX idx_event_participants_user_event (user_id, event_id),
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE event_invites (
    id INT AUTO_INCREMENT PRIMARY KEY,
    event_id INT NOT NULL,
    inviter_id INT NOT NULL,
    invitee_id INT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uniq_event_invitee (event_id, invitee_id),
    INDEX idx_event_invites_invitee (invitee_id, status),
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    FOREIGN KEY (inviter_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (invitee_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE user_availability (                                                                                        
    id INT AUTO_INCREMENT PRIMARY KEY,
    userID INT NOT NULL,
    dayOfWeek VARCHAR(10) NOT NULL,
    startTime TIME NOT NULL,
    endTime TIME NOT NULL,                                                                                              
    FOREIGN KEY (userID) REFERENCES users(id)
 );                                                                                                                      
                  
CREATE TABLE user_ratings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    rater_id INT NOT NULL,
    ratee_id INT NOT NULL,
    score INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uniq_rater_ratee (rater_id, ratee_id),
    FOREIGN KEY (rater_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (ratee_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE password_reset_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

 INSERT INTO facilities (name, description) VALUES                                                                       
 	('Lyon Center', 'USC gym facility'),
 	('USC Village Fitness Center', 'USC Village gym'),                                                                      
 	('Uytengsu Aquatics Center', 'Swimming facility'),
 	('HSC Fitness Center', 'Health Sciences gym'),                                                                          
 	('PED South Gym', 'Physical Education gym'); 