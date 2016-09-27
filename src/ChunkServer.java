
/**
 * 							A Distributed File System based on GFS
 * 
 * This is the ChunkServer Class which is run on Chunk servers.
 * 
 * @author Shailesh Vajpayee Email: srv6224@rit.edu
 * @author Aishwary Pramanik   Email: ap9599@rit.edu
 *
 */

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class ChunkServer {

	static Object clientLock = new Object();
	static Object masterLock = new Object();

	/**
	 * The main function of the class.
	 * 
	 * @param args
	 *            2 Ports for communication.
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public static void main(String[] args) throws UnknownHostException, IOException {
		// MasterSever connection IP and Port
		System.out.println("Connection Stage 1");
		Socket socket = new Socket("localhost", 5000);
		socket.close();

		System.out.println("Ready to accept request from Master & Clients");

		Thread clientThread = new Thread() {
			clientHelper clienthelper = new clientHelper(Integer.parseInt(args[1]));

			public void run() {
				try {
					clienthelper.helpClient();
				} catch (IOException e) {
					e.printStackTrace();
				}
			};

		};
		clientThread.start();

		Thread masterThread = new Thread() {
			masterHelper masterhelper = new masterHelper(Integer.parseInt(args[0]));

			public void run() {
				try {
					masterhelper.helpMaster();
				} catch (IOException e) {
					e.printStackTrace();
				}
			};

		};
		masterThread.start();
	}

	/**
	 * This is a helper class for the Master and chunk communication.
	 */
	public static class masterHelper {
		// Master Connection Port
		static ServerSocket masterServerSocket;
		static Socket masterSocket;
		StringBuffer strBuff = new StringBuffer();

		public masterHelper(int masterPort) throws IOException {
			masterServerSocket = new ServerSocket(masterPort);
		}

		/**
		 * The function which controls communication between Master and chunk.
		 */
		public void helpMaster() throws IOException {
			while (true) {
				System.out.println("Connection Stage 2");
				masterSocket = masterServerSocket.accept();
				System.out.println("Connected to Master:" + masterSocket.getInetAddress().getHostAddress());
				synchronized (masterLock) {
					try {
						DataOutputStream masterOut = new DataOutputStream(masterSocket.getOutputStream());
						DataInputStream masterIn = new DataInputStream(masterSocket.getInputStream());
						if (masterIn.readUTF().equalsIgnoreCase("Make chunk")) {
							System.out.println("Making chunk");
							FileOutputStream fout = new FileOutputStream(new File(masterIn.readUTF() + ".txt"));
							while ((strBuff.append(masterIn.readUTF())).equals("End chunk"))
								fout.write(strBuff.toString().getBytes());
						}
					} catch (IOException e) {
						System.out.println(e);
					}
				}
			}
		}
	}

	/**
	 * This is a helper class for communication between client and chunk.
	 */
	public static class clientHelper {
		// Client Connection Port
		static ServerSocket serverSocket;
		static Socket clientSocket;

		public clientHelper(int clientPort) throws IOException {
			serverSocket = new ServerSocket(clientPort);
		}

		/**
		 * This function controls the communication between client and chunk.
		 * @throws IOException
		 */
		public void helpClient() throws IOException {
			while (true) {
				System.out.println("Connection Stage 3");
				clientSocket = serverSocket.accept();
				System.out.println("Connected to Client:" + clientSocket.getInetAddress().getHostAddress());
				synchronized (clientLock) {
					DataOutputStream clientOut = new DataOutputStream(clientSocket.getOutputStream());
					DataInputStream clientIn = new DataInputStream(clientSocket.getInputStream());
					String[] str = new String[3];
					str = (clientIn.readUTF()).split(" ");
					// change it when chunks get generated
					BufferedReader buffReader = new BufferedReader(new FileReader(str[0] + str[2] + ".txt"));
					int ctrLine = 0;
					while (ctrLine < (Integer.parseInt(str[1]) - 1)) {
						ctrLine++;
						buffReader.readLine();
					}
					clientOut.writeUTF(buffReader.readLine());
				}
			}
		}
	}
}
