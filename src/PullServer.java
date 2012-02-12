
   import java.net.ServerSocket;
   import java.net.Socket;
   import java.io.BufferedReader;
   import java.io.FileWriter;
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
   //import java.sql.*;


    public class PullServer {
   
    	/*
   //private AdminListener adminHandler;
      private ConnectionListener connChecker;
   //private Map<String, PullAdmin> admins;
      private ServerSocket serverSocket;
      private PrintWriter log;
   
      private Connection connect;
      private Statement statement;
      private PreparedStatement preparedStatement;
      private ResultSet resultSet;
   
       public PullServer(int clientPort, int adminPort) {
         long time = System.currentTimeMillis();
         try {
            log = new PrintWriter(new FileWriter("logs/pullServer-" + time + ".txt"));
         } 
             catch (IOException e) {
               System.out.println("Could not initialize log");
            }
         try {
            serverSocket = new ServerSocket(clientPort);
            serverSocket.setSoTimeout(100);
         } 
             catch (IOException e) {
               System.out.println("Could not listen on port " + clientPort);
            }
        //admins = Collections.synchronizedMap(new HashMap<String, PullAdmin>(50));
         //adminHandler = new AdminListener(adminPort);	
         //new Thread(adminHandler).start();
         connChecker = new ConnectionListener();
         new Thread(connChecker).start();
      
         try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection("jdbc:mysql://localhost/clicker?" 
                             + "user=clickeruser&password=clickerpassword");		
         } 
             catch (Exception e) {e.printStackTrace();}
         statement = null;
         preparedStatement = null;
         resultSet = null;	
      }
   
       public void log(String msg) {
         log.println("" + System.currentTimeMillis() + "-" + msg);
      }
   	
       public void shutDown() {
         log.close();
      }
   
       private class ConnectionListener implements Runnable {
      
          public void run() {
            System.out.println("Listening for clients now on PullServer");
            while (true) {
               try {
                  Socket client = serverSocket.accept();
                  client.setSoTimeout(100);
                  BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                  PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                  String idString = "";
                  boolean idReceived = false;
                  while (!idReceived) {
                     try {
                        idString = in.readLine();
                        idReceived = true;
                     }
                         catch (InterruptedIOException e) {}
                  }
                  System.out.println(idString);
                  String[] idparts = idString.split(";");
                  if (idparts.length == 4) {
                    // 	if (admins.containsKey(idparts[2])) {
                  //                      	String qString = admins.get(idparts[2]).getQuestion(Integer.parseInt(idparts[3]));
                  //                     		out.println(qString);
                  //                     		log("QuestionRequest;" + idString);
                  //                     		log("SentResponse;" + idString + ";" + qString);
                  //                     	}
                  //                     	else {
                  //                     		out.println("InvalidAdmin");
                  //                     		log("QuestionRequest;InvalidAdmin:" + idString);
                  //                     	}
                     try {
                        statement = connect.createStatement();
                        String query = "SELECT * FROM questions WHERE questionid='" 
                                     + idparts[3] + "' AND admin='" + idparts[2] + "'";
                        resultSet = statement.executeQuery(query);
                        while (resultSet.next()) {
                           String qString = resultSet.getString("questionstring");
                           out.println("Open;" + idparts[3] + ";" + qString);			
                        }
                     } catch (Exception e) {e.printStackTrace();}
                  }
                  else if (idparts.length == 5) {
                     System.out.println("Processing response now");
                     try {
                        statement = connect.createStatement();
                        String query = "SELECT * FROM questions WHERE questionid=(SELECT followup FROM questions WHERE questionid='" + idparts[3] + "' AND admin='" + idparts[2] + "')";
                        resultSet = statement.executeQuery(query);
                        if (resultSet.next()) {
                           String qString = resultSet.getString("questionstring");
                           out.println("Open;" + resultSet.getString("questionid") + ";" + qString);
                        } 
                        else {
                           out.println("Close;");
                        }		
                     } 
                         catch (Exception e) {e.printStackTrace();}
                    // 	if (admins.containsKey(idparts[2])) {
                  //                     		String rString = admins.get(idparts[2]).handleResponse(Integer.parseInt(idparts[3]), idparts[4]);
                  //                     		out.println(rString);
                  //                     		log("AnswerReceived;" + idString);
                  //                     		log("SentResponse;" + idString + ";" + rString);
                  //                     	}
                  //                     	else {
                  //                     		out.println("InvalidAdmin");
                  //                     		log("AnswerReceived;InvalidAdmin:" + idString);
                  //                     	}
                  }
                  else {
                     out.println("InvalidInformation");
                     log("InvalidRequest;" + idString);
                  }
                  client.close();
               } 
                   catch (Exception e) {}
            }
         }
      }
   
   /*	
   private class AdminListener implements Runnable {
   	
   	private ServerSocket connectionSocket;
   	
   	public AdminListener(int port) {
   		try {
   			connectionSocket = new ServerSocket(port);
   		} 
   		catch (IOException e) {
   			System.out.println("Could not listen on port " + port);
   		}
   	}
   	
   	public void run() {
            while (true) {
            	try {
            		System.out.println("Waiting for admin on pull server");
            		Socket adminSocket = connectionSocket.accept();
            		System.out.println("Admin connected, authenticating now");
            		BufferedReader adminIn = new BufferedReader(new InputStreamReader(adminSocket.getInputStream()));
            		PrintWriter adminOut = new PrintWriter(adminSocket.getOutputStream(), true);
            		String userName = adminIn.readLine();
            		String password = adminIn.readLine();
            		if (userName.equals("SEAN") && password.equals("FRED")) {
            			admins.put(userName, new PullAdmin(userName, adminSocket, adminIn, adminOut));	
            		}
            	} catch (IOException e) {}
            }
   	}
   }
   */	
   
   
   }
