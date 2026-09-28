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
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                System.err.println("MySQL JDBC Driver not found!");
            }
        }

        Properties props = new Properties();
        try (InputStream input = DbConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                props.load(input);
            } else {
                System.out.println("WARNING: application.properties not found on classpath. Using default configuration.");
            }
        } catch (Exception e) {
            System.err.println("Error reading application.properties: " + e.getMessage());
        }

        dbUrl = props.getProperty("db.url", "jdbc:mysql://localhost:3306/taskflow?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        dbUser = props.getProperty("db.username", "root");
        dbPassword = props.getProperty("db.password", "root"); // Replace 'root' if your MySQL password is different
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}