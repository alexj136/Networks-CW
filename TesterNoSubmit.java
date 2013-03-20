public class TesterNoSubmit {
	
	public static void main(String[] args) throws Exception {
		//TesterNoSubmit.testDNSLookup();
		//TesterNoSubmit.testPage();
		TestServer();
	}
	
	public static void testDNSLookup() throws Exception {
		String fqdn = "www.google.co.uk";
		System.out.println("Looking up " + fqdn);
		System.out.println("Lookup returned " + (new DNSLookup().lookup(fqdn)));
		
		String ip = "212.58.244.69";
		System.out.println("Looking up " + ip);
		System.out.println("Lookup returned " + (new DNSLookup().reverseLookup(ip)));
	}
	
	public static void testPage() throws Exception {
		String page1 = "http://www.sussex.ac.uk:80";
		System.out.println("Requesting status of " + page1);
		System.out.println(new Page().check(page1).msg());
		
		String page2 = "http://www.sussex.ac.uk/fakepage:80";
		System.out.println("Requesting status of " + page2);
		System.out.println(new Page().check(page2).msg());
		
		String page3 = "http://stackoverflow.com/questions/355167/how-are-anonymous-inner-classes-used-in-java:80";
		System.out.println("Requesting status of " + page3);
		System.out.println(new Page().check(page3).msg());
	}
	
	public static void TestServer() throws Exception {
		Server server = new Server();
		server.run(2345, "hello", new java.util.ArrayList<String>());
		System.out.println("Success");
	}
}