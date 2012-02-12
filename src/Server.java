import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InterruptedIOException;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.ConcurrentModificationException;


abstract class Server {
	
	private ServerSocket serverSocket;

	
	public Server(int port) {
		try {
			serverSocket = new ServerSocket(port);
			serverSocket.setSoTimeout(100);
		} 
		catch (IOException e) {
			System.out.println("Could not listen on port " + port);
		}
	}
	
	public ServerSocket serverSocket() {
		return serverSocket;
	}
	

	
    //abstract void adminConnected(Admin newAdmin);
}
