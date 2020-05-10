import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.*;
import java.io.*;
import java.util.*;
import java.sql.Timestamp;
import java.util.Date;

import sun.management.resources.agent;

public class Manger {

    static DatagramSocket ds;
    static byte[] receive = new byte[1024];
    private static ArrayList<String[]> agents = new ArrayList<String[]>();
    static DatagramPacket incoming = null;

    // Thread class to listen for incoming UDP Beacons and connections

    public static class BeaconListener extends Manger implements Runnable {

        public BeaconListener() {

        }

        @Override
        public void run() {

            try {
                while (true) {

                    incoming = new DatagramPacket(receive, receive.length);//recive incoming UDP Port

                    ds.receive(incoming);

                    StringBuilder data = new StringBuilder();

                    for (int i = 0; i < receive.length; i++) {///convert from byte[] to StringBuilder
                        if (receive[i] == 0) {
                            break; // end of transmittion
                        }
                        char c = (char) receive[i];
                     

                        data.append(c);
                    }

                    String recv = data.toString();//convert from stringbuilder to string and read the beacon


                    String[] beaconrecvinarray = recv.split("c");//parse the beacon 

                    String[] agent = new String[6];//adding an extra varible to the beacon to record time of last beacon transmittion

                    System.arraycopy(beaconrecvinarray, 0, agent, 0, beaconrecvinarray.length);
                    // adding current recv time to agent before storing it

                    String currentrecvtime = Long.toString(System.nanoTime());//note the current time

                    agent[5] = currentrecvtime; //this is the time the beacon has been recived 

                    int agentexist = 0;
                   
                    if (agents.size() != 0) {// if there are agents already

                        // check for existing agents
                        for (int i = 0; i < agents.size(); i++) {
                            String[] storedagent = agents.get(i);

                            if (agent[0].equals(storedagent[0])) {
                                agents.get(i)[5] = currentrecvtime;// update reciv time of beacon

                                if (Integer.parseInt(agent[1]) != Integer.parseInt(storedagent[1])) {// somehow the
                                                                                                     // startuptime is
                                                                                                     // differnt,did
                                                                                                     // agent restart!?
                                    storedagent[1] = agent[1];//update startup time from incoming restarted beacon
                                    storedagent[4]=agent[4];//update CMD Port as well
                                    System.out.println();
                                    System.out.println("agent with id " + agent[0]
                                            + " restarted with beacause it has diffrient start uptime");
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
                            System.out.println(
                                    "Agent with ID " + agent[0] + " has been added to the list of active agents");
                            System.out.println();

                            ClientAgent ag = new ClientAgent(Integer.parseInt(agent[4]));//start a thread to send the required commands
                            Thread clientAgent = new Thread(ag);
                            clientAgent.start();
                        }
                    } else if (agents.size() == 0) {// if there are no agents yet
                        agents.add(agent);
                        System.out.println();
                        System.out
                                .println("Agent with ID " + agent[0] + " has been added to the list of active agents");
                        System.out.println();
                        ClientAgent ag1 = new ClientAgent(Integer.parseInt(agent[4]));//start a thread to send the required commands
                        Thread clientAgent = new Thread(ag1);
                        clientAgent.start();
                    }
                  

                    System.out.println();
                    System.out.println("Agent beacon Recevied with:");
                    
                    
                    // print beacon infomration
                    for (int i = 0; i < 5; i++) {
                        String s = "";
                        if (i == 0) {
                            s = "ID=";
                        } else if (i == 1) {
                            s = "Startup time=";
                        } else if (i == 2) {
                            s = "TimeInterval=";
                        } else if (i == 3) {
                            s = "TimeInterval=";
                        } else {
                            s = "CMD Port=";
                        }

                        System.out.println(s + agent[i]);

                    }
                    System.out.println();
                    // reset buffer
                    receive = new byte[1024];

                }
            } catch (Exception e) {
                // TODO: handle exception
            }

        }
    }

    // thread to maintain list and check if an agent dies
    public static class AgentMonitor extends Manger implements Runnable {

        public AgentMonitor() {

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
                    long secondsBetween = Math.abs(Long.parseLong(agents.get(i)[5]) - currentrecvtime) / 1000000000;

                    if (secondsBetween > Integer.parseInt(agents.get(i)[2]) * 2) { // more than two*times the interval
                                                                                   // of agent //
                        System.out.println();
                        System.out.println("An agent with ID: " + agents.get(i)[0] + "    has died");
                        agents.remove((i));
                    }

                }

            }

        }
    }

    // thread to spawn when a new agent has been detected and send the commands
    // required from the agent and recive their result

    public static class ClientAgent extends Manger implements Runnable {

        int cmdport = 0;

        public ClientAgent(int cmdport) {

            this.cmdport = cmdport;

        }

        // helper fucntion to use
        static private byte[] toBytes(int i) {
            byte[] result = new byte[4];

            result[0] = (byte) (i >> 24);
            result[1] = (byte) (i >> 16);
            result[2] = (byte) (i >> 8);
            result[3] = (byte) (i /* >> 0 */);

            return result;
        }

        @Override
        public void run() {

            String c = "o                                                        ";// string to use as buffer
            boolean exit = false;// while loop exit condition

            System.out.println();
            int x = 0;

            // note the time before the exexcution of requests
            long time_before_requests = System.nanoTime();
            while (!exit) {
                try {

                    String serverIP = "127.0.0.1";

                    // create a socket
                    Socket clientSocket = new Socket(serverIP, cmdport);

                    // create text reader and writer
                    DataInputStream inStream = new DataInputStream(clientSocket.getInputStream());
                    DataOutputStream outStream = new DataOutputStream(clientSocket.getOutputStream());

                    // prepare a binary buffer

                    byte[] buf = c.getBytes();

                    // send the length, and then buffer
                    byte[] bufLengthInBinary = toBytes(buf.length);

                    // System.out.println("im here2");

                    // send 4 bytes
                    outStream.write(bufLengthInBinary, 0, bufLengthInBinary.length);
                    // send the string
                    outStream.write(buf, 0, buf.length);
                    outStream.flush();

                    // read the data back
                    inStream.readFully(bufLengthInBinary); // ignore the first 4 bytes
                    inStream.readFully(buf); //

                    // convert the binary bytes to string
                    String ret = new String(buf);

                    if (x == 1) {/// if we are getting the result for the time funciton
                        ret = ret.substring(0, 16);
                        System.out.println("this is the local time for agent: ");
                        System.out.println(ret);

                    }

                    if (x == 0) {// if we are getting the result for our operating system function
                        System.out.println("this is the local operating system for agent: ");
                        ret = ret.substring(0, 28);
                        System.out.println(ret);
                    }

                    x++;// move on to the next time function

                    if (x > 1) { // we need to stop now because we have done all our functions
                        // System.out.println("hey stop");
                        long time_after_requests = System.nanoTime();// record time of end of requests
                        long secondsBetween = Math.abs(time_after_requests - time_before_requests);
                        System.out.println();
                        System.out.println("time for excution of requests was " + secondsBetween + " nanosecs");
                        exit = true;
                    }

                    if (x == 1) {// looks like it is time to send our time request
                        // send command for time
                        c = "t                                                        ";
                    }

                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {

        ds = new DatagramSocket(4444);// needed to start a udp connection

        // start our beaconlistener thread
        BeaconListener b1 = new BeaconListener();
        Thread beaconListenerThread = new Thread(b1);
        beaconListenerThread.start();

        // start our agentMonitor thread
        AgentMonitor ag = new AgentMonitor();
        Thread AgentMonitorThread = new Thread(ag);
        AgentMonitorThread.start();

    }
}
