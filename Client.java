import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

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

        try {
            // Connect to the first server
            Registry registry1 = LocateRegistry.getRegistry("192.168.123.10", 1099);
            CrackPass server1 = (CrackPass) registry1.lookup("CrackPass");

            // Connect to the second server (if specified)
            CrackPass server2 = null;
            if (numServers == 2) {
                Registry registry2 = LocateRegistry.getRegistry("192.168.123.11", 1099);
                server2 = (CrackPass) registry2.lookup("CrackPass");
            }

            // Calculate range per server and distribute tasks
            int numCombinations = (int) Math.pow(94, passwordLength);
            int rangePerServer = numCombinations / numServers;

            for (int i = 0; i < numServers; i++) {
                int startIndex = i * rangePerServer;
                int endIndex = (i == numServers - 1) ? numCombinations - 1 : (startIndex + rangePerServer - 1);

                if (i == 0) {
                    server1.crackPassword(targetHash, numThreads, passwordLength, startIndex, endIndex, i);
                } else {
                    server2.crackPassword(targetHash, numThreads, passwordLength, startIndex, endIndex, i);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}