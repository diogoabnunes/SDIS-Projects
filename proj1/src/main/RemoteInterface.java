package main;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {
    void backup(String message) throws RemoteException;
    void restore(String message) throws RemoteException;
    void delete(String message) throws RemoteException;
    void reclaim(String message) throws RemoteException;
    void state(String message) throws RemoteException;
}
