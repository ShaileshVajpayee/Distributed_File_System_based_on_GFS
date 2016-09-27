
/**
 * 							A Distributed File System based on GFS
 * 
 * This is the MasterServer Class which runs the Masters.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class MasterServer {
	static HashMap<Integer, String> NamespaceMap = new HashMap<Integer, String>();
	static HashMap<Integer, ArrayList<String>> ReplicationMap = new HashMap<Integer, ArrayList<String>>();
	static HashMap<Integer, Integer> PortMap = new HashMap<Integer, Integer>();
	static Crawler crawler = new Crawler();
	static Socket clientSocket;
	static Object Lock = new Object();
	static int clientCtr;
	static ServerSocket serverSocket;
	static int chunkNum;

	/**
	 * The main function of the MasterServer.
	 * 
	 * @param args
	 *            Master Port and Client port for chunk
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		int chunkCtr = 0;
		// args[1] -> Monitor IP
		// args[0] -> communication port with the individual clients
		serverSocket = new ServerSocket(Integer.parseInt(args[0]));
		System.out.println("Master Server " + Inet4Address.getLocalHost().getHostAddress() + " Started");
		File file = new File("Namespace.csv");

		// monitor Communication
		try {
			Socket monitorSocket = new Socket(args[1], 7777);
			DataInputStream in = new DataInputStream(monitorSocket.getInputStream());
			DataOutputStream out = new DataOutputStream(monitorSocket.getOutputStream());
			System.out.println(Inet4Address.getLocalHost().getHostAddress() + " " + args[0]);
			out.writeUTF(Inet4Address.getLocalHost().getHostAddress() + " " + args[0]);
			Thread monitorthread = new Thread() {
				public void run() {
					while (true) {
						try {
							Thread.sleep(3000);
							out.writeUTF(clientCtr + "");
						} catch (IOException | InterruptedException e) {
						}
					}
				}
			};

			monitorthread.start();
		} catch (IOException e) {
			System.err.println("Unable to Connect to Monitor");
			System.exit(0);
		}

		PortMap.put(1, 9876);
		PortMap.put(2, 8765);
		PortMap.put(3, 7654);
		PortMap.put(4, 6543);
		PortMap.put(5, 5439);
		PortMap.put(6, 4398);

		// Chunk Interactions
		if (!file.exists()) {
			ServerSocket chunkServerSocket;
			Socket chunkSocket = new Socket();
			// args[1] -> communication port with the individual chunks
			chunkServerSocket = new ServerSocket(5000);
			System.out.println("Waiting for chunk servers to proceed!");
			FileOutputStream fout = new FileOutputStream(file);
			while (chunkCtr < 6) {
				chunkCtr++;
				chunkSocket = chunkServerSocket.accept();
				System.out.println(" Connected to: " + chunkSocket.getInetAddress().getLocalHost().getHostAddress()
						+ " Chunk Name: Chunk" + chunkCtr);
				fout.write((chunkCtr + "," + chunkSocket.getInetAddress().getLocalHost().getHostAddress()).getBytes());
				fout.write("\n".getBytes());
				NamespaceMap.put(chunkCtr, chunkSocket.getInetAddress().getLocalHost().getHostAddress());

			}
			chunkSocket.close();
			chunkServerSocket.close();

		} else {
			BufferedReader buffReader = new BufferedReader(new FileReader(file));
			String str;
			while ((str = buffReader.readLine()) != null) {
				NamespaceMap.put(Integer.parseInt(str.substring(0, 1)), (str.substring(2, str.length() - 1)));
			}
		}
		generateReplica();

		System.out.println(PortMap);
		System.out.println(NamespaceMap);
		System.out.println(ReplicationMap);
		System.out.println("Ready to receive request from Client");

		// Client Communication
		while (true) {
			clientSocket = serverSocket.accept();
			Thread clientThread = new Thread() {
				public void run() {
					try {
						System.out.println("Interacting with Client: "
								+ clientSocket.getInetAddress().getLocalHost().getHostAddress());
						clientCtr++;
						DataInputStream in = new DataInputStream(clientSocket.getInputStream());
						DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
						String[] input = new String[2];
						input = (in.readUTF()).split(" ");
						System.out.println("Client: " + input[0] + " " + input[1]);
						File file = new File(input[0] + "complete.txt");
						chunkNum = (int) Math.ceil(((Integer.parseInt(input[1])) / 800)) + 1;
						if (!file.exists()) {
							System.out.println("Generating Complete file and Distributing it to the chunks");
							String fl = runCrawler(input[0]);
							DataInputStream chunkIn;
							DataOutputStream chunkOut;
							FileInputStream fin = new FileInputStream(new File(fl));
							StringBuffer strBuff1 = new StringBuffer();
							StringBuffer strBuff2 = new StringBuffer();
							StringBuffer strBuff3 = new StringBuffer();
							StringBuffer strBuff4 = new StringBuffer();
							StringBuffer strBuff5 = new StringBuffer();
							StringBuffer strBuff6 = new StringBuffer();
							int lineCtr = 0;

							while (lineCtr < 4800) {
								lineCtr++;
								if (lineCtr <= 800)
									strBuff1.append((char) fin.read());
								else if (lineCtr > 800 && lineCtr <= 1600)
									strBuff2.append((char) fin.read());
								else if (lineCtr > 1600 && lineCtr <= 2400)
									strBuff3.append((char) fin.read());
								else if (lineCtr > 2400 && lineCtr <= 3200)
									strBuff4.append((char) fin.read());
								else if (lineCtr > 3200 && lineCtr <= 4000)
									strBuff5.append((char) fin.read());
								else
									strBuff6.append((char) fin.read());
							}
							// chunk 1

							Socket chunkSocket = new Socket(NamespaceMap.get(1), 3456);
							chunkIn = new DataInputStream(chunkSocket.getInputStream());
							chunkOut = new DataOutputStream(chunkSocket.getOutputStream());

							chunkOut.writeUTF("Make chunk");
							chunkOut.writeUTF(input[0] + "1");
							chunkOut.writeUTF(strBuff1.toString());
							chunkOut.writeUTF("End chunk");

							chunkOut.writeUTF("Make chunk");
							chunkOut.writeUTF(input[0] + "2Rep");
							chunkOut.writeUTF(strBuff2.toString());
							chunkOut.writeUTF("End chunk");

							chunkOut.writeUTF("Make chunk");
							chunkOut.writeUTF(input[0] + "3Rep");
							chunkOut.writeUTF(strBuff3.toString());
							chunkOut.writeUTF("End chunk");

							// chunk 2
							chunkSocket = new Socket(NamespaceMap.get(2), 4567);
							chunkIn = new DataInputStream(chunkSocket.getInputStream());
							chunkOut = new DataOutputStream(chunkSocket.getOutputStream());

							chunkOut.writeUTF("Make chunk");
							chunkOut.writeUTF(input[0] + "2");
							chunkOut.writeUTF(strBuff2.toString());
							chunkOut.writeUTF("End chunk");

							chunkOut.writeUTF("Make chunk");
							chunkOut.writeUTF(input[0] + "3Rep");
							chunkOut.writeUTF(strBuff3.toString());
							chunkOut.writeUTF("End chunk");

							chunkOut.writeUTF("Make chunk");
							chunkOut.writeUTF(input[0] + "4Rep");
							chunkOut.writeUTF(strBuff4.toString());
							chunkOut.writeUTF("End chunk");

							// chunk 3
							chunkSocket = new Socket(NamespaceMap.get(3), 5678);
							chunkIn = new DataInputStream(chunkSocket.getInputStream());
							chunkOut = new DataOutputStream(chunkSocket.getOutputStream());

							chunkOut.writeUTF("Make chunk");
							chunkOut.writeUTF(input[0] + "3");
							chunkOut.writeUTF(strBuff3.toString());
							chunkOut.writeUTF("End chunk");

							chunkOut.writeUTF("Make chunk");
							chunkOut.writeUTF(input[0] + "4Rep");
							chunkOut.writeUTF(strBuff4.toString());
							chunkOut.writeUTF("End chunk");

							chunkOut.writeUTF("Make chunk");
							chunkOut.writeUTF(input[0] + "5Rep");
							chunkOut.writeUTF(strBuff5.toString());
							chunkOut.writeUTF("End chunk");

							// chunk 4
							chunkSocket = new Socket(NamespaceMap.get(4), 6789);
							chunkIn = new DataInputStream(chunkSocket.getInputStream());
							chunkOut = new DataOutputStream(chunkSocket.getOutputStream());

							chunkOut.writeUTF("Make chunk");
							chunkOut.writeUTF(input[0] + "4");
							chunkOut.writeUTF(strBuff4.toString());
							chunkOut.writeUTF("End chunk");

							chunkOut.writeUTF("Make chunk");
							chunkOut.writeUTF(input[0] + "5Rep");
							chunkOut.writeUTF(strBuff5.toString());
							chunkOut.writeUTF("End chunk");

							chunkOut.writeUTF("Make chunk");
							chunkOut.writeUTF(input[0] + "6Rep");
							chunkOut.writeUTF(strBuff6.toString());
							chunkOut.writeUTF("End chunk");

							// chunk 5
							chunkSocket = new Socket(NamespaceMap.get(5), 7893);
							chunkIn = new DataInputStream(chunkSocket.getInputStream());
							chunkOut = new DataOutputStream(chunkSocket.getOutputStream());

							chunkOut.writeUTF("Make chunk");
							chunkOut.writeUTF(input[0] + "5");
							chunkOut.writeUTF(strBuff5.toString());
							chunkOut.writeUTF("End chunk");

							chunkOut.writeUTF("Make chunk");
							chunkOut.writeUTF(input[0] + "1Rep");
							chunkOut.writeUTF(strBuff1.toString());
							chunkOut.writeUTF("End chunk");

							chunkOut.writeUTF("Make chunk");
							chunkOut.writeUTF(input[0] + "6Rep");
							chunkOut.writeUTF(strBuff6.toString());
							chunkOut.writeUTF("End chunk");

							// chunk 6
							chunkSocket = new Socket(NamespaceMap.get(6), 8934);
							chunkIn = new DataInputStream(chunkSocket.getInputStream());
							chunkOut = new DataOutputStream(chunkSocket.getOutputStream());

							chunkOut.writeUTF("Make chunk");
							chunkOut.writeUTF(input[0] + "6");
							chunkOut.writeUTF(strBuff6.toString());
							chunkOut.writeUTF("End chunk");

							chunkOut.writeUTF("Make chunk");
							chunkOut.writeUTF(input[0] + "1Rep");
							chunkOut.writeUTF(strBuff1.toString());
							chunkOut.writeUTF("End chunk");

							chunkOut.writeUTF("Make chunk");
							chunkOut.writeUTF(input[0] + "2Rep");
							chunkOut.writeUTF(strBuff2.toString());
							chunkOut.writeUTF("End chunk");

						}
						System.out.println("Chunk: " + chunkNum + " : " + PortMap.get(chunkNum) + " " + chunkNum);
						// out.writeUTF("localhost "+PortMap.get(chunkNum));
						// out.writeUTF(NamespaceMap.get(chunkNum)+"
						// "+PortMap.get(chunkNum)+" "+chunkNum);
						out.writeUTF(NamespaceMap.get(chunkNum) + " " + PortMap.get(chunkNum) + " complete");
						System.out.println("Sent chunk: " + NamespaceMap.get(chunkNum) + " " + PortMap.get(chunkNum));
						clientCtr--;
					} catch (IOException e) {
						System.err.println(e);
					}
				}
			};
			clientThread.start();
		}
	}

	/**
	 * This function helps in generating replica for chunk server files.
	 */
	private static void generateReplica() {
		ArrayList<String> rep = new ArrayList<String>();
		rep.add(NamespaceMap.get(5));
		rep.add(NamespaceMap.get(6));
		ReplicationMap.put(1, rep);
		rep.set(0, NamespaceMap.get(6));
		rep.set(1, NamespaceMap.get(1));
		ReplicationMap.put(2, rep);
		rep.set(0, NamespaceMap.get(1));
		rep.set(1, NamespaceMap.get(2));
		ReplicationMap.put(3, rep);
		rep.set(0, NamespaceMap.get(2));
		rep.set(1, NamespaceMap.get(3));
		ReplicationMap.put(4, rep);
		rep.set(0, NamespaceMap.get(3));
		rep.set(1, NamespaceMap.get(4));
		ReplicationMap.put(5, rep);
		rep.set(0, NamespaceMap.get(4));
		rep.set(1, NamespaceMap.get(5));
		ReplicationMap.put(6, rep);
	}

	/**
	 * This function runs the crawler to get information based on keyword.
	 * 
	 * @param input
	 *            the keyword
	 * @return the output filename
	 * @throws IOException
	 */
	public static String runCrawler(String input) throws IOException {
		String filename = crawler.crawl(input);
		return filename;
	}

}

/**
 * 
 * This is the Crawler Class which gets data for chunks.
 *
 */


class Crawler {
	public String crawl(String input) throws IOException {

		int status = 0;
		int flag = 1;
		StringBuffer s = new StringBuffer("https://www." + input + ".com");
		Queue<StringBuffer> q = new LinkedList<StringBuffer>();
		q.add(s);
		String filename = input + "complete.txt";
		File file = new File(filename);
		if (!file.exists())
			file.createNewFile();
		FileWriter fout = new FileWriter(file);
		Set<StringBuffer> set = new HashSet<StringBuffer>();
		set.add(s);
		StringBuffer v = new StringBuffer("");
		int ctr = 0;
		while (ctr < 4800) {
			status = ctr % 48;
			if (status == flag) {
				System.out.println("|" + flag + "%|");
				flag++;
			}
			UUID n = UUID.randomUUID();
			q.remove();
			q.add(new StringBuffer(n.toString()));
			fout.append(q.peek());
			fout.append(System.getProperty("line.separator"));
			ctr++;
		}
		System.out.println(filename + " File Genereated");
		fout.close();
		return filename;
	}
}

