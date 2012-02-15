import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InterruptedIOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.ConcurrentModificationException;
import java.sql.ResultSet;
import java.util.Timer;
import java.util.TimerTask;


public class PushAdmin {
    
    private static final int heartbeatTime = 32;
    
    private Map<String, Client> clients;
    private Map<String, ArrayList<String>> groups;
    private String questionForEveryone;
    private Map<String, String> groupQuestions;
    private String ID;
    private Socket adminSocket;
    private BufferedReader in;
    private PrintWriter out;
    //private DisconnectionListener DCListener;
    private CommandHandler commandHandler;
    //private AnswerUpdater answerUpdater;
    private ArrayList<Display> displays;
    private PushServer server;
    private Timer timer;
    
    public PushAdmin(String ID, PushServer server) {
        //	public PushAdmin(String ID, Socket adminSocket, BufferedReader in, PrintWriter out, PushServer server) {
        this.ID = ID;
        //		this.adminSocket = adminSocket;
        //		this.in = in;
        //		this.out = out;
        this.server = server;
        clients = Collections.synchronizedMap(new HashMap<String, Client>(50));
        groups = Collections.synchronizedMap(new HashMap<String, ArrayList<String>>(50));
        questionForEveryone = "";
        groupQuestions = Collections.synchronizedMap(new HashMap<String, String>(50));
        //groups.put("Ungrouped", new ArrayList<String>());
        displays = new ArrayList<Display>();
        //DCListener = new DisconnectionListener();
        //new Thread(DCListener).start();
        //		commandHandler = new CommandHandler();
        //		timer = new Timer();
        //		new Thread(commandHandler).start();
        //answerUpdater = new AnswerUpdater();
        //new Thread(answerUpdater).start();
        
        //		timer.schedule(new PauseTask(), heartbeatTime * 1000);
    }
    
    private void sendMessage(String message) {
        if(out != null) {
            out.println(message);
        }
    }
    
    public void handleResponse(String clientID, String response) {
        System.out.println("Client " + clientID + " responsed with " + response);
    }
    
    public void clientDied(String clientID) {
        System.out.println("Client " + clientID + " has died");
    }
    
    public void displayDied(int displayID) {
        
    }
    
    public void adminConnected(Socket newSocket, BufferedReader newIn, PrintWriter newOut) {
        adminReconnected(newSocket, newIn, newOut);
    }
    
    public void adminReconnected(Socket newSocket, BufferedReader newIn, PrintWriter newOut) {
        if(commandHandler != null) {
            commandHandler.pause();
        }
        adminSocket = newSocket;
        in = newIn;
        out = newOut;
        for (int i=0; i<displays.size(); i++) {
            sendMessage("DisplayConnected`/`" + displays.get(i).getIConsume());
        }
        commandHandler = new CommandHandler();
        timer = new Timer();
        new Thread(commandHandler).start();
        timer.schedule(new PauseTask(), heartbeatTime * 1000);
    }
    
    public void updateGroupQuestions() {
        Iterator<String> gcIter = groupQuestions.keySet().iterator();
        while (gcIter.hasNext()) {
            String gcNext = gcIter.next();
            if (!groups.containsKey(gcNext)) {
                gcIter.remove();
            }
        }
        Iterator<String> gIter = groups.keySet().iterator();
        while (gIter.hasNext()) {
            String gNext = gIter.next();
            if (!groupQuestions.containsKey(gNext)) {
                groupQuestions.put(gNext, "");
            }
        }
    }
    
    
    public void processClient(Socket clientSocket, String clientID, String clientMAC, BufferedReader clientIn, PrintWriter clientOut, String groupName) {
        System.out.println("in process client");
        if (clients.containsKey(clientID)) {
            Client oldClient = clients.get(clientID);
            if (oldClient.getMacAddress().equals(clientMAC)) {
                try {
                    oldClient.getSocket().close();
                } catch (IOException e) {}
                
                oldClient.clientReconnected(clientSocket, clientIn, clientOut);
                String oldGroup = oldClient.getGroup();
                if (oldGroup.equals("")) {
                    if (!questionForEveryone.equals("")) {
                        oldClient.startQuestion(questionForEveryone);
                    }
                } else {
                    oldClient.setGroup(oldGroup);
                    if (!groupQuestions.get(oldGroup).equals("")) {
                        oldClient.startQuestion(groupQuestions.get(oldGroup));
                    } else if (!questionForEveryone.equals("")) {
                        oldClient.startQuestion(questionForEveryone);
                    }
                }
            }
            else {
                sendMessage("DuplicateID");
                try {
                    clientSocket.close();
                } catch (IOException e) {}
            }
        }
        else {
            clients.put(clientID, new Client(clientID, clientMAC, clientSocket, clientIn, clientOut, this));
            if (!groupName.equals("Ungrouped")) {
                if (groups.containsKey(groupName)) {
                    groups.get(groupName).add(clientID);
                    System.out.println("Added person to existing group: " + groupName);
                } else {
                    ArrayList<String> newGroupList = new ArrayList<String>();
                    newGroupList.add(clientID);
                    groups.put(groupName, newGroupList);
                    groupQuestions.put(groupName, "");
                    System.out.println("Added person to new group: " + groupName);
                }
                clients.get(clientID).setGroup(groupName);
                Iterator<String> groupIter = groups.keySet().iterator();
                String groupUpdate = "GroupList`/`";
                while (groupIter.hasNext()) {
                    groupUpdate += groupIter.next() + "`/;";
                }
                out.println(groupUpdate);
            }
        }
        System.out.println("Client connected to admin " + ID + " with an id of " + clientID);
        //out.println("ClientConnected;" + clientID);
    }
    
    public void processDisplay(Socket display, BufferedReader displayIn, PrintWriter displayOut) {
        Display newDisplay = new Display(displays.size() - 1, display, displayIn, displayOut, this);
        displays.add(newDisplay);
        sendMessage("DisplayConnected`/`" + newDisplay.getIConsume());
    }
    
    public String getID() {
        return ID;
    }
    
    public synchronized void updateAnswer(String clientAndAnswer) {
        for (int i=0; i<displays.size(); i++) {
            displays.get(i).sendMessage(clientAndAnswer);
        }
        System.out.println("Sending: " + clientAndAnswer + " to " + displays.size() + " displays");
        sendMessage(clientAndAnswer);
    }
    
    public boolean isPaused() {
        return commandHandler.isPaused();
    }
    
    private class PauseTask extends TimerTask {
        public void run() {
            commandHandler.pause();
            System.out.println("No heartbeat from admin, pausing now");
        }
    }
    
    
    private class CommandHandler implements Runnable{
        
        private boolean paused = false;
        
        public void run() {
            while (!paused) {
                try {
                    //System.out.println("Starting a readline");
                    String command = in.readLine();
                    //System.out.println("Finished a readline");
                    if (command == null) {
                        paused = true;
                        System.out.println("Read null, pausing admin now");
                    } else if (command.equals("AreYouStillThere")) {
                        timer.cancel();
                        timer = new Timer();
                        timer.schedule(new PauseTask(), heartbeatTime * 1000);
                        sendMessage("YesImHere");
                        System.out.println("Received admin heartbeat and reset timer");
                    }
                    else {
                        System.out.println(command);
                        String[] directiveParts = command.split("`/`");
                        if (directiveParts[0].equals("ClientCommand")) {
                            String[] ccParts = directiveParts[1].split("`/&");
                            String[] commandParts = ccParts[0].split("`/;");
                            if (commandParts[0].equals("Open") || commandParts[0].equals("OpenClickPad")) {
                                System.out.println("Got the command to open a question");
                                String displayGString = "";
                                String[] groupsToOpen = ccParts[2].split("`/,");
                                
                                if (groupsToOpen.length == 1 && groupsToOpen[0].equals("Everyone")) {
                                    Iterator<Map.Entry<String, Client>> iter = clients.entrySet().iterator();
                                    while (iter.hasNext()) {
                                        iter.next().getValue().startQuestion(ccParts[0]);
                                    }
                                    displayGString = "Everyone`/:" + clients.size() + "`/,";
                                    questionForEveryone = ccParts[0];
                                } else {
                                    for (int i=0; i<groupsToOpen.length; i++) {
                                        String gName = groupsToOpen[i];
                                        ArrayList<String> groupList = groups.get(gName);
                                        groupQuestions.put(gName, ccParts[0]);
                                        displayGString += gName + "`/:" + groupList.size() + "`/,";
                                        for (int j=0; j<groupList.size(); j++) {
                                            clients.get(groupList.get(j)).startQuestion(ccParts[0]);
                                        }
                                    }
                                }
                                for (int i=0; i<displays.size(); i++) {
                                    displays.get(i).sendMessage(ccParts[0] + "`/&" + ccParts[1] + "`/&" + displayGString);
                                }
                            }
                            else if (commandParts[0].equals("Close")) {
                                for (int i=0; i<displays.size(); i++) {
                                    displays.get(i).sendMessage(directiveParts[1]);
                                }
                                String[] groupsToClose = commandParts[1].split("`/,");
                                if (groupsToClose.length == 1 && groupsToClose[0].equals("Everyone")) {
                                    questionForEveryone = "";
                                    Iterator<Map.Entry<String, Client>> iter = clients.entrySet().iterator();
                                    while (iter.hasNext()) {
                                        iter.next().getValue().stopQuestion();
                                    }
                                } else {
                                    for (int i=0; i<groupsToClose.length; i++) {
                                        String gName = groupsToClose[i];
                                        ArrayList<String> groupList = groups.get(gName);
                                        groupQuestions.put(gName, "");
                                        //check 304
                                        for (int j=0; j<groupList.size(); j++) {
                                            clients.get(groupList.get(j)).stopQuestion();
                                        }
                                    }
                                }
                            }
                        }
                        else if (directiveParts[0].equals("GetQuestionSets")) {
                            System.out.println("Got request for sets");
                            /*
									try {
										ResultSet results = server.runQuery("SELECT DISTINCT idkey FROM questions WHERE admin='"+ID+"'");
									String finalResult = "";
									while (results.next()) {
										finalResult = finalResult + results.getString("idkey") + "&";
									}
									out.println("AllSets`" + finalResult);
									System.out.println("Responding with: " + finalResult);
									} catch (Exception e) {e.printStackTrace();}
                             */
                            out.println(server.getQuestionSets());
                        }
                        else if (directiveParts[0].equals("AddQuestionSet")) {
                            server.addQuestionSet(directiveParts[1], directiveParts[2]);
                        }
                        else if (directiveParts[0].equals("DeleteQuestionSet")) {
                            if (directiveParts.length > 1) {
                                server.deleteQuestionSet(directiveParts[1]);
                            }
                        }
                        else if (directiveParts[0].equals("GetAllQuestions")) {
                            System.out.println("Got request for all from set");
                            /*
								try {
									ResultSet results = server.runQuery("SELECT * FROM questions WHERE idkey='"+directiveParts[1]+"'  AND admin='"+ID+"'");
									String finalResult = "";
									while (results.next()) {
								  		String qString = results.getString("questionid") + ";" + results.getString("questionstring");
										finalResult = finalResult + qString + "&";
									}
									out.println("QuestionSet`" + finalResult);
									System.out.println("Responding with: " + finalResult);
								} catch (Exception e) {e.printStackTrace();}
                             */
                            out.println(server.getQuestionsInSet(directiveParts[1]));
                        }
                        else if (directiveParts[0].equals("GetClientList")) {
                            Iterator<String> iter = groups.keySet().iterator();
                            String output = "";
                            while (iter.hasNext()) {
                                String groupName = iter.next();
                                output = output + groupName + "`/;";
                                ArrayList<String> groupMembers = groups.get(groupName);
                                for (int i=0; i<groupMembers.size(); i++) {
                                    if (i != 0)
                                        output = output + "`/,";
                                    output = output + groupMembers.get(i);
                                }
                                output = output + "`/&";
                            }
                            Iterator<Map.Entry<String, Client>> cIter = clients.entrySet().iterator();
                            String notGrouped = "Not grouped`/;";
                            while (cIter.hasNext()) {
                                Map.Entry<String, Client> next = cIter.next();
                                if (next.getValue().getGroup().equals("")) {
                                    notGrouped += next.getKey() + "`/,";
                                }
                            }
                            notGrouped += "`/&";
                            output = "ClientList`/`" + notGrouped + output;
                            output = output.substring(0, output.length() - 3);
                            System.out.println("Returning: " + output);
                            sendMessage(output);
                        }
                        else if (directiveParts[0].equals("UpdateClientList")) {
                            groups.clear();
                            String[] groupStrings = directiveParts[1].split("`/&");
                            for (int i=0; i<groupStrings.length; i++) {
                                String[] groupParts = groupStrings[i].split("`/;");
                                if (groupParts[0].equals("Not grouped")) {
                                    if (groupParts.length > 1) {
                                        String[] nogroupClients = groupParts[1].split("`/,");
                                        for (int j=0; j<nogroupClients.length; j++) {
                                            clients.get(nogroupClients[j]).unsetGroup();
                                        }
                                    }
                                } else {
                                    ArrayList<String> newGroupListing = new ArrayList<String>();
                                    if (groupParts.length > 1) {
                                        String[] newMembers = groupParts[1].split("`/,");
                                        Collections.addAll(newGroupListing, newMembers);
                                        for (int j=0; j<newMembers.length; j++) {
                                            clients.get(newMembers[j]).setGroup(groupParts[0]);
                                        }
                                    }
                                    groups.put(groupParts[0], newGroupListing);
                                }
                            }
                            updateGroupQuestions();
                        }
                        else {
                            System.out.println("Invalid command: " + command);
                        }
                    }
                } catch (SocketTimeoutException e) {
                } catch (IOException e) {
                    paused = true;
                    System.out.println(e.getMessage());
                    System.out.println("Admin is paused");
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {}
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
            timer.cancel();
            adminSocket = null;
        }
        
        public void unpause() {
            paused = false;
            System.out.println("Admin just got unpaused "+System.currentTimeMillis());
        }
        
        public void pause() {
            paused = true;
            timer.cancel();
            System.out.println("Just paused admin "+System.currentTimeMillis());
        }
        
        public boolean isPaused() {
            return paused;
        }
    }
    
    
    /*
	private class AnswerUpdater implements Runnable {
		public void run() {
			while (true) {
				String combinedAnswer = "";
				Iterator<String> iter = clients.keySet().iterator();
				while (iter.hasNext()) {
					Client nextClient = clients.get(iter.next());
					if (nextClient.onQuestion()) {
						combinedAnswer = combinedAnswer + nextClient.getAnswer() + ";";
					}
				}
				for (int i=0; i<displays.size(); i++) {
					displays.get(i).sendMessage(combinedAnswer);
				}
				try {
					Thread.sleep(400);
				} catch (InterruptedException e) {}
			}
		}
	}
     */
    
    
}
