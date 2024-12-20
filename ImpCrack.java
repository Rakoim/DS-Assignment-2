/* ImpCrack.java
Implements the `CrackPass` interface for distributed password cracking using multi-threading. 
Converts numeric indices to passwords (`indexToPassword`), hashes them with MD5 (`getMd5`), 
and compares them to the target hash.

Key Features:
- Multi-threaded brute-force logic in `crackPassword`.
- Logs progress, results, and supports remote stop.
- Notifies other threads upon finding the password.

Actual password-cracking happens in threads created within `crackPassword` method. */

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImpCrack extends UnicastRemoteObject implements CrackPass {
    // Atomic flag to track if password is found
    private static final AtomicBoolean passwordFound = new AtomicBoolean(false);
    private boolean thisServerFoundPassword = false;
    private boolean thisServerFinished = false; // Marks if the server has finished the task
    private String result = "";
    private ExecutorService executor; // Executor for handling threads
    private final Object syncMontior = new Object(); // Synchronization object for waiting threads

    // Constructor
    protected ImpCrack() throws RemoteException {
        super();
    }

    @Override
    public void crackPassword(String targetHash, int numThreads, int passwordLength, int startIndex, int endIndex, int serverIndex) throws RemoteException {
        // Reset state for each task
        passwordFound.set(false);
        thisServerFoundPassword = false;
        thisServerFinished = false;
        result = "";
        
        // Log the task initiation
        LoggerUtil.logEvent("Server " + (serverIndex + 1) + " initiated password search of length " + passwordLength + " for range " + startIndex + " to " + endIndex);

        executor = Executors.newFixedThreadPool(numThreads); // Create thread pool for parallel execution
        long serverStartTime = System.currentTimeMillis(); // Capture start time for performance measurement

        // Assign work to each thread
        for (int i = 0; i < numThreads; i++) {
            final int start = startIndex + (i * (endIndex - startIndex) / numThreads);
            final int end = (i == numThreads - 1) ? endIndex : start + (endIndex - startIndex) / numThreads;

            executor.submit(() -> {
                for (int index = start; index <= end && !passwordFound.get(); index++) {
                    String password = indexToPassword(index, passwordLength);
                    if (getMd5(password).equals(targetHash)) {
                        synchronized (syncMontior) {
                            if (!passwordFound.get()) {
                                passwordFound.set(true); // Mark password found
                                thisServerFoundPassword = true; // Mark this server found the password
								
                                long threadId = Thread.currentThread().threadId();
                                long serverEndTime = System.currentTimeMillis();
                                double timeTaken = (serverEndTime - serverStartTime) / 1000.0; // Calculate time taken
                                
                                // Prepare result to send to client and log to the server console
                                result = "Server " + (serverIndex + 1) + " has successfully cracked the password. Details are as follows:\n" +
                                        "--------------------------------\n" +
                                        "Thread ID: " + threadId + "\n" +
                                        "Password: " + password + "\n" +
                                        "Time taken: " + timeTaken + " seconds\n" +
                                        "--------------------------------";
                                LoggerUtil.logEvent(result); // Log the result
                                syncMontior.notifyAll(); // Notify other threads
                            }
                        }
                        break;
                    }
                }
                synchronized (syncMontior) {
                    thisServerFinished = true; // Mark the task as finished
                    syncMontior.notifyAll(); // Notify all waiting threads
                }
            });
        }

        executor.shutdown();
        synchronized (syncMontior) {
            while (!executor.isTerminated() && !thisServerFinished) {
                try {
                    syncMontior.wait(); // Wait until either the task finishes or password is found
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Handle thread interruption
                }
            }
        }

        // If this server completes and password was not found, log a message
        if (!thisServerFoundPassword && !passwordFound.get()) {
            LoggerUtil.logEvent("Server " + (serverIndex + 1) + " has finished checking all assigned combinations but failed to find the password.");
        }
    }

    @Override
    public void stopCracking() throws RemoteException {
        passwordFound.set(true); // Mark as found
        // Check if the current server needs to stop because another server found the password
        if (!thisServerFoundPassword) {
            LoggerUtil.logEvent("Password found by another server. Stopping the search.");
        }
    
        // Mark the current server to stop
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow(); // Shut down the executor immediately
        }
    }
    

    @Override
    public boolean isPasswordFound() throws RemoteException {
        return passwordFound.get(); // Return if the password is found
    }

    @Override
    public String getResult() throws RemoteException {
        return result; // Return the result of the cracked password
    }

    @Override
    public boolean isFinished() throws RemoteException {
        return thisServerFinished; // Return if the server has finished the task
    }

    private String indexToPassword(int index, int length) {
        StringBuilder password = new StringBuilder(length);
        int base = 94;
        for (int i = length - 1; i >= 0; i--) {
            password.insert(0, (char) (33 + (index % base)));
            index /= base;
        }
        return password.toString();
    }

    private String getMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashText = no.toString(16);
            while (hashText.length() < 32) {
                hashText = "0" + hashText;
            }
            return hashText;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}