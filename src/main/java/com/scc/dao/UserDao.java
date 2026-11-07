
package com.scc.dao;

import com.scc.model.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public UserDao() {
        createUsersTable();
        defaultAdminGenerator();
    }

    private void createUsersTable() {

        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    roles TEXT NOT NULL,
                    status TEXT NOT NULL
                )
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error creating users table: " + e.getMessage());
        }
    }

    public void defaultAdminGenerator() {

        String checkSql = "SELECT COUNT(*) FROM users WHERE roles = ?";
        String insertSql = "INSERT INTO users(username, password, roles, status) VALUES(?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement check = conn.prepareStatement(checkSql)) {

            check.setString(1, "ADMIN");

            try (ResultSet rs = check.executeQuery()) {

                if (rs.next() && rs.getInt(1) == 0) {

                    try (PreparedStatement insert = conn.prepareStatement(insertSql)) {

                        insert.setString(1, "admin");
                        insert.setString(2, "admin123");
                        insert.setString(3, "ADMIN");
                        insert.setString(4, "APPROVED");

                        insert.executeUpdate();

                        System.out.println(
                                "-> Default Admin created (Username: admin | Password: admin123)"
                        );
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error seeding default admin: " + e.getMessage());
        }
    }

    // REGISTER USER
    public boolean registerUser(String username, String password, String role) {

        String sql = "INSERT INTO users(username, password, roles, status) VALUES(?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role.toUpperCase());
            pstmt.setString(4, "PENDING");

            pstmt.executeUpdate();

            // Get newly registered user ID
            try (PreparedStatement idStmt =
                         conn.prepareStatement("SELECT last_insert_rowid()");
                 ResultSet rs = idStmt.executeQuery()) {

                if (rs.next()) {

                    int newUserId = rs.getInt(1);

                    activityLogDao.logAction(
                            newUserId,
                            "USER_REGISTERED",
                            "New user registered: " + username +
                                    " with role " + role.toUpperCase()
                    );
                }
            }

            System.out.println(
                    "-> Successful Registration, Waiting for Approval!"
            );

            return true;

        } catch (SQLException e) {
            System.err.println("Failed: " + e.getMessage());
            return false;
        }
    }

    // LOGIN USER
    public user login(String username, String password) {

        String sql = "SELECT id, username, password, roles, status FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {

                    String status = rs.getString("status");

                    if ("PENDING".equalsIgnoreCase(status)) {
                        System.out.println(
                                "Account is pending, waiting for Approval"
                        );
                        return null;
                    }

                    return new user(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("roles"),
                            status
                    );

                } else {

                    System.out.println("--> Invalid username or password");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error during login: " + e.getMessage());
        }

        return null;
    }

    // GET ALL USERS
    public List<user> getAllUser() {

        List<user> usersList = new ArrayList<>();

        String sql = "SELECT id, username, password, roles, status FROM users ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {

                usersList.add(new user(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("roles"),
                        rs.getString("status")
                ));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching all users: " + e.getMessage());
        }

        return usersList;
    }

    // GET PENDING USERS
    public List<user> getPendingUser() {

        List<user> pendingList = new ArrayList<>();

        String sql = "SELECT id, username, password, roles, status FROM users WHERE status = ? ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "PENDING");

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {

                    pendingList.add(new user(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("roles"),
                            rs.getString("status")
                    ));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching users: " + e.getMessage());
        }

        return pendingList;
    }

    // APPROVE USER
    public boolean approveUser(int adminId, int userId) {

        String sql = "UPDATE users SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "APPROVED");
            pstmt.setInt(2, userId);

            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {

                activityLogDao.logAction(
                        adminId,
                        "USER_APPROVED",
                        "Admin ID " + adminId +
                                " approved account for User ID " + userId
                );

                System.out.println(
                        "-> User ID: " + userId + " approved successfully!"
                );

                return true;

            } else {

                System.out.println("-> ID not found");
            }

        } catch (SQLException e) {
            System.err.println("Error approving users: " + e.getMessage());
        }

        return false;
    }

    // COMPATIBILITY METHOD
    public boolean approveUser(int userId) {
        return approveUser(0, userId);
    }

    // UPDATE USER ROLE AND STATUS
    public boolean updateUserRoleAndStatus(
            int adminId,
            int userId,
            String newRole,
            String newStatus) {

        String sql = "UPDATE users SET roles = ?, status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newRole.toUpperCase());
            pstmt.setString(2, newStatus.toUpperCase());
            pstmt.setInt(3, userId);

            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {

                activityLogDao.logAction(
                        adminId,
                        "USER_UPDATED",
                        "Admin ID " + adminId +
                                " updated User ID " + userId +
                                " to Role: " + newRole.toUpperCase() +
                                ", Status: " + newStatus.toUpperCase()
                );

                System.out.println("-> User updated successfully!");

                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
        }

        return false;
    }

    // COMPATIBILITY METHOD
    public boolean updateUserRoleAndStatus(
            int userId, String newRole, String newStatus) {

        return updateUserRoleAndStatus(
                0, userId, newRole, newStatus
        );
    }
}
