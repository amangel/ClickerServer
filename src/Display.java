import java.io.BufferedReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.Socket;


public class Display {
	
	private int ID;
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	private String iConsume;
	private PushAdmin admin;
	
	public Display(int id, Socket socket, BufferedReader in, PrintWriter out, PushAdmin admin) {
		this.ID = id;
		this.socket = socket;
		this.in = in;
		this.out = out;
		this.admin = admin;
		boolean ctRead = false;
		while (!ctRead) {
			try {
				String consumeInfo = in.readLine();
				ctRead = true;
				String[] consumeParts = consumeInfo.split("`/;");
				if (consumeParts[0].equals("IConsume")) {
					this.iConsume = consumeParts[1];
				} else {
					System.out.println("Read in: " + consumeParts[0] + " instead of IConsume");
				}
			} catch (SocketTimeoutException e) {} 
			catch (IOException e) {e.printStackTrace(); break;}
		}
		if (!ctRead) {
			System.out.println("Failed to read consumption info");
		}
	}
	
	public void sendMessage(String msg) {
		out.println(msg);
	}
	
	public String getIConsume() {
		return this.iConsume;
	}
	
	 private class DCChecker implements Runnable{
	    	
	    	private String openMessage;
	    	private boolean open;
	    	
	    	public DCChecker(String openMessage) {
	    		this.openMessage = openMessage;
	    		this.open = true;
	    	}
	    	
	    	public void run() {
	    		while (true) {
	    			try {
	    				String response = in.readLine();
	    				if (response == null) {
	    					admin.displayDied(ID);
	    					break;
	    				} 
	    			} catch (InterruptedIOException e) {
	    			} catch (IOException e) {
	    				admin.displayDied(ID);
	    				break;
	    			}
	    		}
	
	    	}
	 }
	
}
