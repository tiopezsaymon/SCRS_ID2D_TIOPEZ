package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.user;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private static final List<String> VALID_ROLES = List.of("STUDENT", "TEACHER", "CUSTODIAN", "ADMIN");
    private static final List<String> VALID_STATUSES = List.of("APPROVED", "PENDING");

    private final UserDao userDao;
    private final Scanner scanner;

    public AdminMenu(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void display(user admin) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n===================================");
            System.out.println("          ADMIN DASHBOARD          ");
            System.out.println("===================================");
            System.out.println("1. View All Users");
            System.out.println("2. View Pending User Approvals");
            System.out.println("3. Approve User Registration");
            System.out.println("4. Add New User");
            System.out.println("5. Update User Role / Status");
            System.out.println("6. Delete User Account");
            System.out.println("7. Logout");
            System.out.print("Choose an option (1-7): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> viewAllUser();
                case "2" -> viewPendingUsers();
                case "3" -> approveUser();
                case "4", "6" -> System.out.println("-> [INFO] This feature is not available yet.");
                case "5" -> updateUser(admin);
                case "7" -> {
                    System.out.println("-> Logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("-> [ERROR] Invalid Option.");
            }
        }
    }

    // ===================== TABLE / VIEW HELPERS =====================

    private void displayUserTable(List<user> users, String title) {
        System.out.println("\n==================== " + title + " ====================");
        if (users.isEmpty()) {
            System.out.println("-> No Records Found");
            return;
        }

        System.out.println("+-----+-----------------+------------+------------+");
        System.out.printf("| %-3s | %-15s | %-10s | %-10s |\n", "ID", "Username", "Role", "Status");
        System.out.println("+-----+-----------------+------------+------------+");
        for (user u : users) {
            System.out.printf("| %-3s | %-15s | %-10s | %-10s |\n",
                    u.id(), u.username(), u.roles(), u.status());
        }
        System.out.println("+-----+-----------------+------------+------------+");
    }

    private void viewAllUser() {
        displayUserTable(userDao.getAllUser(), "ALL REGISTERED USERS");
    }

    private void viewPendingUsers() {
        displayUserTable(userDao.getPendingUser(), "PENDING APPROVALS");
    }

    private void approveUser() {
        List<user> pendingUsers = userDao.getPendingUser();
        displayUserTable(pendingUsers, "PENDING APPROVALS");

        if (pendingUsers.isEmpty()) {
            return;
        }

        int userId = readIntInput("Enter User ID to Approve (or 0 to cancel): ");

        if (userId == 0) {
            System.out.println("-> Operation cancelled by the user.");
            return;
        }

        boolean exists = pendingUsers.stream().anyMatch(u -> u.id() == userId);
        if (!exists) {
            System.out.println("-> [ERROR] User ID not found in the pending list.");
            return;
        }

        userDao.approveUser(userId);
    }

    // ===================== UPDATE USER ROLE / STATUS =====================

    private void updateUser(user currentAdmin) {
        List<user> users = userDao.getAllUser();
        displayUserTable(users, "ALL REGISTERED USERS");

        if (users.isEmpty()) {
            return;
        }

        int targetUserId = readIntInput("Enter the User ID to update (or 0 to cancel): ");
        if (targetUserId == 0) {
            System.out.println("-> Operation cancelled by the user.");
            return;
        }

        // Self-edit restriction
        if (targetUserId == currentAdmin.id()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        boolean exists = users.stream().anyMatch(u -> u.id() == targetUserId);
        if (!exists) {
            System.out.println("-> [ERROR] User ID not found.");
            return;
        }

        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("-> Operation cancelled by the user.");
            return;
        }

        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("-> Operation cancelled by the user.");
            return;
        }

        if (userDao.updateUserRoleAndStatus(targetUserId, newRole, newStatus)) {
            System.out.println("-> [SUCCESS] User ID " + targetUserId
                    + " updated successfully: Role = " + newRole + ", Status = " + newStatus);
        } else {
            System.out.println("-> [ERROR] Update failed. No changes were made.");
        }
    }

    // ===================== INPUT VALIDATION WRAPPERS =====================

    /** Keeps asking until the user types a whole number. Never throws. */
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("-> [ERROR] Invalid input. Please enter a number (digits only).");
            }
        }
    }

    /** Returns a valid role in upper case, or null if the user entered 0 to cancel. */
    private String readValidRole() {
        while (true) {
            System.out.print("Enter new role " + VALID_ROLES + " (or 0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) {
                return null;
            }
            if (VALID_ROLES.contains(input)) {
                return input;
            }
            System.out.println("-> [ERROR] Invalid role. Valid roles are: " + VALID_ROLES);
        }
    }

    /** Returns a valid status in upper case, or null if the user entered 0 to cancel. */
    private String readValidStatus() {
        while (true) {
            System.out.print("Enter new status " + VALID_STATUSES + " (or 0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) {
                return null;
            }
            if (VALID_STATUSES.contains(input)) {
                return input;
            }
            System.out.println("-> [ERROR] Invalid status. Valid statuses are: " + VALID_STATUSES);
        }
    }
}
