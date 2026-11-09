package com.yourdomain.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseConnection {
    // Relative to the project folder when run from IntelliJ.
    private static final String URL = "jdbc:sqlite:students.db";

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
            try (ResultSet rs = st.executeQuery("PRAGMA foreign_keys;")) {
                if (!rs.next() || rs.getInt(1) != 1) {
                    throw new SQLException("Could not enable foreign keys.");
                }
            }
        } catch (SQLException e) {
            conn.close();
            throw e;
        }
        return conn;
    }
}
