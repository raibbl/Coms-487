

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BeaconListener extends Remote {
    public void deposit(String beacon) throws RemoteException; //put p to the list
}
