//Candidate No: 18512

import java.net.HttpURLConnection;
import java.net.URL;

interface Result {
	
    public boolean ok ();
    public String msg ();
    
}

class Page {
	
    public Result check ( String fullURL ) throws Exception {
    	
    	HttpURLConnection conn = (HttpURLConnection) new URL(fullURL).openConnection();
    	
    	final int code = conn.getResponseCode();
		final String message = conn.getResponseMessage();
		
		return new Result() {
			public boolean ok() {
				return code == 200;
			}
			public String msg() {
				return message;
			}
		};
	}
}