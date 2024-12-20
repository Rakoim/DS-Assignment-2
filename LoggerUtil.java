/* LoggerUtil.java
A utility class for logging events with timestamps. This class provides 
a simple method for outputting logs to the console with a consistent format.

Methods:
- logEvent: Accepts a message string and prints it to the console with a timestamp indicating when the event occurred.

Usage:
- Call `LoggerUtil.logEvent("Your message here");` to log events throughout the system. */
import java.text.SimpleDateFormat;
import java.util.Date;

// Utility class for logging events with timestamps
public class LoggerUtil {

    // Method to log messages with a timestamp
    public static void logEvent(String message) {
        // Format the current date and time in "yyyy-MM-dd HH:mm:ss" format
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        
        // Print the log message with timestamp to the console
        System.out.println("[" + timestamp + "] " + message);
    }
}