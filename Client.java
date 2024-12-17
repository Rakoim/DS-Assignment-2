import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
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

        // Calculate the total number of combinations (94^L)
        int numCombinations = (int) Math.pow(94, passwordLength);

        // Calculate the range each server will handle
        int rangePerServer = numCombinations / numServers;

        AtomicBoolean passwordFound = new AtomicBoolean(false);

        try {
            // Connect to the servers and distribute tasks
            for (int i = 0; i < numServers; i++) {
                int startIndex = i * rangePerServer;
                int endIndex = (i == numServers - 1) ? numCombinations - 1 : (startIndex + rangePerServer - 1);

                // Connect to server
                Registry registry = LocateRegistry.getRegistry(serverAddresses[i], 1099);
                CrackPass server = (CrackPass) registry.lookup("CrackPass");

                // Start password cracking task
                server.crackPassword(targetHash, numThreads, passwordLength, startIndex, endIndex, i);
            }

            // Wait for the result
            while (!passwordFound.get()) {
                // Continuously check for the result
                Thread.sleep(1000);
            }

            // Notify other servers to stop
            for (int i = 0; i < numServers; i++) {
                Registry registry = LocateRegistry.getRegistry(serverAddresses[i], 1099);
                CrackPass server = (CrackPass) registry.lookup("CrackPass");
                server.stopCracking();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
