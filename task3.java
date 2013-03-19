import java.util.List;
import java.util.ArrayList;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.DataOutputStream;

class Server {
	
	//Is the server running?
	private boolean running = true;
	
	//We need to keep a reference to all the handlers as we need to force them
	//all to close if any authenticated client initiates a serverExit()
	ArrayList<Handler> handlers = new ArrayList<Handler>();
	
	/**
	 * The "port" variable holds the port number at which the
	 * server will run. The variable "forbidden" implements basic
	 * client-address based access control (firewall). It contains
	 * a list (possibly empty) of IP addresses and/or
	 * FQDNs. Servers connecting from an IP address that is in
	 * this list, or if there is a FQDN in "forbidden" that resolves
	 * to that IP address, have their connection request refused
	 * and an appropriate message is returned to the client (see
	 * below). If the list contains strings that are neither valid
	 * IP addresses or valid FQDNs, these are simply ignored.
	 *
	 * Once the client has connected, the client must supply a
	 * password (so connecting is different from the password
	 * exchange).  If the client successfully returns the correct
	 * password, it gains full access to all the servers
	 * functionality. Otherwise the server sends a suitable error
	 * message back to the client. If the client requests server
	 * functionality without being successfully authenticated, the
	 * server returns suitable error messages (see below).
	 */
	public void run(int port, String password, List<String> forbidden) throws Exception {
		
		//The ServerSocket that will accept incoming connections
		ServerSocket welcomeSocket = new ServerSocket(port);
		
		while(this.running) {
			
			//Accept a new connection, print the IP address of the connecting client
			Socket clientSocket = welcomeSocket.accept();
			String clientIP = clientSocket.getInetAddress().toString();
			
			//Reject the connector if their IP (or a corresponding
			//domain name) is blacklisted
			if(forbidden.contains(clientIP) ||
				forbidden.contains(Server.reverseLookup(clientIP))) { 
				
				clientSocket.close();
			}
			
			//Else we'll allow them to connect
			else {
				
				//Start a new thread for the connector
				Handler h = new Handler(clientSocket, password);
				handlers.add(h);
				h.run();
			}
			
			//Close the ServerSocket
			welcomeSocket.close();
			
			//Terminate all the handlers
			for(Handler h : handlers) {
				h.stop();
			}
		}
	}
	
	/**
	 * Reverse looks up a fully qualified domain name from an IP address
	 * @param ip the IP address to look up
	 * @return a corresponding domain name
	 */
	public static String reverseLookup ( String ip ) throws Exception {
		String fqdn = java.net.InetAddress.getByName(ip).getCanonicalHostName();
		if(fqdn.equals(ip)) throw new Exception("Unable to resolve hostname");
		return fqdn;
	}
	
	class Handler implements Runnable {
		
		//The socket this handler runs on
		private Socket mySocket;
		
		//Are we still accepting commands from the client?
		private boolean running;
		
		//Has the client authenticated?
		private boolean authenticated;
		
		//Password
		private String password;
		
		//Input & output
		private BufferedReader inFromClient;
		private DataOutputStream outToClient;
		
		/**
		 * Sets up the handler: opens data streams etc
		 * @param mySocket the socket this handler operates on
		 * @param password the password the client needs to send in order to
		 * authenticate
		 */
		public Handler(Socket mySocket, String password) throws Exception {
			this.password = password;
			this.mySocket = mySocket;
			this.running = true;
			this.authenticated = false;
			inFromClient = new BufferedReader(
				new InputStreamReader(mySocket.getInputStream()));
			outToClient = new DataOutputStream(mySocket.getOutputStream());
		}
		
		/**
		 * Reads the next command from the client from the input stream
		 * @return the integer value representing the command the client has
		 * requested the server perform. Valid commands are -1 < value < 5. if
		 * an invalid value is found, -1 is returned.
		 */
		private int readCommand() {
			//Store the command when we read it
			int value;
			
			//Read one character from the input stream and convert it to an
			//integer
			try {
				char[] c = new char[1];
				inFromClient.read(c, 0, 1);
				value = Character.getNumericValue(c[0]);
			} catch(Exception e) {
				value = -1;
			}
			
			//Return the integer, or -1 if it's outside the valid range
			return (value > -1 && value < 5) ? value : -1;
		}
		
		/**
		 * Closes this Handler and all its connections
		 */
		private void stop() throws Exception {
			this.running = false;
			inFromClient.close();
			outToClient.close();
			mySocket.close();
		}
		
		/**
		 * Runs a loop that deals with communication to the client, handling
		 * commands one by one
		 */
		public void run() {
			
			while(running) {
				
				//Get the next command
				int nextCommand = this.readCommand();
				
				//Cascaded if/else statements to handle the request
				if(nextCommand == Client.CLIENT_EXIT) {
					
				}
				else if(nextCommand == Client.SERVER_EXIT) {
					
				}
				else if(nextCommand == Client.LIST_DIRECTORY) {
					
				}
				else if(nextCommand == Client.SEND_FILE) {
					
				}
				else if(nextCommand == Client.RECIEVE_FILE) {
					
				}
				else {
					
				}
			}
			
		}
		
		private void sendPassword() throws Exception {
			String pw = "";
			
			//Get the password
			
			if(pw.equals(this.password)) this.authenticated = true;
		}
		
		/**
		 * Terminates the connection to this client
		 */
		private void clientExit() throws Exception {
			this.stop();
		}
		
		/**
		 * Terminates the connection to all clients and stops the server
		 */
		private void serverExit() throws Exception {
			Server.this.running = false;
			this.stop();
		}
		
		/**
		 * Sends a directory listing to the client
		 */
		private void listDirectory() {
			
		}
		
		/**
		 * Recieves a file from the client
		 * Named according to the client, hence the confusing name
		 */
		private void sendFile() {
			
		}
		
		/**
		 * Sends a file to the client
		 * Named according to the client, hence the confusing name
		 */
		private void recieveFile() {
			
		}
	}
}

class Client {
	
	//Possible commands the client can send to the server
	public static final int CLIENT_EXIT = 0;
	public static final int SERVER_EXIT = 1;
	public static final int LIST_DIRECTORY = 2;
	public static final int SEND_FILE = 3;
	public static final int RECIEVE_FILE = 4;
	
	//Address of the server
	private String serverAddress;
	
	//Port on which to connect to the server(must match the server's)
	private int serverPort;
	
	//Have we established a connection?
	private boolean connected;
	
	//Socket to the server
	private Socket mySocket;
	
	//Input & output
	private BufferedReader inFromServer;
	private DataOutputStream outToServer;
	
	public Client(String serverAddress, int serverPort) throws Exception {
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.connected = false;
	}
	
	/**
	 * returns OK if connection succeeds,
	 * If the client is from an IP address that is blocked by the server, 
	 * ClientBlocked is returned. If the connection attempt is
	 * refused for other reasons, CannotConnect is returned.
	 * @return an OK object if the connection was successful, or a
	 * CannotConnect object if not.
	 */
	public Response connect() throws Exception {
		
		try {
			//Open the socket
			this.mySocket = new Socket(this.serverAddress, this.serverPort);
			
			//Initialise the input & output streams
			this.inFromServer = new BufferedReader(
				new InputStreamReader(mySocket.getInputStream()));
			this.outToServer = new DataOutputStream(mySocket.getOutputStream());
			
			this.connected = true;
			
		} catch(Exception e) {
			//If an exception is thrown, this.connected never gets set to true
		}
		
		//Return an OK if the connection was successful,
		//otherwise return a CannotConnect 
		return this.connected ? new OK() : new CannotConnect();
	}
	
	public Response sendPassword(String pw) throws Exception {
		// Returns OK if password passing is successful, else
		// AuthenticationFailed.  There is no need to do anything
		// sophisticated like refusing further connection attempts or
		// sent passwords for 10 seconds attempts after three
		// failures.
		throw new Exception("not implemented yet");
	}

	public void clientExit() throws Exception {
		// Sends a message to the server indicating the wish to
		// terminate the connection, and exits the client
		// unconditionally(i.e. works even if the client has not
		// successfully been authenticated by the server). The server
		// does not acknowledge the disconnection message.  The server
		// does NOT terminate.
		throw new Exception("not implemented yet");
	} 
	
	// All methods below require that the client has been password
	// authenticated before. They will always fail with
	// AuthenticationFailed if they had not been already password
	// authenticated. We will not mention the requirement for
	// authentication below, but it is in place.
	
	public Response serverExit() throws Exception {
		// Sends a message to the server indicating the wish to
		// terminate the connection and also the server.  The server
		// has to acknowledge the termination using OK, and the
		// request will fail if the client has not previously been
		// authenticated.  If the server acknowledge the request, it
		// will terminate.  When the client receives the
		// acknowledgment, it will also terminate.
		throw new Exception("not implemented yet");
	} 
	
	public Response listDirectory() throws Exception {
		// Returns an instance of DirectoryListing with the local
		// directory stored in dir_. If the directory cannot be sent,
		// then DirectoryProblem is returned.
		throw new Exception("not implemented yet");
	}
	
	public Response sendFile(String fileName, String fileContent) throws Exception {
		// Takes a file, here represented by fileContent, sends it to
		// the server, which tries to store it in it's local directory
		// under the name fileName. If this is not possible,
		// e.g. because a file with that name already exists, it
		// returns CannotSendFile to the client, otherwise it returns
		// OK.
		throw new Exception("not implemented yet");
	}
	
	/**
	 * If the server has a file with the name given in fileName in
	 * its local directory, it returns the file content using the
	 * FileContent class, with the data being stored in public
	 * member fileData_. Otherwise it returns a suitable error
	 * message using the CannotRecieveFile class.
	 * @param fileName the file to receive from the server
	 * @return A FileContent object if the file was received successfully,
	 * or a CannotRecieveFile object with an appropriate error message if not
	 */
	public Response receiveFile(String fileName) throws Exception {
		
		//Send the appropriate command number to the server
		//outToServer.writeBytes(Client.RECIEVE_FILE.toString());
		
		//Read the length of the file (length is in lines)
		//int lines = ..... can use readCommand code from Handler class
		
		throw new Exception("not implemented yet");
	}
}


interface Response {}
// This is the interface for the various responses that the server
// returns to the client. See below for detailed description.

class OK implements Response {}
class CannotConnect implements Response {}
class ClientBlocked implements Response {}
class AuthenticationFailed implements Response {}
class DirectoryListing implements Response {
	public String dir_;
	public DirectoryListing (String dir ) { dir_ = dir; } }
class DirectoryProblem implements Response {}
class TerminationRequestDenied implements Response {}
class CannotSendFile implements Response {}
class CannotRecieveFile implements Response {}
class FileContent implements Response {
	public String fileData_;
	public FileContent (String fileData ) { fileData_ = fileData; } }