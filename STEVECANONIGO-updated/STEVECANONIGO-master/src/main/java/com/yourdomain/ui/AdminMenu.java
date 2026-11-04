package com.yourdomain.ui;

import com.yourdomain.dao.UserDao;
import com.yourdomain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class AdminMenu {
    private static final String DOUBLE_LINE = "=".repeat(40);
    private static final String ROW_FORMAT = "%-5s %-15s %-20s %-12s %-10s%n";
    private static final String WIDE_DOUBLE_LINE = "=".repeat(66);
    private static final String TABLE_LINE = "-".repeat(66);

    private final Scanner scanner;
    private final UserDao userDao;
    private final User admin;

    public AdminMenu(Scanner scanner, UserDao userDao, User admin) {
        this.scanner = scanner;
        this.userDao = userDao;
        this.admin = admin;
    }

    public void show() {
        while (true) {
            System.out.println("\n" + DOUBLE_LINE);
            System.out.println("             ADMIN MENU");
            System.out.println(DOUBLE_LINE);
            System.out.println("1. View All Accounts");
            System.out.println("2. View Pending Accounts");
            System.out.println("3. Review Pending Accounts (Approve / Reject)");
            System.out.println("4. Create Account");
            System.out.println("5. Delete Account");
            System.out.println("6. Update User Role / Status");
            System.out.println("0. Logout");
            System.out.println(DOUBLE_LINE);
            System.out.print("Enter choice: ");
            if (!scanner.hasNextLine()) return;

            switch (scanner.nextLine().trim()) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingUsers();
                case "3" -> reviewPendingAccounts();
                case "4" -> createAccount();
                case "5" -> deleteAccount();
                case "6" -> updateUser();
                case "0" -> { return; }
                default -> System.out.println("\nERROR: Invalid choice. Enter 1-6 or 0.");
            }
        }
    }

    private void viewAllUsers() {
        printUserTable("ALL ACCOUNTS", userDao.getAllUsers(), "No registered users found.");
    }

    private void viewPendingUsers() {
        printUserTable("PENDING ACCOUNTS", userDao.getPendingUsers(),
                "No pending accounts.");
    }

    private void reviewPendingAccounts() {
        List<User> pending = userDao.getPendingUsers();
        printUserTable("PENDING ACCOUNTS", pending, "No pending accounts.");
        if (pending.isEmpty()) {
            return;
        }

        String input = prompt("Enter the User ID to review (0 to go back): ");
        int userId;
        try {
            userId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
            return;
        }
        if (userId == 0) {
            return;
        }

        User target = null;
        for (User u : pending) {
            if (u.userId() == userId) {
                target = u;
            }
        }
        if (target == null) {
            System.out.println("User ID " + userId + " is not in the pending list.");
            return;
        }

        System.out.println("Account: " + target.name() + " (" + target.username() + ", " + target.role() + ")");
        System.out.println("1. Approve (status becomes ACTIVE)");
        System.out.println("2. Reject (status becomes INACTIVE)");
        System.out.println("3. Back");
        String choice = prompt("Enter your choice: ");

        switch (choice) {
            case "1" -> {
                if (userDao.approveUser(userId)) {
                    userDao.logActivity(admin.userId(), "Approved user ID " + userId);
                    System.out.println("Account approved. The user can now log in.");
                } else {
                    System.out.println("Could not approve the account. It may no longer be pending.");
                }
            }
            case "2" -> {
                if (userDao.rejectUser(userId)) {
                    userDao.logActivity(admin.userId(), "Rejected user ID " + userId);
                    System.out.println("Account rejected. The user cannot log in.");
                } else {
                    System.out.println("Could not reject the account. It may no longer be pending.");
                }
            }
            case "3" -> { }
            default -> System.out.println("Invalid choice. No changes were made.");
        }
    }

    // Accounts made by an Admin are ACTIVE right away (no pending step).
    private void createAccount() {
        System.out.println("\n========== CREATE ACCOUNT ==========");
        System.out.println("(Leave the name empty to cancel.)");
        String name = prompt("Full name: ");
        if (name.isEmpty()) {
            System.out.println("Account creation cancelled.");
            return;
        }
        String username = prompt("Username (3+ characters, no spaces): ");
        if (username.length() < 3 || username.contains(" ")) {
            System.out.println("\nERROR: Invalid username.");
            return;
        }
        String password = prompt("Password (6+ characters): ");
        if (password.length() < 6) {
            System.out.println("\nERROR: Password must be at least 6 characters.");
            return;
        }
        System.out.println("Role: 1. CUSTOMER  2. CAFE_STAFF  3. ADMIN");
        String role = switch (prompt("Choose role: ")) {
            case "1" -> User.ROLE_CUSTOMER;
            case "2" -> User.ROLE_STAFF;
            case "3" -> User.ROLE_ADMIN;
            default -> null;
        };
        if (role == null) {
            System.out.println("\nERROR: Invalid role. Choose 1, 2, or 3.");
            return;
        }

        switch (userDao.addUser(name, username, password, role, User.STATUS_ACTIVE)) {
            case SUCCESS -> {
                userDao.logActivity(admin.userId(), "Created " + role + " account '" + username + "'");
                System.out.println("\nSUCCESS: Account created.");
                System.out.println("Username: " + username);
                System.out.println("Role: " + role);
                System.out.println("Status: " + User.STATUS_ACTIVE);
            }
            case DUPLICATE_USERNAME -> System.out.println("\nERROR: That username is already taken.");
            case ERROR -> System.out.println("\nERROR: Could not create the account (database error).");
        }
    }

    private void deleteAccount() {
        List<User> all = userDao.getAllUsers();
        printUserTable("DELETE ACCOUNT", all, "No registered users found.");
        if (all.isEmpty()) {
            return;
        }

        System.out.print("Enter User ID to delete (0 to cancel): ");
        String input = scanner.hasNextLine() ? scanner.nextLine().trim() : "0";

        int userId;
        try {
            userId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("\nERROR: Invalid input. Please enter a number.");
            return;
        }
        if (userId == 0) {
            System.out.println("\nDeletion cancelled. No changes were made.");
            return;
        }

        Optional<User> target = userDao.findById(userId);
        if (target.isEmpty()) {
            System.out.println("\nERROR: User ID " + userId + " does not exist.");
            return;
        }
        User user = target.get();

        if (user.userId() == admin.userId()) {
            System.out.println("\nERROR: You cannot delete your own account while logged in.");
            return;
        }
        if (user.role().equals(User.ROLE_ADMIN) && userDao.countByRole(User.ROLE_ADMIN) <= 1) {
            System.out.println("\nERROR: You cannot delete the last Admin account.");
            return;
        }

        String confirm = prompt("Delete '" + user.username() + "' (" + user.role()
                + ")? This cannot be undone. Type YES to confirm: ");
        if (!confirm.equals("YES")) {
            System.out.println("\nDeletion cancelled. No changes were made.");
            return;
        }

        switch (userDao.deleteUser(userId)) {
            case DELETED -> {
                userDao.logActivity(admin.userId(), "Deleted user '" + user.username() + "'");
                System.out.println("\nSUCCESS: Account '" + user.username() + "' deleted.");
            }
            case NOT_FOUND -> System.out.println("\nERROR: The account no longer exists.");
            case ERROR -> System.out.println("\nERROR: Could not delete the account (database error).");
            case HAS_RECORDS -> offerDeactivate(user);
        }
    }

    // The account has session history or activity logs, which must be kept.
    private void offerDeactivate(User user) {
        System.out.println("\nThis account has session or activity records, so it cannot be deleted.");
        String answer = prompt("Set it to INACTIVE instead (blocks login, keeps records)? (y/n): ");
        if (answer.equalsIgnoreCase("y")) {
            if (userDao.deactivateUser(user.userId())) {
                userDao.logActivity(admin.userId(), "Set user '" + user.username() + "' to INACTIVE");
                System.out.println("\nSUCCESS: Account is now INACTIVE and can no longer log in.");
            } else {
                System.out.println("\nERROR: Could not update the account.");
            }
        } else {
            System.out.println("\nNo changes were made.");
        }
    }

    // ------------------------------------------------------------------
    // UPDATE USER ROLE / STATUS  (enter 0 at any step to cancel)
    // ------------------------------------------------------------------
    private void updateUser() {
        List<User> all = userDao.getAllUsers();
        printUserTable("UPDATE USER ROLE / STATUS", all, "No registered users found.");
        if (all.isEmpty()) {
            return;
        }

        int targetUserId = readIntInput("Enter User ID to update (0 to cancel): ");
        if (targetUserId == 0) {
            System.out.println("\nUpdate cancelled. No changes were made.");
            return;
        }

        // Self-edit restriction: the logged-in admin cannot change their own account.
        if (targetUserId == admin.userId()) {
            System.out.println("\nWARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        Optional<User> target = userDao.findById(targetUserId);
        if (target.isEmpty()) {
            System.out.println("\nERROR: User ID " + targetUserId + " does not exist.");
            return;
        }
        User user = target.get();
        System.out.println("\nSelected: " + user.username() + " (" + user.name() + ")"
                + " | Role: " + user.role() + " | Status: " + user.status());

        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("\nUpdate cancelled. No changes were made.");
            return;
        }
        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("\nUpdate cancelled. No changes were made.");
            return;
        }

        if (userDao.updateUserRoleAndStatus(targetUserId, newRole, newStatus)) {
            userDao.logActivity(admin.userId(), "Updated user ID " + targetUserId
                    + " to role " + newRole + ", status " + newStatus);
            System.out.println("\nSUCCESS: User '" + user.username() + "' updated.");
            System.out.println("Role:   " + user.role() + " -> " + newRole);
            System.out.println("Status: " + user.status() + " -> " + newStatus);
        } else {
            System.out.println("\nERROR: Could not update the user. No changes were made.");
        }
    }

    // ------------------------------------------------------------------
    // INPUT VALIDATION WRAPPERS
    // ------------------------------------------------------------------

    /** Keeps asking until the person types a whole number. Never crashes on letters/symbols. */
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                return 0; // input closed: treat as cancel
            }
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("ERROR: Invalid input. Please enter a number (digits only).");
            }
        }
    }

    /**
     * Keeps asking until a valid role is typed. Returns null if the admin enters 0 (cancel).
     * Valid roles: CUSTOMER, CAFE_STAFF, ADMIN.
     */
    private String readValidRole() {
        while (true) {
            System.out.print("Enter new role (CUSTOMER / CAFE_STAFF / ADMIN, 0 to cancel): ");
            if (!scanner.hasNextLine()) {
                return null;
            }
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) {
                return null;
            }
            if (input.equals(User.ROLE_CUSTOMER) || input.equals(User.ROLE_STAFF)
                    || input.equals(User.ROLE_ADMIN)) {
                return input;
            }
            System.out.println("ERROR: Invalid role '" + input + "'. Choose CUSTOMER, CAFE_STAFF, or ADMIN.");
        }
    }

    /**
     * Keeps asking until a valid status is typed. Returns null if the admin enters 0 (cancel).
     * APPROVED is stored as ACTIVE, because ACTIVE is the status this system uses to allow login.
     */
    private String readValidStatus() {
        while (true) {
            System.out.print("Enter new status (APPROVED / PENDING, 0 to cancel): ");
            if (!scanner.hasNextLine()) {
                return null;
            }
            String input = scanner.nextLine().trim().toUpperCase();
            switch (input) {
                case "0" -> { return null; }
                case "APPROVED" -> { return User.STATUS_ACTIVE; }
                case "PENDING" -> { return User.STATUS_PENDING; }
                default -> System.out.println("ERROR: Invalid status '" + input + "'. Choose APPROVED or PENDING.");
            }
        }
    }

    /** Reusable table printer used by every user list. */
    private void printUserTable(String title, List<User> users, String emptyMessage) {
        System.out.println("\n" + WIDE_DOUBLE_LINE);
        System.out.println(title);
        System.out.println(WIDE_DOUBLE_LINE + "\n");
        if (users.isEmpty()) {
            System.out.println(emptyMessage);
            return;
        }
        System.out.printf(ROW_FORMAT, "ID", "Username", "Full Name", "Role", "Status");
        System.out.println(TABLE_LINE);
        for (User u : users) {
            System.out.printf(ROW_FORMAT,
                    String.valueOf(u.userId()),
                    fit(u.username(), 15),
                    fit(u.name(), 20),
                    fit(u.role(), 12),
                    fit(u.status(), 10));
        }
        System.out.println(TABLE_LINE);
        System.out.println("Accounts shown: " + users.size());
    }

    private String prompt(String text) {
        System.out.print(text);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "";
    }

    // Shortens long text with "..." so columns stay aligned.
    private String fit(String text, int width) {
        if (text == null) return "";
        return text.length() <= width ? text : text.substring(0, width - 3) + "...";
    }
}
