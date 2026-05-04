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
    penaltyTracked BOOLEAN DEFAULT false,
    event_restriction_until DATETIME NULL DEFAULT NULL
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
    attendance_finalized BOOLEAN NOT NULL DEFAULT FALSE,
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
    present TINYINT NULL DEFAULT NULL,
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

INSERT INTO users (username, password, email, interests, skill_level, penalties)
VALUES ('guest', 'guest12345678', 'guest@usc.edu', 'fitness,wellness', 'beginner', 0);

                  
 INSERT INTO facilities (name, description) VALUES                                                                       
 	('Lyon Center', 'USC gym facility'),
 	('USC Village Fitness Center', 'USC Village gym'),                                                                      
 	('Uytengsu Aquatics Center', 'Swimming facility'),
 	('HSC Fitness Center', 'Health Sciences gym'),                                                                          
 	('PED South Gym', 'Physical Education gym'); 