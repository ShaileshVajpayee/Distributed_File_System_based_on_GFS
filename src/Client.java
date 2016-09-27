
/**
 * 							A Distributed File System based on GFS
 * 
 * This is the Client Class which is run on the Client machine.
 * 
 * @author Shailesh Vajpayee Email: srv6224@rit.edu
 * @author Aishwary Pramanik   Email: ap9599@rit.edu
 *
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {

	static Socket monitor_socket;
	static Socket master_socket;
	static Socket chunk_socket;
	static DataInputStream monitor_in;
	static DataOutputStream monitor_out;
	static DataInputStream master_in;
	static DataOutputStream master_out;
	static DataInputStream chunk_in;
	static DataOutputStream chunk_out;
	static String monitor_master_adr;
	static boolean monitor_master_adr_flag = false;
	static String master_chunk_adr;
	static boolean master_chunk_adr_flag = false;
	static String msg_to_chunk;
	static String chunk_reply;
	static boolean chunk_reply_flag = false;

	/**
	 * This function is responsible for receiving monitor message.
	 * 
	 * @param in
	 *            The input stream of Monitor
	 */
	public void receive_monitor_msg(DataInputStream in) {
		Monitor_input input = new Monitor_input(in);
		Thread thread = new Thread(input);
		thread.start();
	}

	/**
	 * This function is responsible for receiving Master message.
	 * 
	 * @param in
	 *            The input stream of Master.
	 */
	public void receive_master_msg(DataInputStream in) {
		Master_input input = new Master_input(in);
		Thread thread = new Thread(input);
		thread.start();
	}

	/**
	 * This function is responsible for receiving Chunk message
	 * 
	 * @param in
	 *            The input stream of Chunk.
	 */
	public void receive_chunk_msg(DataInputStream in) {
		Chunk_input input = new Chunk_input(in);
		Thread thread = new Thread(input);
		thread.start();
	}

	/**
	 * This function controls client and monitor communication.
	 * @throws Exception
	 */
	public void monitor_comm() throws Exception {
		System.out.println("Connected to Monitor");
		monitor_in = new DataInputStream(monitor_socket.getInputStream());
		monitor_out = new DataOutputStream(monitor_socket.getOutputStream());
		receive_monitor_msg(monitor_in);
		monitor_out.writeUTF("Request Connection to Master");
		// monitor_out.close();
		String[] master_address = new String[2];
		if (monitor_master_adr_flag == true)
			master_address = monitor_master_adr.split(" ");
		else {
			while (monitor_master_adr_flag == false) {
				Thread.sleep(10);
			}
			master_address = monitor_master_adr.split(" ");
		}

		System.out.println(master_address[0] + " " + master_address[1]);
		master_comm(master_address[0], Integer.parseInt(master_address[1]));
		// master_comm(master_address[0], 7777);
		monitor_in.close();
		monitor_socket.close();
	}

	/**
	 * This function controls client and master communication.
	 * @param IP	The IP of the Master.
	 * @param Port	The Port number of the Master.
	 * @throws Exception
	 */
	public void master_comm(String IP, int Port) throws Exception {
		System.out.println("Connecting to Master... " + IP + " " + Port);
		master_socket = new Socket(IP, Port);
		master_in = new DataInputStream(master_socket.getInputStream());
		master_out = new DataOutputStream(master_socket.getOutputStream());
		System.out.println("Connection established to Master: " + IP + " " + Port);
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter website Keyword and Rank (eg. google 20):");
		String url = sc.nextLine();
		msg_to_chunk = url;
		receive_master_msg(master_in);
		master_out.writeUTF(url);
		String[] chunk_address = new String[3];
		if (master_chunk_adr_flag == true)
			chunk_address = master_chunk_adr.split(" ");
		else {
			System.out.println("above while");
			while (master_chunk_adr_flag == false) {
				Thread.sleep(10);
			}
			System.out.println("in master comm");
			chunk_address = master_chunk_adr.split(" ");

		}
		System.out.println("Chunk address " + chunk_address[0] + " " + chunk_address[1]);
		System.out.println("Redirecting to appropriate chunk..\n");
		chunk_comm(chunk_address[0], Integer.parseInt(chunk_address[1]), url + " " + chunk_address[2]);
		master_in.close();
		sc.close();
	}

	/**
	 * This function is responsible for client and chunk communication.
	 * @param IP	The IP of the chunk.
	 * @param Port	The Port number of the chunk.
	 * @param get	The keyword and the rank requested by the client.
	 * @throws Exception
	 */
	public void chunk_comm(String IP, int Port, String get) throws Exception {
		chunk_socket = new Socket(IP, Port);
		chunk_in = new DataInputStream(chunk_socket.getInputStream());
		chunk_out = new DataOutputStream(chunk_socket.getOutputStream());

		System.out.println("Connection established with chunk server " + IP + " " + Port);
		chunk_out.writeUTF(get);
		receive_chunk_msg(chunk_in);

		if (chunk_reply_flag == true)
			System.out.println(chunk_reply);
		else {
			System.out.println("above while");
			while (chunk_reply_flag == false) {
				Thread.sleep(10);
			}
			System.out.println("Received reply from Chunk");
			System.out.println("The website of ur preferred rank is " + chunk_reply);
		}

		chunk_in.close();
		chunk_out.close();
		chunk_socket.close();
		master_out.writeUTF("Done");
		master_out.close();
		master_socket.close();
		System.out.println("Exiting...");
	}

	/**
	 * The main function of this class.
	 * @param args
	 * 				The IP and port of the monitor.
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Client client = new Client();
		System.out.println("Connecting...");
		if (args.length != 2) {
			System.out.println("Invalid number of arguments: ");
			System.out.println("java Client <IPaddress> <Port>");
			System.out.println("eg: java Reader 127.0.0.1 7777");
			System.exit(0);
		}
		monitor_socket = new Socket(args[0], Integer.parseInt(args[1]));
		System.out.println("Client initialized...");
		client.monitor_comm();
	}

	/**
	 * This class handles the input from Monitor.
	 */
	class Monitor_input implements Runnable {

		DataInputStream in;

		public Monitor_input(DataInputStream in) {
			this.in = in;
		}

		@Override
		public void run() {

			while (monitor_master_adr_flag == false) {
				try {
					System.out.println("waiting for address of master from monitor");
					monitor_master_adr = in.readUTF();
					System.out.println("Got address of master");
					monitor_master_adr_flag = true;
				} catch (IOException e) {
					e.printStackTrace();
				}
				// System.out.println(monitor_master_adr);
			}
		}
	}

	/**
	 * This class handles the input from Master.
	 *
	 */
	class Master_input implements Runnable {

		DataInputStream in;

		public Master_input(DataInputStream in) {
			this.in = in;
		}

		@Override
		public void run() {

			System.out.println("in master thread");
			while (master_chunk_adr_flag == false) {
				try {
					System.out.println("waiting for address of chunk from master");
					master_chunk_adr = in.readUTF();
					System.out.println("Got address of chunk " + master_chunk_adr);
					master_chunk_adr_flag = true;
					System.out.println(master_chunk_adr_flag);
				} catch (IOException e) {
					e.printStackTrace();
				}
				// System.out.println(monitor_master_adr);
			}
		}
	}

	/**
	 * This class handles the input from Chunk.
	 */
	class Chunk_input implements Runnable {

		DataInputStream in;

		public Chunk_input(DataInputStream in) {
			this.in = in;
		}

		@Override
		public void run() {

			System.out.println("in chunk thread");
			while (chunk_reply_flag == false) {
				try {
					System.out.println("waiting for reply from chunk server");
					chunk_reply = in.readUTF();
					System.out.println("Got reply from chunk " + chunk_reply);
					chunk_reply_flag = true;
					System.out.println(chunk_reply_flag);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
