
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Agent implements CmdAgent {
    int version = 1;

    private native void sayHello();

    private String GetVersion() {
        String theversion = "The Version of Agent is " + version;
        return theversion;
    }

    Boolean FirstLaunch = true; // just to make CMDregister run before Beacon Sender so it can get sometime to
                                // register the bind excute for the first time

    private native String GetLocalTime(String time);

    private native String GetLocalOs(String OS);

    public String execute(String CmdID) {
        String time = "                                          ";
        String oString = "                                          ";
        String thetime=" ";
        Agent c = new Agent();
        thetime=c.GetLocalTime(time);
        oString=c.GetLocalOs(oString);

        if(CmdID.equals("GetLocalOs")){
            return thetime;
        }
        else if(CmdID.equals("GetLocalOsTime")){
           return oString;
        }
        
        return time;
    }

    // make the beacon
    public String Beacon(Integer theID) {

       

        String ID = Integer.toString(theID);// id generated acoording to time

        String currentrecvtime = Long.toString(System.nanoTime());// note the current time
        String CmdAgentId = "execute";

        String aBeacon = ID + 'C' + currentrecvtime + 'C' + CmdAgentId;

        return aBeacon;

    }

    static {
        System.loadLibrary("hello");
    }

    private Agent() {
    }

    public static class CmdRegister extends Agent implements Runnable, CmdAgent {

        public CmdRegister() {

        }

        @Override
        public void run() {

            try {
                Agent obj = new Agent();
                CmdAgent stub = (CmdAgent) UnicastRemoteObject.exportObject(obj, 0);

                // Bind the remote object's stub in the registry
                Registry registry = LocateRegistry.getRegistry();
                registry.rebind("execute", stub);

                System.err.println("Agent ready");
            } catch (Exception e) {
                // System.err.println("Server exception: " + e.toString());
                // e.printStackTrace();
            }
        }

    }

    public static class BeaconSender extends Agent implements Runnable {
        String abeacon="";
        public BeaconSender(Integer ID) {
            
            abeacon = Beacon(ID); // Intilize beacon
        }

        @Override
        public void run() {
           
            // System.out.println("I should print onlyonce");
            while (true) {
                String host = null;
                try {
                    // rmi stuff
                    if (FirstLaunch) {// make it wait for CMDRegister on first launch
                        Thread.sleep(400);
                        FirstLaunch = false;
                    }

                    Registry registry = LocateRegistry.getRegistry(host);
                    BeaconListener stub = (BeaconListener) registry.lookup("deposit");
                    stub.deposit(abeacon);

                    Thread.sleep(60000); // every 1 min send a beacon
                } catch (Exception e) {
                    System.err.println("Server exception: " + e.toString());
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        Random rand = new Random(System.nanoTime());    
        Integer ID= rand.nextInt(10000);
        if(args.length>0){
           ID=Integer.parseInt(args[0]);
        }
        
        CmdRegister cr = new CmdRegister();
        Thread CmdRegisterThread = new Thread(cr);
        CmdRegisterThread.start();

        BeaconSender bs = new BeaconSender(ID);
        Thread BeaconSenderThread = new Thread(bs);
        BeaconSenderThread.start();
       
       
    }

}
