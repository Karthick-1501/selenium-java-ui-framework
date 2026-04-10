package com.atms.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads execution and test data configuration from the Environment/ directory.
 * Secrets are kept in a .env file and never committed to source control.
 * Property values referencing ${VAR_NAME} are resolved against the loaded env.
 */
public class ConfigManager {

    private static final Properties executionProps = new Properties();
    private static final Properties testDataProps  = new Properties();
    private static final Dotenv dotenv             = loadDotenv();

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    static {
        loadProperties();
    }

    private static Dotenv loadDotenv() {
        try {
            return Dotenv.configure().ignoreIfMissing().load();
        } catch (DotenvException e) {
            throw new RuntimeException("Failed to load .env file.", e);
        }
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

    /** Resolves ${VAR_NAME} placeholders against .env / system environment. */
    private static String resolve(String value) {
        if (value == null) return null;
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuffer resolved = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String envValue = dotenv.get(varName, System.getenv(varName));
            if (envValue == null) {
                throw new RuntimeException(
                    "Environment variable '" + varName + "' is not set. " +
                    "Add it to your .env file.");
            }
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(envValue));
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }

    public static String getExecution(String key) {
        return resolve(executionProps.getProperty(key));
    }

    public static String getTestData(String key) {
        return resolve(testDataProps.getProperty(key));
    }

    /** Convenience alias — checks testdata first, then execution. */
    public static String get(String key) {
        String value = testDataProps.getProperty(key);
        if (value == null) value = executionProps.getProperty(key);
        return resolve(value);
    }
}
