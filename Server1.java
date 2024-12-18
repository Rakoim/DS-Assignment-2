import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

// Server1 for binding the remote object (CrackPass) to the registry
public class Server1 {
    public static void main(String[] args) {
        try {
            // Create the remote object
            CrackPass impCrack = new ImpCrack();
            
            // Create the RMI registry on port 1099
            Registry registry = LocateRegistry.createRegistry(1099);
            
            // Bind the remote object (impCrack) to the registry with the name "CrackPass"
            registry.rebind("CrackPass", impCrack);

            // Log that the server is running and awaiting client connections
            LoggerUtil.logEvent("Server 1 is now running on port 1099, awaiting client connections...");
        } catch (RemoteException e) {
            e.printStackTrace();  // Handle potential RemoteExceptions
        }
    }
}