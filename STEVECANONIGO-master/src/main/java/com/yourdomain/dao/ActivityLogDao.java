package com.yourdomain.dao;

import com.yourdomain.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Reads and writes the activity_logs audit trail. Every query is parameterized. */
public class ActivityLogDao {

    private static final String SELECT_LOG =
            "SELECT log_id, user_id, action_type, description, timestamp FROM activity_logs";

    /**
     * Inserts one audit entry. The timestamp is filled in by the database.
     * Logging must never crash the feature that called it, so errors are
     * printed and reported as false instead of being thrown.
     */
    public boolean logAction(int userId, String actionType, String description) {
        String sql = "INSERT INTO activity_logs (user_id, action_type, description) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, description);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Could not write activity log: " + e.getMessage());
            return false;
        }
    }

    /** Every log entry, newest first. Empty list if none. */
    public List<ActivityLog> getAllLogs() {
        // log_id DESC breaks ties between entries written in the same second.
        String sql = SELECT_LOG + " ORDER BY timestamp DESC, log_id DESC";
        List<ActivityLog> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.out.println("Database error while reading logs: " + e.getMessage());
        }
        return list;
    }

    /** Log entries for one user, newest first. Empty list if none. */
    public List<ActivityLog> getLogsByUserId(int userId) {
        String sql = SELECT_LOG + " WHERE user_id = ? ORDER BY timestamp DESC, log_id DESC";
        List<ActivityLog> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error while reading logs for user: " + e.getMessage());
        }
        return list;
    }

    /** Deletes every log entry. Returns how many were removed, or -1 on error. */
    public int clearLogs() {
        String sql = "DELETE FROM activity_logs";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Database error while clearing logs: " + e.getMessage());
            return -1;
        }
    }

    private ActivityLog mapRow(ResultSet rs) throws SQLException {
        return new ActivityLog(
                rs.getInt("log_id"),
                rs.getInt("user_id"),
                rs.getString("action_type"),
                rs.getString("description"),
                rs.getString("timestamp"));
    }
}
