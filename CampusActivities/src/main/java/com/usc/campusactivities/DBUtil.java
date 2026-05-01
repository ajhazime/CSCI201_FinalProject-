package com.usc.campusactivities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {
    private static final String URL = "jdbc:mysql://localhost:3306/campusactivities";
    private static final String USER = "root";
<<<<<<< Updated upstream
    private static final String PASSWORD = "password"; // Change as needed
=======
    private static final String PASSWORD = "Rfbc8089inj!2005"; // Change as needed
>>>>>>> Stashed changes

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}