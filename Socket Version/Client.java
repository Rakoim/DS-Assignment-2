import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Get MD5 hash input
        System.out.print("Enter MD5 hash to crack: ");
        String targetHash = scanner.nextLine().trim();

        // Validate MD5 hash
        if (!targetHash.matches("^[a-fA-F0-9]{32}$")) {
            System.out.println("Invalid MD5 hash. Exiting.");
            return;
        }

        // Get number of threads per server
        System.out.print("Enter number of threads per server: ");
        int numThreads = scanner.nextInt();

        // Get password length
        System.out.print("Enter password length: ");
        int passwordLength = scanner.nextInt();

        // Get number of servers
        System.out.print("Enter number of servers (1 or 2): ");
        int numServers = scanner.nextInt();

        // Server addresses
        String[] serverAddresses = {"192.168.123.10", "192.168.123.11"}; // Replace with your servers' IPs
        int serverPort = 5000;

        // Calculate the total number of combinations (94^L)
        int numCombinations = (int) Math.pow(94, passwordLength);

        // Calculate the range each server will handle
        int rangePerServer = numCombinations / numServers;

        ExecutorService executorService = Executors.newFixedThreadPool(numServers);

        AtomicBoolean passwordFound = new AtomicBoolean(false);

        try {
            // Submit tasks to the executor service for simultaneous connection
            for (int i = 0; i < numServers; i++) {
                int startIndex = i * rangePerServer;
                int endIndex = (i == numServers - 1) ? numCombinations - 1 : (startIndex + rangePerServer - 1);

                final int serverIndex = i;
                executorService.submit(() -> {
                    try {
                        // Connect to server
                        Socket socket = new Socket(serverAddresses[serverIndex], serverPort);
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        // Send task details to the server
                        out.println(targetHash);
                        out.println(numThreads);
                        out.println(passwordLength);
                        out.println(startIndex);
                        out.println(endIndex);
                        out.println(serverIndex);

                        // Monitor server output
                        String line;
                        while ((line = in.readLine()) != null) {
                            System.out.println(line);
                            if (line.startsWith("Password:")) {
                                passwordFound.set(true);
                                if (numServers > 1) {
                                    notifyOtherServers(serverAddresses, serverIndex, serverPort);
                                }
                            }
                        }

                        socket.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

            // Wait for all tasks to complete
            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void notifyOtherServers(String[] serverAddresses, int foundServerIndex, int serverPort) {
        for (int i = 0; i < serverAddresses.length; i++) {
            if (i != foundServerIndex) {
                try (Socket socket = new Socket(serverAddresses[i], serverPort);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                    out.println("STOP"); // Notify other servers to stop
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}