/* CrackPass.java
This interface defines the Remote Methods for the password-cracking server.
It provides methods to start and stop the password-cracking task, check its status, 
and retrieve the result when the password is found. */
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CrackPass extends Remote {
    // Method to crack the password
    void crackPassword(String targetHash, int numThreads, int passwordLength, long startIndex, long endIndex, int serverIndex) throws RemoteException;

    // Method to stop cracking
    void stopCracking() throws RemoteException;

    // Method to check if password is found
    boolean isPasswordFound() throws RemoteException;

    // Method to get the result after password is found
    String getResult() throws RemoteException;

    // Method to check if server is finished
    boolean isFinished() throws RemoteException;
}