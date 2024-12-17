import java.math.BigInteger;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImpCrack extends UnicastRemoteObject implements CrackPass {
    private static final AtomicBoolean passwordFound = new AtomicBoolean(false);
    private boolean thisServerFoundPassword = false;
    private String result = "";
    private ExecutorService executor;

    protected ImpCrack() throws RemoteException {
        super();
    }

    @Override
    public void crackPassword(String targetHash, int numThreads, int passwordLength, int startIndex, int endIndex, int serverIndex) throws RemoteException {
        // Reset state
        passwordFound.set(false);
        thisServerFoundPassword = false;
        result = "";

        System.out.println("Server " + (serverIndex + 1) + " initiated password search of length " + passwordLength + " for range " + startIndex + " to " + endIndex);
        
        executor = Executors.newFixedThreadPool(numThreads);
        long serverStartTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            final int start = startIndex + (i * (endIndex - startIndex) / numThreads);
            final int end = (i == numThreads - 1) ? endIndex : start + (endIndex - startIndex) / numThreads;
            executor.submit(() -> {
                for (int index = start; index <= end && !passwordFound.get(); index++) {
                    String password = indexToPassword(index, passwordLength);
                    if (getMd5(password).equals(targetHash) && !passwordFound.get()) {
                        passwordFound.set(true);
                        thisServerFoundPassword = true;

                        long threadId = Thread.currentThread().threadId();
                        long serverEndTime = System.currentTimeMillis();
                        double timeTaken = (serverEndTime - serverStartTime) / 1000.0;

                        // Log to the server console
                        System.out.println("Server " + (serverIndex + 1) + " has successfully cracked the password. Details are as follows:");

                        // Prepare result to send to client
                        result = "\nPassword Cracked by Server " + (serverIndex + 1) + ":\n" +
                                "--------------------------------\n" +
                                "Thread ID: " + threadId + "\n" +
                                "Password: " + password + "\n" +
                                "Time taken: " + timeTaken + " seconds\n" +
                                "--------------------------------";

                        // Display result on the server if only one server
                        System.out.println(result);
                        break;
                    } else if (index == end && !passwordFound.get()) {
                        result = "No password found.";
                    }
                }
            });
        }
        executor.shutdown();
    }

    @Override
    public void stopCracking() throws RemoteException {
        if (!thisServerFoundPassword) {
            System.out.println("Password has been found by another server. Stopping password search operation.");
        }
        passwordFound.set(true);
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    @Override
    public boolean isPasswordFound() throws RemoteException {
        return passwordFound.get();
    }

    @Override
    public String getResult() throws RemoteException {
        return result;
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