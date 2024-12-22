import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    public static void main(String[] args) {
        // Create a scanner object for reading input
        Scanner scanner = new Scanner(System.in);

        // Variables for inputs from user
        String targetHash;
        int numThreads = 0;
        int passwordLength = 0;
        int numServers = 0;
        // Array of server IP addresses
        String[] serverAddresses = {"192.168.123.10", "192.168.123.11"}; // Replace with your servers' IPs
        List<Integer> connectedServerIndices = new ArrayList<>(); // List of connected server indices

        try {
            // Get MD5 hash input and validate
            while (true) {
                System.out.print("Enter MD5 hash to crack: ");
                targetHash = scanner.nextLine().trim();
                // Validate MD5 hash (32 characters, hex)
                if (targetHash.matches("^[a-fA-F0-9]{32}$")) {
                    break; // Exit the loop if valid
                } else {
                    System.out.println("Invalid MD5 hash. Please enter a valid 32-character MD5 hash.");
                }
            }

            // Get number of threads per server and validate
            while (true) {
                System.out.print("Enter number of threads per server: ");
                try {
                    numThreads = scanner.nextInt();
                    if (numThreads > 0) {
                        break; // Exit the loop if valid
                    } else {
                        System.out.println("Invalid number of threads. Please enter a positive integer.");
                    }
                } catch (InputMismatchException e) {
                    System.out.println("Invalid number of threads. Please enter a positive integer.");
                    scanner.next(); // Clear the invalid input
                }
            }

            // Get password length and validate
            while (true) {
                System.out.print("Enter password length: ");
                try {
                    passwordLength = scanner.nextInt();
                    if (passwordLength > 0) {
                        break; // Exit the loop if valid
                    } else {
                        System.out.println("Invalid password length. Please enter a positive integer.");
                    }
                } catch (InputMismatchException e) {
                    System.out.println("Invalid password length. Please enter a positive integer.");
                    scanner.next(); // Clear the invalid input
                }
            }

            // Get number of servers and validate
            while (true) {
                System.out.print("Enter number of servers (1 or 2): ");
                try {
                    numServers = scanner.nextInt();
                    if (numServers == 1 || numServers == 2) {
                        break; // Exit the loop if the input is valid
                    } else {
                        System.out.println("Invalid number of servers. Please enter 1 or 2.");
                    }
                } catch (InputMismatchException e) {
                    System.out.println("Invalid number of servers. Please enter 1 or 2.");
                    scanner.next(); // Clear the invalid input
                }
            }

            // Calculate the total number of combinations (94^L)
            long numCombinations = (long) Math.pow(94, passwordLength);

            // Calculate the range each server will handle
            long rangePerServer = numCombinations / numServers;

            // Make variables effectively final for use in lambda
            final String finalTargetHash = targetHash;
            final int finalNumThreads = numThreads;
            final int finalPasswordLength = passwordLength;

            try {
                // Executor to handle parallel server connections
                ExecutorService executorService = Executors.newFixedThreadPool(numServers);
                CountDownLatch latch = new CountDownLatch(numServers); // Used to wait for all servers to be connected

                // Connect to the servers in parallel
                for (int i = 0; i < numServers; i++) {
                    final int serverIndex = i;  // Make serverIndex final
                    final long startIndex = serverIndex * rangePerServer; // Calculate startIndex outside the lambda
                    final long endIndex = (serverIndex == numServers - 1) ? numCombinations - 1 : (startIndex + rangePerServer - 1); // Calculate endIndex outside the lambda

                    executorService.submit(() -> {
                        boolean connected = false;
                        int currentServerIndex = serverIndex; // Start with the assigned server index
                        while (!connected) {
                            try {
                                // Attempt to connect to the current server using RMI
                                Registry registry = LocateRegistry.getRegistry(serverAddresses[currentServerIndex], 1099);
                                CrackPass server = (CrackPass) registry.lookup("CrackPass");

                                LoggerUtil.logEvent("Server " + (currentServerIndex + 1) + " connected.");
                                connectedServerIndices.add(currentServerIndex); // Add to the list of connected server indices
                                latch.countDown(); // Signal that this server is connected

                                // Wait until all servers are connected before starting the cracking task
                                latch.await();

                                // Start password cracking task
                                LoggerUtil.logEvent("Initiated password cracking task on Server " + (currentServerIndex + 1) + ".");
                                server.crackPassword(finalTargetHash, finalNumThreads, finalPasswordLength, startIndex, endIndex, currentServerIndex);

                                connected = true;
                            } catch (Exception e) {
                                System.err.println("Failed to connect to Server " + (currentServerIndex + 1) + ". Error: " + e.getMessage());
                                System.out.println("Retrying with the next available server...");
                                currentServerIndex = (currentServerIndex + 1) % serverAddresses.length; // Switch to the next server in the list

                                // If we've tried all servers and failed, break the loop
                                if (currentServerIndex == serverIndex) {
                                    LoggerUtil.logEvent("All servers failed to connect for task starting at index " + startIndex + ".");
                                    System.out.println("Tips: Ensure the server is running, check the network connection, and verify the server's IP address and port.");
                                    System.out.print("Retry connecting to Server " + (currentServerIndex + 1) + "? (y/n): ");

                                    String retry = scanner.next().trim().toLowerCase();
                                    if (!retry.equals("y")) {
                                        LoggerUtil.logEvent("User chose not to retry connecting to Server " + (currentServerIndex + 1) + ".");
                                        latch.countDown(); // Prevent deadlock in case of user giving up
                                        break;
                                    }
                                }
                            }
                        }
                    });
                }

                // Wait for all servers to be connected before starting the cracking process
                latch.await();
                LoggerUtil.logEvent("All servers connected, starting the cracking process.");
                executorService.shutdown();

                // Monitor the connected servers for password crack result
                boolean passwordFound = false;
                while (!passwordFound) {
                    boolean allServersFinished = true;
                    for (int serverIndex : connectedServerIndices) {
                        // Check if password is found on the server
                        Registry registry = LocateRegistry.getRegistry(serverAddresses[serverIndex], 1099);
                        CrackPass server = (CrackPass) registry.lookup("CrackPass");

                        if (server.isPasswordFound()) {
                            passwordFound = true;
                            String result = server.getResult();
                            LoggerUtil.logEvent(result);

                            // Stop cracking on all connected servers
                            for (int stopServerIndex : connectedServerIndices) {
                                Registry stopRegistry = LocateRegistry.getRegistry(serverAddresses[stopServerIndex], 1099);
                                CrackPass stopServer = (CrackPass) stopRegistry.lookup("CrackPass");
                                stopServer.stopCracking();
                            }
                            break;
                        } else if (!server.isFinished()) {
                            allServersFinished = false;
                        }
                    }

                    // If all servers are finished and no password found, log message
                    if (allServersFinished && !passwordFound) {
                        LoggerUtil.logEvent("Password not found after checking all combinations. Please ensure the password length is correct.");
                        break;
                    }
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            // Ensure the scanner is closed after all input has been processed
            scanner.close();
        }
    }
}