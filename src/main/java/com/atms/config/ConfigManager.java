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
 *
 * Priority order (highest wins):
 *   1. JVM system properties  (-Dkey=value passed via Maven)
 *   2. Environment variables  (GitHub Actions secrets / OS env)
 *   3. .env file              (local development)
 *   4. execution.properties / testdata.properties
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

    /** Resolves ${VAR_NAME} placeholders against env / .env file. */
    private static String resolve(String value) {
        if (value == null) return null;
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuffer resolved = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);

            // Check System.getenv() first (works in CI + locally if env is set)
            // Fall back to dotenv (.env file for local dev)
            String envValue = System.getenv(varName);
            if (envValue == null) {
                envValue = dotenv.get(varName, null);
            }

            if (envValue == null) {
                throw new RuntimeException(
                    "Environment variable '" + varName + "' is not set. " +
                    "Add it to your .env file or GitHub Actions secrets.");
            }
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(envValue));
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }
    /**
     * Returns the value for a key from execution.properties.
     * JVM system properties (-Dkey=value) take precedence over the file value.
     */
    public static String getExecution(String key) {
        String sysProp = System.getProperty(key);
        if (sysProp != null) return sysProp;
        return resolve(executionProps.getProperty(key));
    }

    /**
     * Returns the value for a key from testdata.properties.
     * JVM system properties (-Dkey=value) take precedence over the file value.
     */
    public static String getTestData(String key) {
        String sysProp = System.getProperty(key);
        if (sysProp != null) return sysProp;
        return resolve(testDataProps.getProperty(key));
    }

    /** Convenience alias — checks testdata first, then execution, then system props. */
    public static String get(String key) {
        String sysProp = System.getProperty(key);
        if (sysProp != null) return sysProp;
        String value = testDataProps.getProperty(key);
        if (value == null) value = executionProps.getProperty(key);
        return resolve(value);
    }
}
