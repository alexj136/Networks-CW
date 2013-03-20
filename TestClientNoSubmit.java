public class TestClientNoSubmit {
	public static void main(String[] args) throws Exception {
		Client client = new Client("localhost", 2345);
		client.connect();
		client.sendPassword("hello");
		client.recieveFile("testfile.txt");
	}
}