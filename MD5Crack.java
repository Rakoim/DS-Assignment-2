// MD5Crack class implementing Runnable to define the password-cracking task
public class MD5Crack implements Runnable {
    private final int start;      // Start ASCII range for the thread
    private final int end;        // End ASCII range for the thread
    private final int length;     // Password length to attempt
    private final int threadId;   // Thread ID for logging

    MD5Crack(int start, int end, int length, int threadId) {
        this.start = start;
        this.end = end;
        this.length = length;
        this.threadId = threadId;
    }

    @Override
    public void run() {
        // Start the recursive cracking process with an empty character array of the given length
        crackPassword(new char[length], 0);
    }

    // Recursive method to attempt all character combinations within the specified range
    private synchronized void crackPassword(char[] chars, int pos) {
        // Base case: if the end of the character array is reached, form the password and check its hash
        if (pos == chars.length) {
            String attempt = new String(chars);
            // Compare MD5 hash of attempt with target hash
            if (Main.getMd5(attempt).equals(Main.getTargetHash()) && !Main.isFound()) {
                // If a match is found, mark as found and output details
                long endTime = System.currentTimeMillis();
                Main.setFound(true); // Set the password found flag
                System.out.println("Thread ID: " + threadId);
                System.out.println("Password: " + attempt);
                System.out.println("Time taken: " + (endTime - Main.getStartTime()) / 1000.0 + " seconds");
            }
            return;
        }

        // Try each character within the thread's assigned range at the current position
        for (int i = (pos == 0 ? start : 33); i <= (pos == 0 ? end : 126) && !Main.isFound(); i++) {
            chars[pos] = (char) i;
            crackPassword(chars, pos + 1); // Recurse to the next position
        }
    }
}