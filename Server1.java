import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Server1 {
    public static void main(String[] args) {
        try {
            // Create the remote object
            CrackPass impCrack = new ImpCrack();
            
            // Create and bind the registry
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("CrackPass", impCrack);

            System.out.println("Server 1 running at 192.168.123.10, waiting for clients...");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}