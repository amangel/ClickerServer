import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.util.Timer;
import java.util.TimerTask;

public class Client {
	
	private static final int heartbeatTime = 32;

	private String ID;
	private String macAddress;
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	private String currentAnswer;
	private PushAdmin admin;
	//private ResponseHandler responseHandler;
	private CommandHandler commandHandler;
	private String group;
	private Timer timer;
	
	public Client(String ID, String macAddress, Socket socket, BufferedReader in, PrintWriter out, PushAdmin admin) {
		this.ID = ID;
		this.macAddress = macAddress;
		this.socket = socket;
		this.in = in;
		this.out = out;
		this.admin = admin;
		//this.responseHandler = null;
		this.currentAnswer = " ";
		this.group = "";
		this.commandHandler = new CommandHandler();
		this.timer = new Timer();
		new Thread(this.commandHandler).start();
		timer.schedule(new PauseTask(), heartbeatTime * 1000);
		
	}
	
	public String getID() {
		return ID;
	}
	
	public String getMacAddress() {
		return macAddress;
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public BufferedReader getReader() {
		return in;
	}
	
	public PrintWriter getWriter() {
		return out;
	}
	
	public String getAnswer() {
		return currentAnswer;
	}
	
	public String getGroup() {
		return group;
	}
	
	public void clientReconnected(Socket newSocket, BufferedReader newIn, PrintWriter newOut) {
		this.socket = newSocket;
		this.in = newIn;
		this.out = newOut;
		commandHandler = new CommandHandler();
		timer = new Timer();
		new Thread(commandHandler).start();
		timer.schedule(new PauseTask(), heartbeatTime * 1000);
	}
	
	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	
	public void setGroup(String groupName) {
		group = groupName;
		out.println("System`/;GROUP`/;" + groupName);
	}
	
	public void unsetGroup() {
		group = "";
		out.println("System`/;GROUP`/;Ungrouped");
	}
	
	public boolean isPaused() {
		return commandHandler.isPaused();
	}
		
	public void startQuestion(String openMessage) {
		out.println(openMessage);
		/*
		responseHandler = new ResponseHandler(openMessage);
		new Thread(responseHandler).start();
		System.out.println("Response handler created");
		*/
	}
	
	
	
	public void stopQuestion() {
		out.println("Close");
		/*
		if (responseHandler != null) {
			responseHandler.close();
			responseHandler = null;
		}
		*/
	}
	
	/*
	public boolean onQuestion() {
		if (responseHandler != null) {
			if (responseHandler.isOpen()) {
				return true;
			}
		}
		return false;
	}
	*/
	
	
	private class PauseTask extends TimerTask {
		public void run() {
			commandHandler.pause();
			System.out.println("No heartbeat from client " + ID + ", pausing now");
		}
	}
	
	
	
	
	private class CommandHandler implements Runnable{
		
		private boolean paused = false;
		    
		public void run() {
				while (!paused) {
					try {
						String command = in.readLine();
						if (command == null) {
							pause();
							//start a paused timer and when it expires call client died?
							System.out.println("Client " + ID + " is paused");
						}
						else {
							if (command.equals("AreYouStillThere")) {
								timer.cancel();
								timer = new Timer();
								timer.schedule(new PauseTask(), heartbeatTime * 1000);
								out.println("YesImHere");
								out.flush();
								System.out.println("Received heartbeat from " + ID + " and reset timer");
							} else {
								admin.updateAnswer(ID + "`/;" + command);
							}
						}
					} catch (SocketTimeoutException e) {
					} catch (IOException e) {
						pause();
						System.out.println(e.getMessage());
						System.out.println("Client " + ID + " is paused");
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {}
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
		}
		
		public void unpause() {
			paused = false;
			System.out.println("Client " + ID + " just got unpaused");
		}
		
		public void pause() {
			paused = true;
			timer.cancel();
			System.out.println("Just paused client " + ID);
		}
		
		public boolean isPaused() {
			return paused;
		}
	}
	
    /*
	private class ResponseHandler implements Runnable{
    	
    	private String openMessage;
    	private boolean open;
    	
    	public ResponseHandler(String openMessage) {
    		this.openMessage = openMessage;
    		this.open = true;
    	}
    	
    	public void run() {
    		out.println(openMessage);
    		while (open) {
    			try {
    				String response = in.readLine();
    				if (response != null) {
					   System.out.println("Got: " + response);
					   admin.updateAnswer(ID + "`/;" + response);
    				}
    				else {
    					open = false;
    					admin.clientDied(ID);
    				}
    			} catch (InterruptedIOException e) {
    			} catch (IOException e) {
    				open = false;
    			}
    		}
    		out.println("Close");
    		System.out.println("Sent close to client");
//    		while (true) {
//    			try {
//    				System.out.println("Calling last readline");
//    				String response = in.readLine();
//    				if (response != null) {
//    					admin.handleResponse(ID, response);
//    				}
//    				else {
//    					admin.clientDied(ID);
//    				}
//    				break;
//    			} catch (InterruptedIOException e) {
//    			} catch (IOException e) {
//    				break;
//    			}
//    		}
    		System.out.println("Out of last while loop");
    	}
    	
    	public void close() {
    		open = false;
    	}
    	
    	public boolean isOpen() {
    		return open;
    	}
    }
    */
}
