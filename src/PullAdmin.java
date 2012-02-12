import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InterruptedIOException;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.ConcurrentModificationException;

public class PullAdmin {
	
	private String ID;
	private Socket adminSocket;
	private BufferedReader in;
	private PrintWriter out;
		
	public PullAdmin(String ID, Socket adminSocket, BufferedReader in, PrintWriter out) {
		this.ID = ID;
		this.adminSocket = adminSocket;
		this.in = in;
		this.out = out;
	}
	
	public String getQuestion(int questionID) {
		out.println("GetQuestion;" + questionID);
		String response = "";
		while (response.equals("")) {
			try {
				response = in.readLine();
			} catch (IOException e) {}
		}
		return response;
	}
	
	public String handleResponse(int questionID, String response) {
		out.println("HandleResponse;" + questionID + ";" + response);
		String adminResponse = "";
		while (adminResponse.equals("")) {
			try {
				adminResponse = in.readLine();
			} catch (IOException e) {}
		}
		return adminResponse;
	}
}
