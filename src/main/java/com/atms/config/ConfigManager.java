package com.atms.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads execution and test data configuration from the Environment/ directory.
 * Resolves paths relative to the project root (working directory).
 */
public class ConfigManager {

    private static final Properties executionProps = new Properties();
    private static final Properties testDataProps  = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream execStream = new FileInputStream("Environment/execution.properties");
             InputStream dataStream = new FileInputStream("Environment/testdata.properties")) {

            executionProps.load(execStream);
            testDataProps.load(dataStream);

        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to load property files from Environment/. " +
                "Ensure execution.properties and testdata.properties exist.", e);
        }
    }

    public static String getExecution(String key) {
        return executionProps.getProperty(key);
    }

    public static String getTestData(String key) {
        return testDataProps.getProperty(key);
    }
    
    
    
}
