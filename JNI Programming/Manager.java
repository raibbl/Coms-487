
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class Manager implements BeaconListener {
    private static ArrayList<String[]> agents = new ArrayList<String[]>();

    public Manager() {

    }

    private void askforexcute() {
        try {

            // rmi stuff
            String host = null;
            Registry registry = LocateRegistry.getRegistry(host);
            CmdAgent stub = (CmdAgent) registry.lookup("execute");
            String astring = "";
            System.out.println(stub.execute("GetLocalOs"));
            System.out.println(stub.execute("GetLocalOsTime"));

        } catch (Exception e) {
            // System.err.println(" exception: " + e.toString());
            // e.printStackTrace();
        }
    }

    public void deposit(String beacon) {

        String[] beaconrecvinarray = beacon.split("C");// parse the beacon

        String[] agent = new String[4];// adding an extra varible to the beacon to record time of last beacon
                                       // transmittion

        System.arraycopy(beaconrecvinarray, 0, agent, 0, beaconrecvinarray.length);

        String currentrecvtime = Long.toString(System.nanoTime());// note the current time

        agent[3] = currentrecvtime;

        System.out.println(beacon);

        // for (int i = 0; i < agent.length; i++) {
        // System.out.println(agent[i]);
        // }
        int agentexist = 0;

        if (agents.size() != 0) {// if there are agents already

            // check for existing agents
            for (int i = 0; i < agents.size(); i++) {
                String[] storedagent = agents.get(i);

                if (agent[0].equals(storedagent[0])) {
                    agents.get(i)[3] = currentrecvtime;// update reciv time of beacon

                    if (Long.parseLong(agent[1]) != Long.parseLong(storedagent[1])) {// somehow the
                                                                                     // startuptime is
                                                                                     // differnt,did
                                                                                     // agent restart!?
                        storedagent[1] = agent[1];// update startup time from incoming restarted beacon

                        System.out.println();
                        System.out.println(
                                "agent with id " + agent[0] + " restarted with beacause it has diffrient start uptime");
                        System.out.println();
                    }

                    agentexist = 1;
                    break;

                }

            }

            // if there is no exisint agent add it and start a thread
            if (agentexist != 1) {
                agents.add(agent);
                System.out.println();
                System.out.println("Agent with ID " + agent[0] + " has been added to the list of active agents");
                System.out.println();

                askforexcute();
            }
        } else if (agents.size() == 0) {// if there are no agents yet
            agents.add(agent);
            System.out.println();
            System.out.println("Agent with ID " + agent[0] + " has been added to the list of active agents");
            System.out.println();
            askforexcute();
        }

    }

    public static class BeaconListenerRegister extends Manager implements Runnable, BeaconListener {

        public BeaconListenerRegister() {

        }

        @Override
        public void run() {

            try {
                Manager obj = new Manager();
                BeaconListener stub = (BeaconListener) UnicastRemoteObject.exportObject(obj, 0);

                // Bind the remote object's stub in the registry
                Registry registry = LocateRegistry.getRegistry();
                registry.bind("deposit", stub);

                System.err.println("Manager ready");

            } catch (Exception e) {
                System.err.println("Manager exception: " + e.toString());
                e.printStackTrace();
            }
        }

    }

    // thread to maintain list and check if an agent dies
    public static class ManagerMaintain extends Manager implements Runnable {

        public ManagerMaintain() {

        }

        @Override
        public void run() {

            while (true) {
                // keep checking for beacons last sent time.
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    // TODO: handle exception
                }

                for (int i = 0; i < agents.size(); i++) {
                    // comparing Secs between beacons
                    long currentrecvtime = System.nanoTime();
                    long secondsBetween = Math.abs(Long.parseLong(agents.get(i)[3]) - currentrecvtime) / 1000000000;

                    if (secondsBetween > 120) { // more than two*times the interval
                                                // of agent //
                        System.out.println();
                        System.out.println("An agent with ID: " + agents.get(i)[0] + "    has died");
                        agents.remove((i));
                    }

                }

            }

        }
    }

    public static void main(String args[]) {
        BeaconListenerRegister blr = new BeaconListenerRegister();
        Thread BeaconListenerRegisterThread = new Thread(blr);
        BeaconListenerRegisterThread.start();

        ManagerMaintain mm = new ManagerMaintain();
        Thread ManagerMaintainThread = new Thread(mm);
        ManagerMaintainThread.start();
    }
}
