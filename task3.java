import java.util.List;
import java.util.ArrayList;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.io.File;

class Server {
	
	public static final int FAILURE = 0;
	public static final int SUCCESS = 1;
	
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
		 * Closes this Handler and all its connections
		 */
		private void stop() throws Exception {
			if(this.running) {
				inFromClient.close();
				outToClient.close();
				mySocket.close();
			}
			this.running = false;
		}
		
		/**
		 * Runs a loop that deals with communication to the client, handling
		 * commands one by one
		 */
		public void run() {	
			try {
			
				while(running) {
					
					//Get the next command
					int nextCommand =
						Integer.parseInt(this.inFromClient.readLine());
					
					//Cascaded if/else statements to handle the request
					if(nextCommand == Client.SEND_PASSWORD) this.sendPassword();
					else if(nextCommand == Client.CLIENT_EXIT) this.clientExit();
					else if(nextCommand == Client.SERVER_EXIT) this.serverExit();
					else if(nextCommand == Client.LIST_DIRECTORY) this.listDirectory();
					else if(nextCommand == Client.SEND_FILE) this.sendFile();
					else if(nextCommand == Client.RECIEVE_FILE) this.recieveFile();
					else /*Nothing to do*/;

				}
				
			} catch(Exception e) {}
		}
		
		private void sendPassword() throws Exception {
			
			//Get the sent password
			String pw = inFromClient.readLine();
			
			//If the passwords match:
			if(pw.equals(this.password)) {
				
				//set the authenticated field to true
				this.authenticated = true;
				
				//Send a successful response code
				outToClient.writeBytes(Server.SUCCESS + "\n");
			}
			else {
				//Send a failed response code
				outToClient.writeBytes(Server.FAILURE + "\n");
			}
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
			if(this.authenticated) {
				//Send a success response code
				outToClient.writeBytes(Server.SUCCESS + "\n");
				
				//Stop this connection
				this.stop();
				
				//Stop the server
				Server.this.running = false;
			}
			else {
				//Send a failed response code
				outToClient.writeBytes(Server.FAILURE + "\n");
			}
		}
		
		/**
		 * Sends a directory listing to the client
		 */
		private void listDirectory() throws Exception {
			if(this.authenticated) {
				
				//Get the files in this directory
				File[] files = new File(".").listFiles();
				
				//Create a string with the directory listing
				String listing = "";
				for(File file : files) listing += file.getName() + " ";
				
				//Send that listing to the client
				outToClient.writeBytes(listing + "\n");
			}
			else {
				//Send a failed response code
				outToClient.writeBytes(Server.FAILURE + "\n");
			}
		}
		
		/**
		 * Recieves a file from the client
		 * Named according to the client, hence the confusing name
		 */
		private void sendFile() throws Exception {
			
		}
		
		/**
		 * Sends a file to the client
		 * Named according to the client, hence the confusing name
		 */
		private void recieveFile() throws Exception {
						
			//Get the file name from the client
			String fileName = inFromClient.readLine();
			
			//Stores the file if it is found in this directory
			File requestedFile;
			
			//Have we found the requested file?
			boolean foundFile = false;
			
			//Get the files in this directory
			File[] files = new File(".").listFiles();
			
			//Look for the requested file in this directory
			for(File file : files) {
				if(file.getName().equals(fileName) && !file.isDirectory()) {
					foundFile = true;
					requestedFile = file;
				}
			}
			
			if(this.authenticated && foundFile) {
				//Tell the client we found the file and can send it
				outToClient.writeBytes(Server.SUCCESS + "\n");
				
				BufferedReader br = 
					new BufferedReader(new FileReader(requestedFile));
				
				ArrayList<String> fileLines = new ArrayList<String>();
				
				String nextLine = br.readLine();
				
				while(nextLine != null) {
					fileLines.add(nextLine);
					nextLine = br.readLine();
				}
				
				//Tell the client howmany lines we're sending
				outToClient.writeBytes(fileLines.size() + "\n");
				
				for(String line : fileLines) {
					outToClient.writeBytes(line + "\n");
				}
			}
			else {
				//Send a fail message because the file wasn't found
				//or the client hasn't authenticated
				outToClient.writeBytes(Server.FAILURE + "\n");
			}
		}
	}
}

class Client {
	
	//Possible commands the client can send to the server
	public static final int SEND_PASSWORD = 0;
	public static final int CLIENT_EXIT = 1;
	public static final int SERVER_EXIT = 2;
	public static final int LIST_DIRECTORY = 3;
	public static final int SEND_FILE = 4;
	public static final int RECIEVE_FILE = 5;
	
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
	
	public void exit() throws Exception {
		this.inFromServer.close();
		this.outToServer.close();
		this.mySocket.close();
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
			
			//Initialise the input stream
			this.inFromServer = new BufferedReader(
				new InputStreamReader(mySocket.getInputStream()));
			
			//Initialise the output stream
			this.outToServer = new DataOutputStream(mySocket.getOutputStream());
			
			this.connected = true;
			
		} catch(Exception e) {
			//If an exception is thrown, this.connected never gets set to true
		}
		
		//Return an OK if the connection was successful,
		//otherwise return a CannotConnect 
		return this.connected ? new OK() : new CannotConnect();
	}
	
	/**
	 * Returns OK if password passing is successful, else
	 * AuthenticationFailed.  There is no need to do anything
	 * sophisticated like refusing further connection attempts or
	 * sent passwords for 10 seconds attempts after three
	 * failures.
	 * @param pw The password to authenticate with
	 * @return an OK if the authentication was successful, AuthenticationFailed
	 * otherwise.
	 */
	public Response sendPassword(String pw) throws Exception {
		
		//Tell the server we're sending the password
		outToServer.writeBytes(Client.SEND_PASSWORD + "\n");
		
		//Send the password to the server
		outToServer.writeBytes(pw + "\n");
		
		//Read the server's response
		int response = Integer.parseInt(inFromServer.readLine());
		
		if(response == 1) {
			//If response = 1, authentication was successful
			return new OK();
		}
		else {
			//If response = 0, or something else went wrong, 
			//return an AuthenticationFailed
			return new AuthenticationFailed();
		}
	}
	
	/**
	 * Sends a message to the server indicating the wish to
	 * terminate the connection, and exits the client
	 * unconditionally(i.e. works even if the client has not
	 * successfully been authenticated by the server). The server
	 * does not acknowledge the disconnection message.  The server
	 * does NOT terminate.
	 */
	public void clientExit() throws Exception {

		//Simply tell the server we're terminating the connection
		outToServer.writeBytes(Client.CLIENT_EXIT + "\n");
		
		//Stop the client
		this.exit();
	} 
	
	// All methods below require that the client has been password
	// authenticated before. They will always fail with
	// AuthenticationFailed if they had not been already password
	// authenticated. We will not mention the requirement for
	// authentication below, but it is in place.
	
	/**
	 * Sends a message to the server indicating the wish to
	 * terminate the connection and also the server.  The server
	 * has to acknowledge the termination using OK, and the
	 * request will fail if the client has not previously been
	 * authenticated.  If the server acknowledges the request, it
	 * will terminate.  When the client receives the
	 * acknowledgment, it will also terminate.
	 * @return an OK object if the server accepts the termination request, or
	 * a TerminationRequestDenied object if not.
	 */
	public Response serverExit() throws Exception {
		
		//Tell the server we're to terminate
		outToServer.writeBytes(Client.SERVER_EXIT + "\n");
		
		//Read the server's response
		int response = Integer.parseInt(inFromServer.readLine());
		
		if(response == Server.SUCCESS) {
			//Stop the client
			this.exit();
			
			//Return an OK
			return new OK();
		}
		else {
			//Return a TerminationRequestDenied
			return new TerminationRequestDenied();
		}
	} 
	
	/**
	 * Returns an instance of DirectoryListing with the local
	 * directory stored in dir_. If the directory cannot be sent,
	 * then DirectoryProblem is returned.
	 * @return a DirectoryListing if the request was successful, otherwise a
	 * DirectoryProblem
	 */
	public Response listDirectory() throws Exception {
		//Tell the server we want a directory listing
		outToServer.writeBytes(Client.SERVER_EXIT + "\n");
		
		//Read the server's response
		String line = inFromServer.readLine();
		
		//If the server put a "0" on the buffer, that corresponds to an error
		//(most likely a authentication issue)
		if(line.equals("0") || line.equals("0\n")) {
			return new DirectoryProblem();
		}
		//Otherwise return the string as it will be the directory listing
		else {
			return new DirectoryListing(line);
		}
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
		outToServer.writeBytes(Client.RECIEVE_FILE + "\n");
		
		//Send the file name to the server
		outToServer.writeBytes(fileName + "\n");
		
		//Read the response from the server i.e. if we read a 1, expect a file,
		//if we read a 0 expect nothing and return a error
		int status = Integer.parseInt(inFromServer.readLine());
		
		if(status == Server.SUCCESS) {
			//Get the number of lines for the file from the server
			int linesInFile = Integer.parseInt(inFromServer.readLine());
			
			//Read the file from the buffer
			String file = "";
			for(int i = 0; i < linesInFile; i++) {
				file += inFromServer.readLine() + "\n";
			}
			
			//Return the file in a FileContent object
			return new FileContent(file);
		}
		else{
			return new CannotRecieveFile();
		}
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