public class TestClientNoSubmit {
	public static void main(String[] args) throws Exception {
		Client client = new Client("localhost", 2345);
		client.connect();
		client.sendPassword("hello");
		
		client.sendFile("file.p", "hello there c\nan we be fr\niends please");
		
		client.serverExit();
	}
}