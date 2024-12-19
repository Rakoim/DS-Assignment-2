import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CrackPass extends Remote {
    // Method to crack the password
    void crackPassword(String targetHash, int numThreads, int passwordLength, int startIndex, int endIndex, int serverIndex) throws RemoteException;

    // Method to stop cracking
    void stopCracking() throws RemoteException;

    // Method to check if password is found
    boolean isPasswordFound() throws RemoteException;

    // Method to get the result after password is found
    String getResult() throws RemoteException;

    // Method to check if server is finished
    boolean isFinished() throws RemoteException;
}