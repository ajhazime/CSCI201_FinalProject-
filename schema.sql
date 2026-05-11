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
    security_question VARCHAR(255),
    security_answer_hash VARCHAR(255),
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

CREATE TABLE facility_reviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    facility_id INT NOT NULL,
    user_id INT NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_facility (facility_id, user_id),
    FOREIGN KEY (facility_id) REFERENCES facilities(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
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

CREATE TABLE attendance_appeals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    event_id INT NOT NULL,
    appellant_id INT NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP NULL DEFAULT NULL,
    UNIQUE KEY uniq_event_appellant (event_id, appellant_id),
    INDEX idx_appeals_host_lookup (event_id, status),
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    FOREIGN KEY (appellant_id) REFERENCES users(id) ON DELETE CASCADE
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

-- ---------------------------------------------------------------------------
-- Test users for invites + matching (password for all: testpass123)
-- Interests are comma-separated; overlap drives MatchingEngine.comparePreferences.
-- Log in as any of these or register with overlapping interests (e.g. basketball, swimming, yoga).
-- ---------------------------------------------------------------------------
INSERT INTO users (username, password, email, interests, skill_level, penalties, firstName, lastName, avgRating, preferredLocations, penaltyTracked) VALUES
('sam_swim', 'testpass123', 'samswim@usc.edu', 'swimming,yoga,pilates,running', 'beginner', 0, 'Sam', 'Nguyen', 4.6, 'Uytengsu Aquatics Center,Lyon Center', false),
('riley_run', 'testpass123', 'rileyrun@usc.edu', 'running,soccer,basketball,swimming', 'intermediate', 0, 'Riley', 'Park', 4.4, 'Lyon Center,PED South Gym', false),
('morgan_lift', 'testpass123', 'morganlift@usc.edu', 'weightlifting,basketball,running,yoga', 'competitive', 0, 'Morgan', 'Lee', 4.7, 'Lyon Center,USC Village Fitness Center', false),
('casey_yoga', 'testpass123', 'caseyyoga@usc.edu', 'yoga,pilates,swimming,mindfulness', 'beginner', 0, 'Casey', 'Diaz', 4.3, 'USC Village Fitness Center,HSC Fitness Center', false),
('jordan_ball', 'testpass123', 'jordanball@usc.edu', 'basketball,volleyball,soccer,running', 'intermediate', 0, 'Jordan', 'Kim', 4.5, 'PED South Gym,Lyon Center', false),
('taylor_lane', 'testpass123', 'taylorlane@usc.edu', 'swimming,running,weightlifting', 'intermediate', 0, 'Taylor', 'Brown', 4.2, 'Uytengsu Aquatics Center,Lyon Center', false),
('drew_volley', 'testpass123', 'drewvolley@usc.edu', 'volleyball,basketball,running,yoga', 'intermediate', 0, 'Drew', 'Martinez', 4.5, 'PED South Gym,USC Village Fitness Center', false),
('jamie_mix', 'testpass123', 'jamiemix@usc.edu', 'swimming,yoga,running,pilates', 'beginner', 0, 'Jamie', 'Chen', 4.4, 'Uytengsu Aquatics Center,USC Village Fitness Center', false),
('quinn_core', 'testpass123', 'quinncore@usc.edu', 'pilates,yoga,weightlifting', 'competitive', 0, 'Quinn', 'Singh', 4.8, 'Lyon Center,HSC Fitness Center', false),
('sky_soccer', 'testpass123', 'skysoccer@usc.edu', 'soccer,running,basketball', 'intermediate', 0, 'Sky', 'Patel', 4.1, 'PED South Gym,Lyon Center', false),
('dev_gym', 'testpass123', 'devgym@usc.edu', 'weightlifting,running,basketball,yoga', 'competitive', 0, 'Dev', 'Okonkwo', 4.6, 'Lyon Center,USC Village Fitness Center', false),
('alex_well', 'testpass123', 'alexwell@usc.edu', 'yoga,pilates,swimming,running', 'beginner', 0, 'Alex', 'Wells', 4.0, 'USC Village Fitness Center,Uytengsu Aquatics Center', false);

-- Overlapping weekly availability (Monday/Wednesday evening) for stronger compareAvailability scores.
INSERT INTO user_availability (userID, dayOfWeek, startTime, endTime)
SELECT id, 'Monday', '17:00:00', '19:30:00' FROM users WHERE username IN ('sam_swim','riley_run','morgan_lift','jordan_ball','taylor_lane','drew_volley');

INSERT INTO user_availability (userID, dayOfWeek, startTime, endTime)
SELECT id, 'Wednesday', '12:00:00', '14:00:00' FROM users WHERE username IN ('casey_yoga','jamie_mix','quinn_core','alex_well');

INSERT INTO user_availability (userID, dayOfWeek, startTime, endTime)
SELECT id, 'Saturday', '09:00:00', '11:00:00' FROM users WHERE username IN ('sky_soccer','dev_gym','riley_run','morgan_lift');