
package com.scc.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {

        // Initialize existing user database
        new UserDao();

        // Create activity_logs table
        String sql = "CREATE TABLE IF NOT EXISTS activity_logs (" +
                "log_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "action_type TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);

            System.out.println("-> Database table verified");
            System.out.println("-> Activity logs table created/verified");

        } catch (SQLException e) {

            System.out.println("Database initialization failed!");
            e.printStackTrace();

        }
    }
}
