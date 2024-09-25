package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnection {

    private static final String URL = "jdbc:postgresql://localhost:5432/mydatabase";
    private static final String USER = "postgres";
    private static final String PASSWORD = "1234567890";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
