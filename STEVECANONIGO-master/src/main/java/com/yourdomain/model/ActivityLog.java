package com.yourdomain.model;

/**
 * One row of the activity_logs audit trail.
 * {@code timestamp} is kept as the text SQLite stores ("yyyy-MM-dd HH:mm:ss", local time).
 */
public record ActivityLog(int logId, int userId, String actionType,
                          String description, String timestamp) {

    // ---- Action types used across the application ----
    public static final String LOGIN = "LOGIN";
    public static final String LOGOUT = "LOGOUT";
    public static final String USER_REGISTERED = "USER_REGISTERED";
    public static final String USER_CREATED = "USER_CREATED";
    public static final String USER_APPROVED = "USER_APPROVED";
    public static final String USER_REJECTED = "USER_REJECTED";
    public static final String USER_UPDATED = "USER_UPDATED";
    public static final String USER_DELETED = "USER_DELETED";
    public static final String LOGS_CLEARED = "LOGS_CLEARED";

    // ---- CafeClock business actions (call these from CustomerMenu / StaffMenu) ----
    public static final String SESSION_STARTED = "SESSION_STARTED";
    public static final String SESSION_ENDED = "SESSION_ENDED";
    public static final String BILL_PROCESSED = "BILL_PROCESSED";
    public static final String COMPUTER_UPDATED = "COMPUTER_UPDATED";
}
