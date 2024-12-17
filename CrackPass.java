import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CrackPass extends Remote {
    void crackPassword(String targetHash, int numThreads, int passwordLength, int startIndex, int endIndex, int serverIndex) throws RemoteException;
    void stopCracking() throws RemoteException;
    boolean isPasswordFound() throws RemoteException;
    String getResult() throws RemoteException;
}