package cascii.config;

import java.io.FileInputStream; // File reading
import java.io.IOException; // Input/Output error handler
import java.util.Properties; // Key-value pair storage (key/value = string)

public class Config {
    private static final String FILE_NAME = "config.properties";
    private static final Properties properties = new Properties();

    private Config() {
    }

    public static void load() {
        // Read & load constants from "config.properties"
        try (FileInputStream in = new FileInputStream(FILE_NAME)) {
            properties.load(in);
            System.out.println("Settings loaded from " + FILE_NAME);
        }
        // "config.properties" not found
        catch (IOException e) {
            System.out.println(FILE_NAME + " not found, using default values");
        }
    }

    public static double getDouble(String key, double defaultValue) {
        // Read constant's value if present
        String text = properties.getProperty(key);
        if (text == null) {
            return defaultValue;
        }
        // Parse constant's value
        try {
            return Double.parseDouble(text.trim());
        }
        // NaN handler
        catch (NumberFormatException e) {
            System.out.println("Warning: '" + key + "' is not a number in " + FILE_NAME
                    + ", using " + defaultValue);
            return defaultValue;
        }
    }
}