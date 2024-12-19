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