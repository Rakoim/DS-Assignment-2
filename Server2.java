import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Server2 {
    public static void main(String[] args) {
        try {
            // Create the remote object
            CrackPass impCrack = new ImpCrack();
            
            // Create and bind the registry
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("CrackPass", impCrack);

            System.out.println("Server 2 is now running on 192.168.123.11 (port 1099), awaiting client connections...");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}