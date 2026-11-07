
package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.dao.ActivityLogDao;
import com.scc.model.user;
import com.scc.model.ActivityLog;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private static final List<String> VALID_ROLES =
            List.of("STUDENT", "TEACHER", "CUSTODIAN", "ADMIN");

    private static final List<String> VALID_STATUSES =
            List.of("APPROVED", "PENDING");

    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final Scanner scanner;

    public AdminMenu(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
        this.activityLogDao = new ActivityLogDao();
    }

    public void display(user admin) {

        boolean loggedIn = true;

        while (loggedIn) {

            System.out.println("\n===================================");
            System.out.println("          ADMIN DASHBOARD");
            System.out.println("===================================");
            System.out.println("1. View All Users");
            System.out.println("2. View Pending User Approvals");
            System.out.println("3. Approve User Registration");
            System.out.println("4. Add New User");
            System.out.println("5. Update User Role / Status");
            System.out.println("6. Delete User Account");
            System.out.println("7. View System Audit Logs");
            System.out.println("8. Logout");
            System.out.print("Choose an option (1-8): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> viewAllUser();
                case "2" -> viewPendingUsers();
                case "3" -> approveUser(admin);
                case "4", "6" ->
                        System.out.println("-> [INFO] This feature is not available yet.");
                case "5" -> updateUser(admin);
                case "7" -> auditLogMenu();
                case "8" -> {
                    System.out.println("-> Logged out.");
                    loggedIn = false;
                }
                default ->
                        System.out.println("-> [ERROR] Invalid Option.");
            }
        }
    }

    // ================= USER TABLE =================

    private void displayUserTable(List<user> users, String title) {

        System.out.println("\n==================== " + title + " ====================");

        if (users.isEmpty()) {
            System.out.println("-> No Records Found");
            return;
        }

        System.out.println("+-----+-----------------+------------+------------+");
        System.out.printf("| %-3s | %-15s | %-10s | %-10s |%n",
                "ID", "Username", "Role", "Status");
        System.out.println("+-----+-----------------+------------+------------+");

        for (user u : users) {
            System.out.printf("| %-3s | %-15s | %-10s | %-10s |%n",
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

    // ================= APPROVE USER =================

    private void approveUser(user admin) {

        List<user> pendingUsers = userDao.getPendingUser();

        displayUserTable(pendingUsers, "PENDING APPROVALS");

        if (pendingUsers.isEmpty()) {
            return;
        }

        int userId = readIntInput(
                "Enter User ID to Approve (or 0 to cancel): ");

        if (userId == 0) {
            System.out.println("-> Operation cancelled.");
            return;
        }

        boolean exists = pendingUsers.stream()
                .anyMatch(u -> u.id() == userId);

        if (!exists) {
            System.out.println("-> [ERROR] User ID not found.");
            return;
        }

        if (userDao.approveUser(admin.id(), userId)) {
            System.out.println("-> [SUCCESS] Account approved!");
        }
    }

    // ================= UPDATE USER =================

    private void updateUser(user currentAdmin) {

        List<user> users = userDao.getAllUser();

        displayUserTable(users, "ALL REGISTERED USERS");

        if (users.isEmpty()) {
            return;
        }

        int targetUserId = readIntInput(
                "Enter User ID to update (or 0 to cancel): ");

        if (targetUserId == 0) {
            System.out.println("-> Operation cancelled.");
            return;
        }

        if (targetUserId == currentAdmin.id()) {
            System.out.println("WARNING: Modifying your own admin account is restricted.");
            return;
        }

        boolean exists = users.stream()
                .anyMatch(u -> u.id() == targetUserId);

        if (!exists) {
            System.out.println("-> [ERROR] User ID not found.");
            return;
        }

        String newRole = readValidRole();

        if (newRole == null) {
            return;
        }

        String newStatus = readValidStatus();

        if (newStatus == null) {
            return;
        }

        if (userDao.updateUserRoleAndStatus(
                currentAdmin.id(),
                targetUserId,
                newRole,
                newStatus)) {

            System.out.println("-> [SUCCESS] User updated successfully!");

        } else {
            System.out.println("-> [ERROR] Update failed.");
        }
    }

    // ================= AUDIT LOG MENU =================

    private void auditLogMenu() {

        boolean running = true;

        while (running) {

            System.out.println("\n===================================");
            System.out.println("          SYSTEM AUDIT LOGS");
            System.out.println("===================================");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Clear Historical Logs");
            System.out.println("4. Back to Admin Menu");
            System.out.println("===================================");
            System.out.print("Enter choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {

                case "1" -> displayLogs(
                        activityLogDao.getAllLogs());

                case "2" -> {
                    int userId = readIntInput("Enter User ID: ");

                    if (userId > 0) {
                        displayLogs(
                                activityLogDao.getLogsByUserId(userId));
                    } else {
                        System.out.println("-> Invalid User ID.");
                    }
                }

                case "3" -> clearLogs();

                case "4" -> running = false;

                default ->
                        System.out.println("-> Invalid option.");
            }
        }
    }

    // ================= DISPLAY LOGS =================

    private void displayLogs(List<ActivityLog> logs) {

        System.out.println("\n==================== SYSTEM AUDIT RECORDS ====================");

        if (logs.isEmpty()) {
            System.out.println("-> No activity records found.");
            return;
        }

        System.out.printf(
                "%-5s | %-8s | %-18s | %-35s | %-20s%n",
                "ID", "USER ID", "ACTION TYPE",
                "DESCRIPTION", "TIMESTAMP"
        );

        System.out.println("-----------------------------------------------------------------------------------------------");

        for (ActivityLog log : logs) {

            System.out.printf(
                    "%-5d | %-8d | %-18s | %-35s | %-20s%n",
                    log.getLogId(),
                    log.getUserId(),
                    shorten(log.getActionType(), 18),
                    shorten(log.getDescription(), 35),
                    shorten(log.getTimestamp(), 20)
            );
        }

        System.out.println("-----------------------------------------------------------------------------------------------");
    }

    private String shorten(String text, int maxLength) {

        if (text == null) {
            return "";
        }

        if (text.length() > maxLength) {
            return text.substring(0, maxLength - 3) + "...";
        }

        return text;
    }

    // ================= CLEAR LOGS =================

    private void clearLogs() {

        System.out.print(
                "WARNING: Are you sure you want to delete ALL historical logs? (YES/NO): ");

        String confirmation = scanner.nextLine().trim();

        if (confirmation.equalsIgnoreCase("YES")) {

            if (activityLogDao.clearAllLogs()) {
                System.out.println("-> Historical logs cleared successfully.");
            } else {
                System.out.println("-> Failed to clear logs.");
            }

        } else {
            System.out.println("-> Clear operation cancelled.");
        }
    }

    // ================= INPUT VALIDATION =================

    private int readIntInput(String prompt) {

        while (true) {

            System.out.print(prompt);

            String input = scanner.nextLine().trim();

            try {
                return Integer.parseInt(input);

            } catch (NumberFormatException e) {
                System.out.println("-> [ERROR] Please enter numbers only.");
            }
        }
    }

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

            System.out.println("-> Invalid role.");
        }
    }

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

            System.out.println("-> Invalid status.");
        }
    }
}
