package com.yourdomain.dao;

import com.yourdomain.model.User;
import com.yourdomain.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    public enum RegisterResult { SUCCESS, DUPLICATE_USERNAME, ERROR }

    public enum DeleteResult { DELETED, NOT_FOUND, HAS_RECORDS, ERROR }

    private static final String SELECT_USER =
            "SELECT user_id, name, username, password_hash, role, status FROM users";

    /** Public registration: always starts as PENDING. */
    public RegisterResult register(String name, String username, String password, String role) {
        return addUser(name, username, password, role, User.STATUS_PENDING);
    }

    public RegisterResult addUser(String name, String username, String password,
                                  String role, String status) {
        if (findByUsername(username).isPresent()) {
            return RegisterResult.DUPLICATE_USERNAME;
        }
        String sql = "INSERT INTO users (name, username, password_hash, role, status) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, username);
            ps.setString(3, PasswordUtil.hash(password));
            ps.setString(4, role);
            ps.setString(5, status);
            ps.executeUpdate();
            return RegisterResult.SUCCESS;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                return RegisterResult.DUPLICATE_USERNAME;
            }
            System.out.println("Database error while registering: " + e.getMessage());
            return RegisterResult.ERROR;
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = SELECT_USER + " WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error while finding user: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<User> findById(int userId) {
        String sql = SELECT_USER + " WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error while finding user: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Deletes one account. Foreign keys protect session history and activity logs:
     * if the user has any, the delete is refused (HAS_RECORDS) and nothing is lost.
     */
    public DeleteResult deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0 ? DeleteResult.DELETED : DeleteResult.NOT_FOUND;
        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toUpperCase();
            if (msg.contains("FOREIGN KEY")) {
                return DeleteResult.HAS_RECORDS;
            }
            System.out.println("Database error while deleting user: " + e.getMessage());
            return DeleteResult.ERROR;
        }
    }

    /** Disables an account without deleting its records. */
    public boolean deactivateUser(int userId) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, User.STATUS_INACTIVE);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Database error while deactivating user: " + e.getMessage());
            return false;
        }
    }

    /** Checks username + password only. The caller must check status and role. */
    public Optional<User> authenticate(String username, String password) {
        Optional<User> user = findByUsername(username);
        if (user.isPresent() && PasswordUtil.verify(password, user.get().passwordHash())) {
            return user;
        }
        return Optional.empty();
    }

    /** Every registered user, any role, any status. Empty list if none. */
    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = SELECT_USER + " ORDER BY user_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.out.println("Database error while listing all users: " + e.getMessage());
        }
        return list;
    }

    /** Every user with PENDING status, any role. Empty list if none. */
    public List<User> getPendingUsers() {
        List<User> list = new ArrayList<>();
        String sql = SELECT_USER + " WHERE status = ? ORDER BY user_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, User.STATUS_PENDING);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error while listing pending users: " + e.getMessage());
        }
        return list;
    }

    /** Changes PENDING to ACTIVE. True only if a row was updated. */
    public boolean approveUser(int userId) {
        return changePendingStatus(userId, User.STATUS_ACTIVE);
    }

    /** Changes PENDING to INACTIVE (rejected). True only if a row was updated. */
    public boolean rejectUser(int userId) {
        return changePendingStatus(userId, User.STATUS_INACTIVE);
    }

    // Only touches accounts that are still PENDING.
    private boolean changePendingStatus(int userId, String newStatus) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ? AND status = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, userId);
            ps.setString(3, User.STATUS_PENDING);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Database error while updating account status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates BOTH the role and the status of one user in a single statement.
     * Inputs are converted to upper case before they reach the database.
     * Returns true only if a row was updated.
     */
    public boolean updateUserRoleAndStatus(int userId, String newRole, String newStatus) {
        // Table is "users", columns are "role" / "status" / "user_id" in this project.
        String sql = "UPDATE users SET role = ?, status = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newRole.toUpperCase());
            ps.setString(2, newStatus.toUpperCase());
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Database error while updating user: " + e.getMessage());
            return false;
        }
    }

    /** Returns the number of users with this role, or -1 on error. */
    public int countByRole(String role) {
        String sql = "SELECT COUNT(*) FROM users WHERE role = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.out.println("Database error while counting users: " + e.getMessage());
            return -1;
        }
    }

    /** Writes a row to activity_logs (login, logout, approvals, etc.). */
    public void logActivity(int userId, String action) {
        String sql = "INSERT INTO activity_logs (user_id, action) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Could not write activity log: " + e.getMessage());
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("name"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("role"),
                rs.getString("status"));
    }
}
