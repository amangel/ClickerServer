   import java.net.ServerSocket;
   import java.net.Socket;
   import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
   import java.io.InputStreamReader;
   import java.io.PrintWriter;
   import java.io.IOException;
   import java.io.FileReader;
   import java.io.InterruptedIOException;

import java.util.ArrayList;
   import java.util.Collections;
   import java.util.Map;
   import java.util.HashMap;
   import java.util.Iterator;
   import java.util.concurrent.Semaphore;
   import java.util.ConcurrentModificationException;
import java.sql.*;

    public class PushServer {
   
      private ConnectionListener connChecker;
      private AdminListener adminHandler;
      private DisplayListener displayHandler;
      private Map<String, PushAdmin> admins;
      private Map<String, ArrayList<String>> questionSets;
      private Map<String, Question> questions;
      private ServerSocket serverSocket;
   
      private Connection connect;
      private Statement clientStatement;
      private ResultSet clientResultSet;
      private Statement adminStatement;
      private ResultSet adminResultSet;
   
   	
       public PushServer(int clientPort, int adminPort, int displayPort) {
         try {
            serverSocket = new ServerSocket(clientPort);
            serverSocket.setSoTimeout(100);
         } 
             catch (IOException e) {
               System.out.println("Could not listen on port " + clientPort);
            }
         admins = Collections.synchronizedMap(new HashMap<String, PushAdmin>(50));
         adminHandler = new AdminListener(adminPort);
         new Thread(adminHandler).start();
         connChecker = new ConnectionListener();
         new Thread(connChecker).start();
         displayHandler = new DisplayListener(displayPort);
         new Thread(displayHandler).start();
         
         questions = Collections.synchronizedMap(new HashMap<String, Question>());
         questionSets = Collections.synchronizedMap(new HashMap<String, ArrayList<String>>());
      
         //connectToSQLDatabase();
      
         //clientStatement = null;
         //clientResultSet = null;	
         //adminStatement = null;
         //adminResultSet = null;
         
         loadQuestions();
      }
       
       
      private void loadQuestions() {
    	  try {
    		  BufferedReader qIn = new BufferedReader(new FileReader("questions/questions.txt"));
    		  String str;
    		  while ((str = qIn.readLine()) != null)
    			  readQuestionSet(str);
    		 qIn.close();
    	  } catch (IOException e) {
    		  System.out.println("Could not open question file");
    	  }
      }
      
      private void readQuestionSet(String setString) {
    	  System.out.println("reading a question set");
    	  System.out.println(setString);
    	  String[] setParts = setString.split("`/@");
    	  System.out.println(setParts[0]);
    	  ArrayList<String> questionList = new ArrayList<String>();
    	  if (setParts.length > 1) {
    		  if (!setParts[1].equals("")) {
    			  String[] setQuestions = setParts[1].split("`/&");
    			  Question newQuestion = new Question(setQuestions[0]);
    			  String previous = newQuestion.getID();
    			  questions.put(previous, newQuestion);
    			  questionList.add(previous);
    			  if (setQuestions.length > 1) {
    				  for (int i=1; i<setQuestions.length; i++) {
    					  newQuestion = new Question(setQuestions[i]);
    					  String newID = newQuestion.getID();
    					  questions.get(previous).setFollowUp(newID);
    					  questions.put(newID, newQuestion);
    					  questionList.add(newID);
    					  previous = newID;
    				  }
    			  }
    		  }
    	  }
    	  System.out.println("Adding list of size " + questionList.size() + " to set: " + setParts[0]);
    	  questionSets.put(setParts[0], questionList);
      }
      
      public void addQuestionSet(String oldName, String qSetString) {
    	  if (!oldName.equals("")) {
    		  questionSets.remove(oldName);
    	  }
    	  readQuestionSet(qSetString);
    	  saveQuestionSets();
      }
      
      public void deleteQuestionSet(String oldName) {
    	  questionSets.remove(oldName);
    	  saveQuestionSets();
      }
      
      public void saveQuestionSets() {
    	  try {
    		  BufferedWriter fOutput = new BufferedWriter(new FileWriter("questions/questions.txt"));
              Iterator<String> qsIter = questionSets.keySet().iterator();
              while (qsIter.hasNext()) {
            	  String nextQS = qsIter.next();
            	  String qsLine = nextQS + "`/@";
            	  ArrayList<String> setQuestions = questionSets.get(nextQS);
            	  if (setQuestions.size() > 0) {
            		  qsLine += questions.get(setQuestions.get(0)).getQuestionString();
            		  for (int i=1; i<setQuestions.size(); i++) {
            			  qsLine += "`/&" + questions.get(setQuestions.get(i)).getQuestionString();
            		  }
            	  }
            	  fOutput.write(qsLine + "\n");
            	  fOutput.flush();
              }
              fOutput.close();
    	  } catch (IOException e) {System.out.println("Could not save questions");}
      }
       
       /*
       private void connectToSQLDatabase(){
         try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection("jdbc:mysql://localhost/clicker?" 
                             + "autoReconnect=true&user=clickeruser&password=clickerpassword");		
         } 
             catch (Exception e) {
               e.printStackTrace();
            }
      }
      */
   
       /*
       public synchronized ResultSet runQuery(String query) {
         try {
            Statement qStatement = connect.createStatement();
            return qStatement.executeQuery(query);
         } 
             catch (Exception e) {e.printStackTrace();}
         return null;
      }
      */
   
       public PushServer getPushServer() {
         return this;
      }
   
       public String getQuestionSets() {
    	   Iterator<String> qSetIter = questionSets.keySet().iterator();
    	   String[] resultArray = new String[questionSets.size()];
    	   String finalResult = "";
    	   int i=0;
			while (qSetIter.hasNext()) {
				resultArray[i] = qSetIter.next();
				i++;
				//finalResult = finalResult + qSetIter.next() + "`/&";
			}
			java.util.Arrays.sort(resultArray);
			for (String qString: resultArray) {
				finalResult = finalResult + qString + "`/&";
			}
			return "AllSets`/`" + finalResult;
       }
       
       public String getQuestionsInSet(String qSetKey) {
    	   System.out.println("Getting questions for set: " + qSetKey);
    	   ArrayList<String> setQs = questionSets.get(qSetKey);
    	   String finalResult = "";
    	   for (int i=0; i<setQs.size(); i++) {
    		   System.out.println("In a loop");
    		   Question next = questions.get(setQs.get(i));
    		   finalResult = finalResult + next.getID() + "`/;" + next.getFlags() + "`/;" 
          			 + next.getWidgets() + "`/;" + next.getColor() + "`/&";
    	   }
    	   System.out.println("Finalresult: " + finalResult);
    	   return "QuestionSet`/`" + finalResult;
       }
   	
       private class ConnectionListener implements Runnable {
      
          public void run() {
            System.out.println("Listening for clients now on PushServer");
            while (true) {
               try {
                  Socket client = serverSocket.accept();
                  client.setKeepAlive(true);
                  client.setSoTimeout(100);
                  BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                  PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                  String idString = "";
                  boolean idset = false;
                  while (!idset) {
                     try {
                        idString = in.readLine();
                        idset = true;
                     }
                         catch (InterruptedIOException e) {}
                  }
                  String[] idparts = idString.split("`/;");
                  if (idparts.length == 3) {
                     String cid = idparts[0];
                     String cmac = idparts[1];
                     String[] adminParts = idparts[2].split("`/,");
                     String adminID = adminParts[0];
                     String groupID = adminParts[1];
                     System.out.println("Client is wanting to connect to admin: " + adminID);
                     if (admins.containsKey(adminID)) {
                        System.out.println("Calling processClient");
                        admins.get(adminID).processClient(client, cid, cmac, in, out, groupID);
                     } 
                     else {
                        out.println("InvalidAdmin");
                        client.close();
                     }
                  } 
                  else if (idparts.length == 4) {
                	 /*
                     try {
                        clientStatement = connect.createStatement();
                        String query = "SELECT * FROM questions WHERE questionid='" 
                                     + idparts[3] + "' AND admin='" + idparts[2] + "'";
                        clientResultSet = clientStatement.executeQuery(query);
                        while (clientResultSet.next()) {
                           String qString = clientResultSet.getString("questionstring");
                           out.println("Open;" + idparts[3] + ";" + qString);
									System.out.println("Open;" + idparts[3] + ";" + qString);			
                        }	
                     } 
                         catch (Exception e) {e.printStackTrace();}
                     */
                	 Question requested = questions.get(idparts[3]);
                	 out.println("Open`/;" + idparts[3] + "`/;" + requested.getFlags() + "`/;" 
                			 + requested.getWidgets() + "`/;" + requested.getColor());
                  } 
                  else if (idparts.length == 5) {
                	 /*
                     System.out.println("Processing response now");
                     try {
                        clientStatement = connect.createStatement();
								System.out.println("Looking for followups for question " + idparts[3]);
                        String query = "SELECT * FROM questions WHERE questionid=(SELECT followup FROM questions WHERE questionid='" + idparts[3] + "' AND admin='" + idparts[2] + "')";
                        clientResultSet = clientStatement.executeQuery(query);
                        if (clientResultSet.next()) {
									System.out.println("Opening another question");
                           String qString = clientResultSet.getString("questionstring");
                           out.println("Open;" + clientResultSet.getString("questionid") + ";" + qString);
                        } 
                        else {
									System.out.println("No more followups");
                           out.println("Close;");
                        }		
                     }  
                     catch (Exception e) {e.printStackTrace();}	
                     */
                	 Question previous = questions.get(idparts[3]);
                	 if (previous.hasFollowUp()) {
                		 Question next = questions.get(previous.getFollowUp());
                    	 out.println("Open`/;" + next.getID() + "`/;" + next.getFlags() + "`/;" 
                    			 + next.getWidgets() + "`/;" + next.getColor());
                		 
                	 } else {
                		 out.println("Close`/;");
                	 }
                  } 
                  else {
                  //invalid
                  }
               } 
                   catch (Exception e) {}
            }
         }
      }
   
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
					String userName = "";
					String password = "";
					Socket adminSocket = null;
					BufferedReader adminIn = null;
					PrintWriter adminOut = null;
					
               try {
                  System.out.println("Waiting for admin on push server");
                  adminSocket = connectionSocket.accept();
                  adminSocket.setKeepAlive(true);
                  System.out.println("Admin connected, authenticating now");
                  adminIn = new BufferedReader(new InputStreamReader(adminSocket.getInputStream()));
                  adminOut = new PrintWriter(adminSocket.getOutputStream(), true);
                  userName = adminIn.readLine();
                  password = adminIn.readLine();
               } catch (IOException e){}
               /*
               try{
                  adminStatement = connect.createStatement();
               } 
                   catch (com.mysql.jdbc.exceptions.jdbc4.CommunicationsException e){
                     connectToSQLDatabase();
							try {
                     	adminStatement = connect.createStatement();
							} catch (Exception derp) {}
                  } 
                   catch (Exception e){}
               try {
                  String query = "SELECT * FROM users WHERE username='" 
                                     + userName + "' AND password='" + password + "'";
                  adminResultSet = adminStatement.executeQuery(query);
                  if (adminResultSet.next()) {
                     if (admins.containsKey(userName)) {
                        System.out.println("Already have admin key");
                        PushAdmin oldAdmin = admins.get(userName);
                        System.out.println("Calling admin reconnected");
                        oldAdmin.adminReconnected(adminSocket, adminIn, adminOut);	
                     }
                     else {
                        admins.put(userName, new PushAdmin(userName, adminSocket, adminIn, adminOut, getPushServer()));
                     }
                  }
               } catch (Exception e) {e.printStackTrace();}
               */
             if (userName.equals("frederis") && password.equals("testpw")) {
                 if (admins.containsKey(userName)) {
                     System.out.println("Already have admin key");
                     PushAdmin oldAdmin = admins.get(userName);
                     System.out.println("Calling admin reconnected");
                     oldAdmin.adminReconnected(adminSocket, adminIn, adminOut);	
                  }
                  else {
                     admins.put(userName, new PushAdmin(userName, adminSocket, adminIn, adminOut, getPushServer()));
                  } 
             }
            }
         }
      }
   
   	
   private class DisplayListener implements Runnable {
   	
   	private ServerSocket displaySocket;
   	
   	public DisplayListener(int port) {
   		try {
   			displaySocket = new ServerSocket(port);
   		} 
   		catch (IOException e) {
   			System.out.println("Could not listen on port " + port);
   		}
   	}
   	
   	public void run() {
            while (true) {
            	try {
            		System.out.println("Waiting for display on push server");
            		Socket display = displaySocket.accept();
            		display.setKeepAlive(true);
            		display.setSoTimeout(100);
            		BufferedReader displayIn = new BufferedReader(new InputStreamReader(display.getInputStream()));
   				PrintWriter displayOut = new PrintWriter(display.getOutputStream(), true);
   				String requestedAdmin = displayIn.readLine();
   				if (admins.containsKey(requestedAdmin)) {
   					admins.get(requestedAdmin).processDisplay(display, displayIn, displayOut);
   				}
            	} catch (IOException e) {}
            }
   	}
   }	
   
   }
