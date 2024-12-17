import java.math.BigInteger;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImpCrack extends UnicastRemoteObject implements CrackPass {
    public static AtomicBoolean passwordFound = new AtomicBoolean(false);

    protected ImpCrack() throws RemoteException {
        super();
    }

    @Override
    public void crackPassword(String targetHash, int numThreads, int passwordLength, int startIndex, int endIndex, int serverIndex) throws RemoteException {
        System.out.println("Server " + (serverIndex + 1) + " initiated password search of length " + passwordLength + " for range " + startIndex + " to " + endIndex);
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        String[] result = {null};
        long serverStartTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            final int start = startIndex + (i * (endIndex - startIndex) / numThreads);
            final int end = (i == numThreads - 1) ? endIndex : start + (endIndex - startIndex) / numThreads;
            executor.submit(() -> {
                for (int index = start; index <= end && !passwordFound.get(); index++) {
                    String password = indexToPassword(index, passwordLength);
                    if (getMd5(password).equals(targetHash) && !passwordFound.get()) {
                        passwordFound.set(true);
                        result[0] = password;

                        long threadId = Thread.currentThread().threadId();
                        long serverEndTime = System.currentTimeMillis();
                        double timeTaken = (serverEndTime - serverStartTime) / 1000.0;

                        // Send result to client
                        System.out.println("\nPassword Cracked by Server " + (serverIndex + 1) + ":");
                        System.out.println("--------------------------------");
                        System.out.println("Thread ID: " + threadId);
                        System.out.println("Password: " + password);
                        System.out.println("Time taken: " + timeTaken + " seconds");
                        System.out.println("--------------------------------");
                        break;
                    }
                }
            });
        }
        executor.shutdown();
    }

    @Override
    public void stopCracking() throws RemoteException {
        passwordFound.set(true);
        System.out.println("Password has been found by another server. Stopping password search operation.");
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
