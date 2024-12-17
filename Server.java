import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {
    public static AtomicBoolean passwordFound = new AtomicBoolean(false); // Atomic flag to indicate password found

    public static void main(String[] args) {
        int port = 5000;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                // Receive task details
                String firstInput = in.readLine();

                if ("STOP".equals(firstInput)) {
                    passwordFound.set(true); // Stop processing if notified by client
                    System.out.println("Password has been found by another server. Stopping password search operation.");
                    return;
                }

                String targetHash = firstInput;
                int numThreads = Integer.parseInt(in.readLine());
                int passwordLength = Integer.parseInt(in.readLine());
                int startIndex = Integer.parseInt(in.readLine());
                int endIndex = Integer.parseInt(in.readLine());
                int serverIndex = Integer.parseInt(in.readLine());

                System.out.println("Server " + (serverIndex + 1) + " initiated password search of length " 
                + passwordLength + " for range " + startIndex + " to " + endIndex);

                // Start brute force cracking
                ExecutorService executor = Executors.newFixedThreadPool(numThreads);
                String[] result = {null};

                long serverStartTime = System.currentTimeMillis();

                for (int i = 0; i < numThreads; i++) {
                    executor.submit(() -> {
                        crackPassword(targetHash, startIndex, endIndex, passwordLength, result, out, serverIndex, serverStartTime);
                    });
                }

                executor.shutdown();
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

                passwordFound.set(false); // Reset flag for next task

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void crackPassword(String targetHash, int startIndex, int endIndex, int length, String[] result, PrintWriter out, int serverIndex, long serverStartTime) {
            for (int i = startIndex; i <= endIndex && !passwordFound.get(); i++) {
                String password = indexToPassword(i, length);
                if (getMd5(password).equals(targetHash) && !passwordFound.get()) {
                    passwordFound.set(true);
                    result[0] = password;

                    long threadId = Thread.currentThread().threadId();
                    long serverEndTime = System.currentTimeMillis();
                    double timeTaken = (serverEndTime - serverStartTime) / 1000.0;

                    // Send result to client
                    out.println("\nPassword Cracked by Server " + (serverIndex + 1) + ":");
                    out.println("--------------------------------");
                    out.println("Thread ID: " + threadId);
                    out.println("Password: " + password);
                    out.println("Time taken: " + timeTaken + " seconds");
                    out.println("--------------------------------");
                    out.flush(); // Ensure the message is sent immediately

                    // Log to the server console
                    System.out.println("Server " + (serverIndex + 1) + " has successfully found the password: " + password);

                    break;
                }
            }
        }

        private String indexToPassword(int index, int length) {
            StringBuilder password = new StringBuilder(length);
            int base = 94; // Number of possible characters
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
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }
}   