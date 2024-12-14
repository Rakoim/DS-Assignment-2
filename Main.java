import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.concurrent.*;

// Main class for the MD5 password-cracking program
public class Main {
    private static boolean isFound = false; // Tracks if the password is found
    private static String targetHash;       // The target MD5 hash to crack
    private static long startTime;          // Records the start time for performance tracking

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Prompt the user to enter an MD5 hash
        while (true) {
            System.out.print("Enter MD5 hash to crack: ");
            targetHash = scanner.nextLine().trim();
            // Validate that the input is a valid MD5 hash
            if (isValidMD5(targetHash)) {
                break;
            } else {
                System.out.println("Invalid MD5 hash. Please enter a valid 32-character hexadecimal string.");
            }
        }

        int numThreads;
        // Prompt the user for the number of threads to use (from 1 to 10)
        while (true) {
            System.out.print("Enter number of threads (1 to 10): ");
            numThreads = scanner.nextInt();
            scanner.nextLine(); // Consume newline character
            if (numThreads >= 1 && numThreads <= 10) {
                break;
            } else {
                System.out.println("Invalid input. Please enter a thread count between 1 and 10.");
            }
        }

        // Record the start time of the cracking process
        startTime = System.currentTimeMillis();
        System.out.println("Starting search with " + numThreads + " threads...");

        // Initialize a thread pool with the specified number of threads
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        // Attempt password cracking with lengths from 3 to 6 characters
        for (int length = 3; length <= 6 && !isFound; length++) {
            // Divide the character range (33 to 126 ASCII) across threads
            int range = 94 / numThreads;
            for (int i = 0; i < numThreads; i++) {
                int start = 33 + i * range;  // Start ASCII for each thread
                int end = (i == numThreads - 1) ? 126 : start + range - 1; // End ASCII for each thread
                // Submit a new MD5Crack task to the executor
                executor.submit(new MD5Crack(start, end, length, i + 1));
            }
        }

        executor.shutdown();
        // Wait indefinitely for all threads to complete
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (!isFound) {
            System.out.println("Password not found.");
        }
    }

    // Validates if a string is a 32-character hexadecimal (valid MD5 hash)
    private static boolean isValidMD5(String hash) {
        return hash.matches("^[a-fA-F0-9]{32}$");
    }

    // Generates the MD5 hash of a given string
    public static String getMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashText = no.toString(16);
            // Pad with leading zeros to make the hash 32 characters
            while (hashText.length() < 32) {
                hashText = "0" + hashText;
            }
            return hashText;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // Getter for isFound, targetHash, and startTime
    public static boolean isFound() {
        return isFound;
    }

    public static String getTargetHash() {
        return targetHash;
    }

    public static long getStartTime() {
        return startTime;
    }

    // Setter to mark the password as found
    public static void setFound(boolean found) {
        isFound = found;
    }
}