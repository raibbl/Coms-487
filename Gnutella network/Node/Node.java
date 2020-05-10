
// A Java program for a Server 

import java.net.*;
import java.util.ArrayList;
import java.util.UUID;

import jdk.net.Sockets;

import java.util.Random;
import java.io.*;
import java.io.File;

public class Node {
	// initialize socket and input stream

	private DataInputStream in = null;
	private static int num = 0;
	private static ArrayList<String> IDs = new ArrayList<String>();
	private static ArrayList<Long> IDstime = new ArrayList<Long>();
	private static ArrayList<String> DoneIDs = new ArrayList<String>();
	private static ArrayList<Socket> sockets = new ArrayList<Socket>();
	private static ArrayList<String> files = new ArrayList<String>();
	private static ArrayList<String> quries = new ArrayList<String>();
	private static long lastTime = 0;
	private static String uniqueID = "";
	private static Boolean seed = false;
	private static String command = "c";
	private static int port = 0;
	private static boolean commandDone=false;

	private static void sendmsgTcp(Socket socket, String msg) throws IOException {

		OutputStream output = socket.getOutputStream();
		PrintWriter writer = new PrintWriter(output, true);
		writer.println(msg);
		// System.out.println("x");
	}

	private static void getFilesinfo() {
		File folder = new File("f");

		File[] filesinf = folder.listFiles();

		for (File file : filesinf) {
			files.add(file.getName());
		//	System.out.println(file.getName());
		}
	}

	private static void sendFile(Socket socket, String fileName) {
		try {
			
			FileInputStream filestream = new FileInputStream("f\\" + fileName);
		
			byte b[] = new byte[1000000];
			filestream.read(b, 0, b.length);
			OutputStream os = socket.getOutputStream();
			os.flush();
			os.write(b,0,b.length);
			os.flush();
			filestream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void recvFile(Socket socket, String fileName) {
		try {

			byte b[] = new byte[1000000];
			InputStream stream = socket.getInputStream();
			FileOutputStream filestream = new FileOutputStream("f\\" + fileName);
			
			System.out.println("this ishimm "+b.toString());
		//	stream.skip(25);
			stream.read(b, 0, b.length);
			filestream.write(b, 0, b.length);
			filestream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void handleQuery(String q) {
		String[] arrOfStr = q.split("-");
		Socket socket = null;
		 System.out.println("handling Query");
		for (int i = 0; i < files.size(); i++) {
			 System.out.println("Checking file");
			if (files.get(i).equals(arrOfStr[4])) {// if file found on this node
				System.out.println("found file");
				
				try {
					socket = new Socket("127.0.0.1",Integer.parseInt(arrOfStr[1]));
				} catch (Exception e) {
					//TODO: handle exception
				}
				
				writeTcpMsg wMsg = new writeTcpMsg(socket, socket.getPort(),
						"file:" + arrOfStr[4] + "found at " + uniqueID, true, 10000);
				Thread writeTcpMsgThread = new Thread(wMsg);
				writeTcpMsgThread.start();

				// send the actual file

				sendFile(socket, arrOfStr[4]);
				 System.out.println("Done Query?");
			}
			// System.out.println(file.getName());
		}
	}

	private static boolean readDataTcp(boolean donotprint, InputStream input, BufferedReader reader)
			throws IOException {

		String line = reader.readLine();
		if (line.contains("found")) {
			System.out.println(line);
			return true;
		}
		if (line.contains("Query")) {// if it is a Query
			//System.out.println(quries.size());
			for(int i =0;i<quries.size();i++){
				if(quries.get(i).equals(line)){
					return false;
				}
			}
			quries.add(line);
			System.out.println(line);
			handleQuery(line);
			return false;
		}
		if(!line.contains("localhost")){
		 	return false;
		 }

		boolean idExists = false;

		if (line != null) {
			// System.out.println(IDs.size());
			for (int i = 0; i < IDs.size(); i++) {

				if (IDs.get(i).equals(line)) {
					idExists = true;
					IDstime.set(i, System.nanoTime());
				}
			}

			if (!line.contains("-") && !line.contains("Server")) {
				// System.out.println("im Reading");
				if (!donotprint) {
					System.out.println(line);
				}

			}

			else if (!idExists && line.contains("-") && !line.contains("Query")) {
				System.out.println(line);
				IDs.add(line);
				IDstime.add(System.nanoTime());
				String[] arrOfStr = line.split("-");
				// ConnecttoNewtwork(port);

				if (!line.equals(uniqueID)) {
				//	System.out.println("Connecting to " + arrOfStr[0]);
					ConnecttoNewtwork(Integer.parseInt(arrOfStr[0]));
				}
				if (!donotprint && !line.equals(uniqueID)) {
					System.out.println(line);
				}
				return false;
			}
			// System.out.println(line);
			else if (line.contains("-") && !line.equals(uniqueID)) {
				if (!donotprint) {
					System.out.println(line);
				}
			} else {
				if (!line.contains("Server")) {
					if (!donotprint) {
						System.out.println(line);
					}
				}
				lastTime = System.nanoTime();

				// System.out.print("lasttime: " + lastTime);
			}

		}

		return false;
		// reads a single character
	}

	

	public static class ReadTcpMsg implements Runnable {
		// static int port;
		Socket socket;
		boolean runOrNot = true;
		boolean donotprint = false;

		public ReadTcpMsg(boolean donotprint, Socket socket) {
			this.socket = socket;
			this.donotprint = donotprint;
		}

		@Override
		public void run() {
			InputStream input = null;
			BufferedReader reader = null;
			try {
				input = socket.getInputStream();
				reader = new BufferedReader(new InputStreamReader(input));

			} catch (Exception e) {
				// TODO: handle exception
			}
			while (runOrNot) {
				try {

					boolean haveseenNode = readDataTcp(donotprint, input, reader);
					// System.out.println("mama")
					if (haveseenNode) {
						Thread.sleep(200);
						recvFile(socket, command);
						// sockets.add(socket);
						// System.out.println(sockets.size());
					}

					Thread.sleep(200);
				
				
				} catch (Exception e) {
					

				}

			}

			while (true)

			{
			
			}
		}

	}

	public static class broadcast implements Runnable {
		public broadcast() {

		}

		@Override
		public void run() {

			// if (seed) {
			for (int i = 0; i < sockets.size(); i++) {
				
				Socket socket = sockets.get(i);
				// System.out.println("hi");
				for (int j = 0; j < IDs.size(); j++) {
					if (i != j) {
						// broadcast message to all ids

						writeTcpMsg wMsg = new writeTcpMsg(socket, port, IDs.get(j), true, 10000); // send
					
						Thread writeTcpMsgThread = new Thread(wMsg);
						writeTcpMsgThread.start();
					
						try {
							Thread.sleep(2000);
						} catch (Exception e) {
							// TODO: handle exception
						}

						if (!command.equals("c")&&!commandDone) {
							commandDone=true;
						wMsg = new writeTcpMsg(socket, port, SendCommandString(5), true, 100); // send an id all

							writeTcpMsgThread = new Thread(wMsg);
							writeTcpMsgThread.start();
						}
					}
				}
			
			}
			

		}
		
	}

	public static class writeTcpMsg implements Runnable {
		// static int port;
		Socket socket;
		int port;
		String msg;
		boolean runOrNot = false;
		boolean something = true;
		int time;

		public writeTcpMsg(Socket socket, int port, String msg, boolean runOrNot, int time) {

			this.port = port;
			this.socket = socket;
			this.msg = msg;
			this.runOrNot = runOrNot;
			this.time = time;
		}

		public void stop() {
			something = false;
		}

		@Override
		public void run() {

			while (something) {

				try {

					sendmsgTcp(socket, msg);

					if (runOrNot) {
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(time);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		}

	}

	public static class serverAccept implements Runnable {
		// static int port;
		int port;
		ServerSocket server;
		Socket socket;

		public serverAccept(ServerSocket server, int port, Socket socket) {
			this.port = port;
			this.server = server;
			this.socket = socket;
		}

		@Override
		public void run() {

			boolean binded = false;
			while (true) {
				try {
					if (server == null) {
						server = new ServerSocket(port);
						binded = true;

						System.out.println("hey");
					}

					socket = server.accept();
					
					if (socket != null) {
						System.out.println("I accept");
						sockets.add(socket);
						ReadTcpMsg rTM = new ReadTcpMsg(false, socket);
						Thread rTMThread = new Thread(rTM);
						rTMThread.start();

						
						writeTcpMsg wMsg = new writeTcpMsg(socket, port, uniqueID, false, 10000);
						Thread writeTcpMsgThread = new Thread(wMsg);
						writeTcpMsgThread.start();

						broadcast b = new broadcast();
						Thread bThread = new Thread(b);
						bThread.start();

					
					//	System.out.print(sockets.size());

					}
				
				} catch (Exception e) {
					// TODO: handle exception
					System.out.println(e);
				}

			}

		}

	}

	public static void StartNetwork(int port) throws IOException {
		Socket socket = null;
		ServerSocket server = null;
		
		System.out.println("Server started");

		System.out.println("Waiting for a client ...");

		try {
			serverAccept sA = new serverAccept(server, port, socket);
			Thread sAThread = new Thread(sA);
			sAThread.start();

		} catch (Exception e) {
			// e.printStackTrace();
		}

		
	}

	public static void ConnecttoNewtwork(int port) throws IOException {
		Socket socket = null;
		ServerSocket server = null;
		socket = new Socket("127.0.0.1", port);
		sockets.add(socket);
		
		System.out.println("Connected I'm client");

		
		try {
			writeTcpMsg idMsg = new writeTcpMsg(socket, port, uniqueID, false, 10000); // send an id all the time
			Thread writeTcpMsgThread = new Thread(idMsg);
			writeTcpMsgThread.start();

			ReadTcpMsg rTM = new ReadTcpMsg(false, socket);
			Thread rTMThread = new Thread(rTM);
			rTMThread.start();

			rTM = new ReadTcpMsg(true, socket);
			rTMThread = new Thread(rTM);
			rTMThread.start();

		} catch (Exception e) {
			// e.printStackTrace();
		}

	}

	public static String SendCommandString(int TTL) {

		Random r = new Random();
		r.setSeed(System.nanoTime());
		String tosend = "Query" + '-' + uniqueID + '-' + command + '-' + (r.nextInt(8000 - 5000 + 1) + 5000) + '-' + TTL
				+ '-';
		quries.add(tosend);
		return tosend;

	}

	// constructor with port
	public Node(int port) {
		try {
		
			Random r = new Random();
			r.setSeed(System.nanoTime());
			if (!seed) {
				uniqueID = String.valueOf(r.nextInt(8000 - 5000 + 1) + 5000);
			} else {
				uniqueID = String.valueOf(4000);

			}

			getFilesinfo();
			String localhost="localhost";
			uniqueID = uniqueID + '-' + files.size() + '-' + localhost;

		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	public static void main(String args[]) {

		if (args.length == 0) {
			seed = true;
		}
		try {
			Node n = new Node(port);
			

			if (args.length > 0) {
				port = Integer.parseInt(args[0]);
				System.out.println(args[0]);
				String[] arrOfStr = uniqueID.split("-");
				if (args.length > 1) {

					command = args[1];
					System.out.println(command);

				}
				ConnecttoNewtwork(port);
				StartNetwork(Integer.parseInt(arrOfStr[0]));
			} else {

				StartNetwork(4000);
			}

		} catch (Exception e) {
			e.printStackTrace();// TODO: handle exception
		}

	}
}
