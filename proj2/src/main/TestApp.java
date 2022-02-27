package main;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import main.exceptions.*;
import main.auxiliar.*;

public class TestApp {

    private String accessPoint;
    private String operation;
    private String pathName = null;
    private String amountDiskSpace = null;
    private String replicationDegree = null;

    public static void main(String[] args) {
        TestApp testApp = new TestApp();

        try {
            testApp.processInput(args);
            testApp.sendRequest();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // ------------------------------------------
    //              SEND REQUEST
    // ------------------------------------------

    public void sendRequest() throws Exception{
        Registry registry = LocateRegistry.getRegistry("localhost");
        RemoteInterface stub = (RemoteInterface) registry.lookup(this.accessPoint);

        String message = this.buildMessage();

        switch (this.operation) {
            case "BACKUP" -> this.sendBackupRequest(stub, message);
            case "RESTORE" -> this.sendRestoreRequest(stub, message);
            case "DELETE" -> this.sendDeleteRequest(stub, message);
            case "RECLAIM" -> this.sendReclaimRequest(stub, message);
            default -> this.sendStateRequest(stub, message);
        }
    }

    public String buildMessage() {
        String ret = "";
        if (this.pathName != null) ret += this.pathName;
        if (this.amountDiskSpace != null) ret += this.amountDiskSpace;
        if (this.replicationDegree != null) ret += " " + this.replicationDegree;
        return ret;
    }

    public void sendBackupRequest(RemoteInterface stub, String message) throws Exception {
        System.out.println(ConsoleColors.BLUE + "Sending Backup Request to Peer: " + this.accessPoint + ConsoleColors.RESET);
        System.out.println(ConsoleColors.BLUE + "Message: " + message + ConsoleColors.RESET);
        stub.backup(message);
    }

    public void sendRestoreRequest(RemoteInterface stub, String message) throws Exception {
        System.out.println(ConsoleColors.BLUE + "Sending Restore Request to Peer: " + this.accessPoint + ConsoleColors.RESET);
        System.out.println(ConsoleColors.BLUE + "Message: " + message + ConsoleColors.RESET);
        stub.restore(message);
    }

    public void sendDeleteRequest(RemoteInterface stub, String message) throws Exception {
        System.out.println(ConsoleColors.BLUE + "Sending Delete Request to Peer: " + this.accessPoint + ConsoleColors.RESET);
        System.out.println(ConsoleColors.BLUE + "Message: " + message + ConsoleColors.RESET);
        stub.delete(message);
    }

    public void sendReclaimRequest(RemoteInterface stub, String message) throws Exception {
        System.out.println(ConsoleColors.BLUE + "Sending Reclaim Request to Peer: " + this.accessPoint + ConsoleColors.RESET);
        System.out.println(ConsoleColors.BLUE + "Message: " + message + ConsoleColors.RESET);
        stub.reclaim(message);
    }

    public void sendStateRequest(RemoteInterface stub, String message) throws Exception {
        System.out.println(ConsoleColors.BLUE + "Sending State Request to Peer: " + this.accessPoint + ConsoleColors.RESET);
        System.out.println(ConsoleColors.BLUE + "Message: " + message + ConsoleColors.RESET);
        stub.state(message);
    }

    // ------------------------------------------
    //              PROCESS INPUT
    // ------------------------------------------

    public void processInput(String[] args) throws InvalidArgumentsException {
        if(!(args.length >= 2 && args.length <= 4)) throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>" + ConsoleColors.RESET);

        this.processAccessPoint(args[0]);
        this.processOperation(args[1]);
        if(args.length > 2) this.processThirdArgument(args[2]);
        if(args.length > 3) this.processLastArgument(args[3]);

        if((this.operation.equals("BACKUP") || this.operation.equals("RESTORE") || this.operation.equals("DELETE")) && this.pathName == null)
            throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>" + ConsoleColors.RESET);
        if(this.operation.equals("RECLAIM") && this.amountDiskSpace == null)
            throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>" + ConsoleColors.RESET);
        if(this.operation.equals("BACKUP") && this.replicationDegree == null)
            throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>" + ConsoleColors.RESET);
    }

    public void processAccessPoint(String accessPoint) throws InvalidArgumentsException {
        if(!accessPoint.contains(":") && !Utils.isInteger(accessPoint)) throw new InvalidArgumentsException("4");
        else if(!accessPoint.contains(":") && Utils.isInteger(accessPoint)) this.accessPoint = accessPoint;
        else {
            String[] parts = accessPoint.split(":");
            if(parts.length != 2 || !Utils.isInteger(parts[1])) throw new InvalidArgumentsException("5");

            this.accessPoint = accessPoint;
        }
    }

    public void processOperation(String operation) throws InvalidArgumentsException {
        String op = operation.toUpperCase();

        if(!op.equals("BACKUP") && !op.equals("RESTORE") && !op.equals("DELETE") && !op.equals("RECLAIM") && !op.equals("STATE"))
            throw new InvalidArgumentsException(ConsoleColors.RED + "ERROR: Operation " + op + " unknown..." + ConsoleColors.RESET);

        this.operation = op;
    }

    public void processThirdArgument(String arg) throws InvalidArgumentsException {
        if(this.operation.equals("BACKUP") || this.operation.equals("RESTORE") || this.operation.equals("DELETE")) {
            if(!Utils.isInteger(arg)) this.pathName = arg;
            else throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>" + ConsoleColors.RESET);
        }
        else if(this.operation.equals("RECLAIM")) {
            if(!Utils.isInteger(arg)) throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>" + ConsoleColors.RESET);
            else this.amountDiskSpace = arg;
        }
        else throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>" + ConsoleColors.RESET);
    }

    public void processLastArgument(String arg) throws InvalidArgumentsException {
        if(this.operation.equals("BACKUP") && Utils.isInteger(arg)) this.replicationDegree = arg;
        else throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>" + ConsoleColors.RESET);
    }
}
