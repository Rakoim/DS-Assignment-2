import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String targetHash;
        int numThreads = 0;
        int passwordLength = 0;
        int numServers = 0;
        String[] serverAddresses = {"192.168.123.10", "192.168.123.11"}; // Replace with your servers' IPs

        // Get MD5 hash input and validate
        while (true) {
            System.out.print("Enter MD5 hash to crack: ");
            targetHash = scanner.nextLine().trim();

            // Validate MD5 hash
            if (targetHash.matches("^[a-fA-F0-9]{32}$")) {
                break;  // Exit the loop if valid
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
                    break;  // Exit the loop if valid
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
                    break;  // Exit the loop if valid
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
                    break;  // Exit the loop if the input is valid
                } else {
                    System.out.println("Invalid number of servers. Please enter 1 or 2.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid number of servers. Please enter 1 or 2.");
                scanner.next(); // Clear the invalid input
            }
        }

        // Calculate the total number of combinations (94^L)
        int numCombinations = (int) Math.pow(94, passwordLength);

        // Calculate the range each server will handle
        int rangePerServer = numCombinations / numServers;

        try {
            // Connect to the servers and distribute tasks
            boolean passwordFound = false;
            for (int i = 0; i < numServers; i++) {
                int startIndex = i * rangePerServer;
                int endIndex = (i == numServers - 1) ? numCombinations - 1 : (startIndex + rangePerServer - 1);

                // Connect to server
                Registry registry = LocateRegistry.getRegistry(serverAddresses[i], 1099);
                CrackPass server = (CrackPass) registry.lookup("CrackPass");

                // Start password cracking task
                server.crackPassword(targetHash, numThreads, passwordLength, startIndex, endIndex, i);
            }

            // Monitor the servers for password crack result
            while (!passwordFound) {
                boolean allServersFinished = true;
                for (int i = 0; i < numServers; i++) {
                    Registry registry = LocateRegistry.getRegistry(serverAddresses[i], 1099);
                    CrackPass server = (CrackPass) registry.lookup("CrackPass");
                    if (server.isPasswordFound()) {
                        passwordFound = true;
                        String result = server.getResult();
                        System.out.println(result);
                        break;
                    } else {
                        allServersFinished = allServersFinished && server.isPasswordFound();
                    }
                }

                // If no password is found and all servers are done, break the loop
                if (!passwordFound && allServersFinished) {
                    System.out.println("No password found after all servers completed the search.");
                    break;
                }

                Thread.sleep(1000); // Wait before checking again
            }

            // Notify other servers to stop if more than one server
            if (numServers > 1) {
                for (int i = 0; i < numServers; i++) {
                    Registry registry = LocateRegistry.getRegistry(serverAddresses[i], 1099);
                    CrackPass server = (CrackPass) registry.lookup("CrackPass");
                    server.stopCracking();
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
