import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class PushAdmin {
    
    private static final int heartbeatTime = 32;
    
    private final Map<String, Client> clients;
    private final Map<String, ArrayList<String>> groups;
    private String questionForEveryone;
    private final Map<String, String> groupQuestions;
    private final String ID;
    private Socket adminSocket;
    private BufferedReader in;
    private PrintWriter out;
    private CommandHandler commandHandler;
    private final ArrayList<Display> displays;
    private final PushServer server;
    private Timer timer;
    
    public PushAdmin(final String ID, final PushServer server) {
        // public PushAdmin(String ID, Socket adminSocket, BufferedReader in,
        // PrintWriter out, PushServer server) {
        this.ID = ID;
        // this.adminSocket = adminSocket;
        // this.in = in;
        // this.out = out;
        this.server = server;
        clients = Collections.synchronizedMap(new HashMap<String, Client>(50));
        groups = Collections.synchronizedMap(new HashMap<String, ArrayList<String>>(50));
        questionForEveryone = "";
        groupQuestions = Collections.synchronizedMap(new HashMap<String, String>(50));
        displays = new ArrayList<Display>();
        // commandHandler = new CommandHandler();
        // timer = new Timer();
        // new Thread(commandHandler).start();
        // answerUpdater = new AnswerUpdater();
        // new Thread(answerUpdater).start();
        
        // timer.schedule(new PauseTask(), heartbeatTime * 1000);
    }
    
    private void sendMessage(final String message) {
        if (out != null) {
            out.println(message);
        }
    }
    
    public void handleResponse(final String clientID, final String response) {
        System.out.println("Client " + clientID + " responsed with " + response);
    }
    
    public void clientDied(final String clientID) {
        System.out.println("Client " + clientID + " has died");
    }
    
    public void displayDied(final int displayID) {
        
    }
    
    public void adminConnected(final Socket newSocket, final BufferedReader newIn, final PrintWriter newOut) {
        adminReconnected(newSocket, newIn, newOut);
    }
    
    public void adminReconnected(final Socket newSocket, final BufferedReader newIn, final PrintWriter newOut) {
        if (commandHandler != null) {
            commandHandler.pause();
        }
        adminSocket = newSocket;
        in = newIn;
        out = newOut;
        for (int i = 0; i < displays.size(); i++) {
            sendMessage("DisplayConnected" + PushServer.GRAVE_SEPARATOR + displays.get(i).getIConsume());
        }
        commandHandler = new CommandHandler();
        timer = new Timer();
        new Thread(commandHandler).start();
        timer.schedule(new PauseTask(), heartbeatTime * 1000);
    }
    
    public void updateGroupQuestions() {
        final Iterator<String> gcIter = groupQuestions.keySet().iterator();
        while (gcIter.hasNext()) {
            final String gcNext = gcIter.next();
            if (!groups.containsKey(gcNext)) {
                gcIter.remove();
            }
        }
        final Iterator<String> gIter = groups.keySet().iterator();
        while (gIter.hasNext()) {
            final String gNext = gIter.next();
            if (!groupQuestions.containsKey(gNext)) {
                groupQuestions.put(gNext, "");
            }
        }
    }
    
    public void processClient(final Socket clientSocket,
            final String clientID,
            final String clientMAC,
            final BufferedReader clientIn,
            final PrintWriter clientOut,
            final String groupName) {
        System.out.println("in process client");
        if (clients.containsKey(clientID)) {
            final Client oldClient = clients.get(clientID);
            if (oldClient.getMacAddress().equals(clientMAC)) {
                try {
                    oldClient.getSocket().close();
                } catch (final IOException e) {
                }
                
                oldClient.clientReconnected(clientSocket, clientIn, clientOut);
                final String oldGroup = oldClient.getGroup();
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
            } else {
                sendMessage("DuplicateID");
                try {
                    clientSocket.close();
                } catch (final IOException e) {
                }
            }
        } else {
            clients.put(clientID, new Client(clientID, clientMAC, clientSocket, clientIn, clientOut, this));
            if (!groupName.equals("Ungrouped")) {
                if (groups.containsKey(groupName)) {
                    groups.get(groupName).add(clientID);
                    System.out.println("Added person to existing group: " + groupName);
                } else {
                    final ArrayList<String> newGroupList = new ArrayList<String>();
                    newGroupList.add(clientID);
                    groups.put(groupName, newGroupList);
                    groupQuestions.put(groupName, "");
                    System.out.println("Added person to new group: " + groupName);
                }
                clients.get(clientID).setGroup(groupName);
                final Iterator<String> groupIter = groups.keySet().iterator();
                String groupUpdate = "GroupList" + PushServer.GRAVE_SEPARATOR;
                while (groupIter.hasNext()) {
                    groupUpdate += groupIter.next() + PushServer.SEMI_COLON_SEPARATOR;
                }
                out.println(groupUpdate);
            }
        }
        System.out.println("Client connected to admin " + ID + " with an id of " + clientID);
        // out.println("ClientConnected;" + clientID);
    }
    
    public void processDisplay(final Socket display, final BufferedReader displayIn, final PrintWriter displayOut) {
        final Display newDisplay = new Display(displays.size() - 1, display, displayIn, displayOut, this);
        displays.add(newDisplay);
        sendMessage("DisplayConnected" + PushServer.GRAVE_SEPARATOR + newDisplay.getIConsume());
    }
    
    public String getID() {
        return ID;
    }
    
    public synchronized void updateAnswer(final String clientAndAnswer) {
        for (int i = 0; i < displays.size(); i++) {
            displays.get(i).sendMessage(clientAndAnswer);
        }
        System.out.println("Sending: " + clientAndAnswer + " to " + displays.size() + " displays");
        sendMessage(clientAndAnswer);
    }
    
    public boolean isPaused() {
        return commandHandler.isPaused();
    }
    
    private class PauseTask extends TimerTask {
        
        @Override
        public void run() {
            commandHandler.pause();
            System.out.println("No heartbeat from admin, pausing now");
        }
    }
    
    private class CommandHandler implements Runnable {
        
        private boolean paused = false;
        
        @Override
        public void run() {
            while (!paused) {
                try {
                    // System.out.println("Starting a readline");
                    final String command = in.readLine();
                    // System.out.println("Finished a readline");
                    if (command == null) {
                        paused = true;
                        System.out.println("Read null, pausing admin now");
                    } else if (command.equals("AreYouStillThere")) {
                        timer.cancel();
                        timer = new Timer();
                        timer.schedule(new PauseTask(), heartbeatTime * 1000);
                        sendMessage("YesImHere");
                        System.out.println("Received admin heartbeat and reset timer");
                    } else {
                        System.out.println(command);
                        final String[] directiveParts = command.split(PushServer.GRAVE_SEPARATOR);
                        if (directiveParts[0].equals("ClientCommand")) {
                            final String[] ccParts = directiveParts[1].split(PushServer.AMPERSAND_SEPARATOR);
                            final String[] commandParts = ccParts[0].split(PushServer.SEMI_COLON_SEPARATOR);
                            if (commandParts[0].equals("Open") || commandParts[0].equals("OpenClickPad")) {
                                System.out.println("Got the command to open a question");
                                String displayGString = "";
                                final String[] groupsToOpen = ccParts[2].split(PushServer.COMMA_SEPARATOR);
                                
                                if ((groupsToOpen.length == 1) && groupsToOpen[0].equals("Everyone")) {
                                    final Iterator<Map.Entry<String, Client>> iter = clients.entrySet().iterator();
                                    while (iter.hasNext()) {
                                        iter.next().getValue().startQuestion(ccParts[0]);
                                    }
                                    displayGString = "Everyone" + PushServer.COLON_SEPARATOR + clients.size() + PushServer.COMMA_SEPARATOR;
                                    questionForEveryone = ccParts[0];
                                } else {
                                    for (final String gName : groupsToOpen) {
                                        final ArrayList<String> groupList = groups.get(gName);
                                        groupQuestions.put(gName, ccParts[0]);
                                        displayGString += gName + PushServer.COLON_SEPARATOR + groupList.size() + PushServer.COMMA_SEPARATOR;
                                        for (int j = 0; j < groupList.size(); j++) {
                                            clients.get(groupList.get(j)).startQuestion(ccParts[0]);
                                        }
                                    }
                                }
                                for (int i = 0; i < displays.size(); i++) {
                                    displays.get(i).sendMessage(ccParts[0] + PushServer.AMPERSAND_SEPARATOR + ccParts[1] + PushServer.AMPERSAND_SEPARATOR + displayGString);
                                }
                            } else if (commandParts[0].equals("Close")) {
                                for (int i = 0; i < displays.size(); i++) {
                                    displays.get(i).sendMessage(directiveParts[1]);
                                }
                                final String[] groupsToClose = commandParts[1].split(PushServer.COMMA_SEPARATOR);
                                if ((groupsToClose.length == 1) && groupsToClose[0].equals("Everyone")) {
                                    questionForEveryone = "";
                                    final Iterator<Map.Entry<String, Client>> iter = clients.entrySet().iterator();
                                    while (iter.hasNext()) {
                                        iter.next().getValue().stopQuestion();
                                    }
                                } else {
                                    for (final String gName : groupsToClose) {
                                        final ArrayList<String> groupList = groups.get(gName);
                                        groupQuestions.put(gName, "");
                                        // check 304
                                        for (int j = 0; j < groupList.size(); j++) {
                                            clients.get(groupList.get(j)).stopQuestion();
                                        }
                                    }
                                }
                            }
                        } else if (directiveParts[0].equals("GetQuestionSets")) {
                            System.out.println("Got request for sets");
                            /*
                             * try { ResultSet results = server.runQuery(
                             * "SELECT DISTINCT idkey FROM questions WHERE admin='"
                             * +ID+"'"); String finalResult = ""; while
                             * (results.next()) { finalResult = finalResult +
                             * results.getString("idkey") + "&"; }
                             * out.println("AllSets`" + finalResult);
                             * System.out.println("Responding with: " +
                             * finalResult); } catch (Exception e)
                             * {e.printStackTrace();}
                             */
                            out.println(server.getQuestionSets());
                        } else if (directiveParts[0].equals("AddQuestionSet")) {
                            server.addQuestionSet(directiveParts[1], directiveParts[2]);
                        } else if (directiveParts[0].equals("DeleteQuestionSet")) {
                            if (directiveParts.length > 1) {
                                server.deleteQuestionSet(directiveParts[1]);
                            }
                        } else if (directiveParts[0].equals("GetAllQuestions")) {
                            System.out.println("Got request for all from set");
                            /*
                             * try { ResultSet results = server.runQuery(
                             * "SELECT * FROM questions WHERE idkey='"
                             * +directiveParts[1]+"'  AND admin='"+ID+"'");
                             * String finalResult = ""; while (results.next()) {
                             * String qString = results.getString("questionid")
                             * + ";" + results.getString("questionstring");
                             * finalResult = finalResult + qString + "&"; }
                             * out.println("QuestionSet`" + finalResult);
                             * System.out.println("Responding with: " +
                             * finalResult); } catch (Exception e)
                             * {e.printStackTrace();}
                             */
                            out.println(server.getQuestionsInSet(directiveParts[1]));
                        } else if (directiveParts[0].equals("GetClientList")) {
                            final Iterator<String> iter = groups.keySet().iterator();
                            String output = "";
                            while (iter.hasNext()) {
                                final String groupName = iter.next();
                                output = output + groupName + PushServer.SEMI_COLON_SEPARATOR;
                                final ArrayList<String> groupMembers = groups.get(groupName);
                                for (int i = 0; i < groupMembers.size(); i++) {
                                    if (i != 0) {
                                        output = output + PushServer.COMMA_SEPARATOR;
                                    }
                                    output = output + groupMembers.get(i);
                                }
                                output = output + PushServer.AMPERSAND_SEPARATOR;
                            }
                            final Iterator<Map.Entry<String, Client>> cIter = clients.entrySet().iterator();
                            String notGrouped = "Not grouped" + PushServer.SEMI_COLON_SEPARATOR;
                            while (cIter.hasNext()) {
                                final Map.Entry<String, Client> next = cIter.next();
                                if (next.getValue().getGroup().equals("")) {
                                    notGrouped += next.getKey() + PushServer.SEMI_COLON_SEPARATOR;
                                }
                            }
                            notGrouped += PushServer.AMPERSAND_SEPARATOR;
                            output = "ClientList" + PushServer.GRAVE_SEPARATOR + notGrouped + output;
                            output = output.substring(0, output.length() - 3);
                            System.out.println("Returning: " + output);
                            sendMessage(output);
                        } else if (directiveParts[0].equals("UpdateClientList")) {
                            groups.clear();
                            final String[] groupStrings = directiveParts[1].split(PushServer.AMPERSAND_SEPARATOR);
                            for (final String groupString : groupStrings) {
                                final String[] groupParts = groupString.split(PushServer.SEMI_COLON_SEPARATOR);
                                if (groupParts[0].equals("Not grouped")) {
                                    if (groupParts.length > 1) {
                                        final String[] nogroupClients = groupParts[1].split(PushServer.COMMA_SEPARATOR);
                                        for (final String nogroupClient : nogroupClients) {
                                            clients.get(nogroupClient).unsetGroup();
                                        }
                                    }
                                } else {
                                    final ArrayList<String> newGroupListing = new ArrayList<String>();
                                    if (groupParts.length > 1) {
                                        final String[] newMembers = groupParts[1].split(PushServer.COMMA_SEPARATOR);
                                        Collections.addAll(newGroupListing, newMembers);
                                        for (final String newMember : newMembers) {
                                            clients.get(newMember).setGroup(groupParts[0]);
                                        }
                                    }
                                    groups.put(groupParts[0], newGroupListing);
                                }
                            }
                            updateGroupQuestions();
                        } else {
                            System.out.println("Invalid command: " + command);
                        }
                    }
                } catch (final SocketTimeoutException e) {
                } catch (final IOException e) {
                    paused = true;
                    System.out.println(e.getMessage());
                    System.out.println("Admin is paused");
                }
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException e) {
                }
            }
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
            }
            timer.cancel();
            adminSocket = null;
        }
        
        public void unpause() {
            paused = false;
            System.out.println("Admin just got unpaused " + System.currentTimeMillis());
        }
        
        public void pause() {
            paused = true;
            timer.cancel();
            System.out.println("Just paused admin " + System.currentTimeMillis());
        }
        
        public boolean isPaused() {
            return paused;
        }
    }
    
}
