package com.taskflow.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DbConfig {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    static {
        // 1. Load MySQL Driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                System.err.println("MySQL JDBC Driver not found!");
            }
        }

        // 2. Load fallback properties from application.properties
        Properties props = new Properties();
        try (InputStream input = DbConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                props.load(input);
            } else {
                System.out.println("WARNING: application.properties not found on classpath.");
            }
        } catch (Exception e) {
            System.err.println("Error reading application.properties: " + e.getMessage());
        }

        // 3. Prioritize System Environment Variables (Render) > properties file > hardcoded fallback
        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPassword = System.getenv("DB_PASSWORD");

        dbUrl = (envUrl != null && !envUrl.trim().isEmpty()) 
                ? envUrl 
                : props.getProperty("db.url", "jdbc:mysql://localhost:3306/taskflow?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");

        dbUser = (envUser != null && !envUser.trim().isEmpty()) 
                 ? envUser 
                 : props.getProperty("db.username", "root");

        dbPassword = (envPassword != null && !envPassword.trim().isEmpty()) 
                     ? envPassword 
                     : props.getProperty("db.password", "root");
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}