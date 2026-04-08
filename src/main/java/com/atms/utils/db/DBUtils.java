package com.atms.utils.db;

import java.sql.Connection;
import java.sql.DriverManager;

import com.atms.config.ConfigManager;

public class DBUtils {

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(
                    ConfigManager.getExecution("db.url"),
                    ConfigManager.getExecution("db.username"),
                    ConfigManager.getExecution("db.password")
            );
        } catch (Exception e) {
            throw new RuntimeException("DB connection failed", e);
        }
    }
}