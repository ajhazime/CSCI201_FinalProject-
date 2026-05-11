# CSCI201 Final Project - Campus Activities Web App

This is a web application for USC students to find and join campus activities.

## Features

- User registration and login with USC email validation
- Guest browsing of activities
- Event creation and scheduling
- User profiles with interests and skill levels
- Facility information and ratings
- Penalties for cancellations

## Technologies

- Java Servlets and JSP
- MySQL Database
- Maven for build
- Tomcat for deployment

## Setup

1. Install Java 11, Maven, MySQL, Tomcat.

2. Create database: Run schema.sql in MySQL.

3. Update DBUtil.java with your database credentials.

4. Build: `mvn clean package`

5. Deploy the generated WAR file to Tomcat.

6. Access at http://localhost:8080/CampusActivities/

## Project Structure

- src/main/java: Java source files
- src/main/webapp: JSP and web resources
- schema.sql: Database schema
