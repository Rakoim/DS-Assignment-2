/* Server1.java
This file initializes Server 2 for the distributed MD5 hash cracking system. 
It binds the remote object (implementation of CrackPass) to the RMI registry 
on port 1099, allowing clients to invoke remote methods.

Usage:
- Run this class to start Server 2.
- Ensure the client is configured to connect to Server 1 using the correct host and port details.

Note:
- Server 1 and Server 2 use the same remote object interface but can be deployed 
independently on different machines for load distribution.*/
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

// Server 2 for binding the remote object (CrackPass) to the registry
public class Server2 {
    public static void main(String[] args) {
        try {
            // Create the remote object
            CrackPass impCrack = new ImpCrack();
            
            // Create the RMI registry on port 1099
            Registry registry = LocateRegistry.createRegistry(1099);
            
            // Bind the remote object (impCrack) to the registry with the name "CrackPass"
            registry.rebind("CrackPass", impCrack);

            // Log that the server is running and awaiting client connections
            LoggerUtil.logEvent("Server 2 is now running on port 1099, awaiting client connections...");
        } catch (RemoteException e) {
            e.printStackTrace();  // Handle potential RemoteExceptions
        }
    }
}