package com.atms.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads and provides access to configuration and test data properties.
 */
public class ConfigManager {

    private static Properties executionProps = new Properties();
    private static Properties testDataProps = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try {
            InputStream execStream = new FileInputStream("Environment/execution.properties");
            InputStream dataStream = new FileInputStream("Environment/testdata.properties");

            executionProps.load(execStream);
            testDataProps.load(dataStream);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load property files", e);
        }
    }

    public static String getExecution(String key) {
        return executionProps.getProperty(key);
    }

    public static String getTestData(String key) {
        return testDataProps.getProperty(key);
    }
}