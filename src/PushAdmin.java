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

// TODO: Auto-generated Javadoc
/**
 * The Class PushAdmin.
 */
public class PushAdmin {
    
    
    
    /** The Constant heartbeatTime. 
     * <br>This is the maximum amount of time that the server will 
     * wait for an admin to have sent a heartbeat request. If this time 
     * has elapsed without having recieved a request, the admin is 
     * considered to have disconnected and the connection will be closed.*/
    private static final int heartbeatTimeSeconds = 32;
    
    /** The map of clients. <br>Maps(Name:String, Client:client)*/
    private final Map<String, Client> clientMap;
    
    /** The map of groups.<br>
     * Maps group name to an arraylist of the client names in the group */
    private final Map<String, ArrayList<String>> groupMap;
    
    /** The question for everyone. */
    private String questionForEveryone;
    
    /** The group questions. */
    private final Map<String, String> groupQuestions;
    
    /** The ID. */
    private final String adminId;
    
    /** The admin socket. */
    private Socket adminSocket;
    
    /** The in. */
    private BufferedReader in;
    
    /** The out. */
    private PrintWriter out;
    
    /** The command handler. */
    private CommandHandler commandHandler;
    
    /** The displays. */
    private final ArrayList<Display> displays;
    
    /** The server. */
    private final PushServer server;
    
    /** The timer. */
    private Timer timer;
    
    /**
     * Instantiates a new push admin.
     * 
     * @param ID
     *            the iD
     * @param server
     *            the server
     */
    public PushAdmin(final String ID, final PushServer server) {
        // public PushAdmin(String ID, Socket adminSocket, BufferedReader in,
        // PrintWriter out, PushServer server) {
        this.adminId = ID;
        // this.adminSocket = adminSocket;
        // this.in = in;
        // this.out = out;
        this.server = server;
        clientMap = Collections.synchronizedMap(new HashMap<String, Client>(50));
        groupMap = Collections.synchronizedMap(new HashMap<String, ArrayList<String>>(50));
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
    
    /**
     * Send message.
     * 
     * @param message
     *            the message
     */
    private void sendMessage(final String message) {
        if (out != null) {
            out.println(message);
            out.flush();
        }
    }
    
    /**
     * Handle response.
     * 
     * @param clientID
     *            the client id
     * @param response
     *            the response
     */
    public void handleResponse(final String clientID, final String response) {
        System.out.println("Client " + clientID + " responsed with " + response);
    }
    
    /**
     * Client died.
     * 
     * @param clientID
     *            the client id
     */
    public void clientDied(final String clientID) {
        System.out.println("Client " + clientID + " has died");
    }
    
    /**
     * Display died.
     * 
     * @param displayID
     *            the display id
     */
    public void displayDied(final int displayID) {
        
    }
    
    /**
     * Admin connected.
     * 
     * @param newSocket
     *            the new socket
     * @param newIn
     *            the new in
     * @param newOut
     *            the new out
     */
    public void connectAdmin(final Socket newSocket,
            final BufferedReader newIn,
            final PrintWriter newOut) {
        reconnectAdmin(newSocket, newIn, newOut);
    }
    
    /**
     * Admin reconnected.
     * 
     * @param newSocket
     *            the new socket
     * @param newIn
     *            the new in
     * @param newOut
     *            the new out
     */
    public void reconnectAdmin(final Socket newSocket,
            final BufferedReader newIn,
            final PrintWriter newOut) {
        if (commandHandler != null) {
            commandHandler.pause();
        }
        adminSocket = newSocket;
        in = newIn;
        out = newOut;
        for (int i = 0; i < displays.size(); i++) {
            sendMessage(Constants.DISPLAY_CONNECTED + PushServer.GRAVE_SEPARATOR + displays.get(i).getIConsume());
        }
        commandHandler = new CommandHandler();
        timer = new Timer();
        new Thread(commandHandler).start();
        timer.schedule(new PauseTask(), heartbeatTimeSeconds * 1000);
    }
    
    /**
     * Update group questions.
     */
    public void updateGroupQuestions() {
        final Iterator<String> gcIter = groupQuestions.keySet().iterator();
        while (gcIter.hasNext()) {
            final String gcNext = gcIter.next();
            if (!groupMap.containsKey(gcNext)) {
                gcIter.remove();
            }
        }
        final Iterator<String> gIter = groupMap.keySet().iterator();
        while (gIter.hasNext()) {
            final String gNext = gIter.next();
            if (!groupQuestions.containsKey(gNext)) {
                groupQuestions.put(gNext, "");
            }
        }
    }
    
    /**
     * Process client.
     * 
     * @param clientSocket
     *            the client socket
     * @param clientId
     *            the client id
     * @param clientMac
     *            the client mac
     * @param clientIn
     *            the client in
     * @param clientOut
     *            the client out
     * @param groupName
     *            the group name
     */
    public void processClient(final Socket clientSocket,
            final String clientId,
            final String clientMac,
            final BufferedReader clientIn,
            final PrintWriter clientOut,
            final String groupName) {
        System.out.println("in process client");
        if (clientMap.containsKey(clientId)) {
            final Client oldClient = clientMap.get(clientId);
            if (oldClient.getMacAddress().equals(clientMac)) {
                try {
                    oldClient.getSocket().close();
                } catch (final IOException e) {
                }
                
                oldClient.clientReconnected(clientSocket, clientIn, clientOut);
                final String oldGroupName = oldClient.getGroup();
                if (oldGroupName.equals("")) {
                    if (!questionForEveryone.equals("")) {
                        oldClient.startQuestion(questionForEveryone);
                    }
                } else {
                    oldClient.setGroup(oldGroupName);
                    if (!groupQuestions.get(oldGroupName).equals("")) {
                        oldClient.startQuestion(groupQuestions.get(oldGroupName));
                    } else if (!questionForEveryone.equals("")) {
                        oldClient.startQuestion(questionForEveryone);
                    }
                }
            } else {
                sendMessage(Constants.DUPLICATE_ID);
                try {
                    clientSocket.close();
                } catch (final IOException e) {
                }
            }
        } else {
            clientMap.put(clientId, new Client(clientId,
                    clientMac,
                    clientSocket,
                    clientIn,
                    clientOut,
                    this));
            if (!groupName.equals(Constants.UNGROUPED)) {
                if (groupMap.containsKey(groupName)) {
                    groupMap.get(groupName).add(clientId);
                    System.out.println("Added person to existing group: " + groupName);
                } else {
                    final ArrayList<String> newGroupList = new ArrayList<String>();
                    newGroupList.add(clientId);
                    groupMap.put(groupName, newGroupList);
                    groupQuestions.put(groupName, "");
                    System.out.println("Added person to new group: " + groupName);
                }
                clientMap.get(clientId).setGroup(groupName);
                final Iterator<String> groupIter = groupMap.keySet().iterator();
                String groupUpdate = Constants.GROUP_LIST + PushServer.GRAVE_SEPARATOR;
                while (groupIter.hasNext()) {
                    groupUpdate += groupIter.next() + PushServer.SEMI_COLON_SEPARATOR;
                }
                out.println(groupUpdate);
            }
        }
        System.out.println("Client connected to admin " + adminId + " with an id of " + clientId);
        // out.println("ClientConnected;" + clientID);
    }
    
    /**
     * Process display.
     * 
     * @param displaySocket
     *            the display
     * @param displayIn
     *            the display in
     * @param displayOut
     *            the display out
     * @param displayName
     *            the display name
     */
    public void processDisplay(final Socket displaySocket,
            final BufferedReader displayIn,
            final PrintWriter displayOut,
            final String displayName) {
        final Display newDisplay = new Display(displays.size() - 1,
                displaySocket,
                displayIn,
                displayOut,
                this,
                displayName);
        displays.add(newDisplay);
        sendMessage(Constants.DISPLAY_CONNECTED + PushServer.GRAVE_SEPARATOR + newDisplay.getIConsume());
    }
    
    /**
     * Gets the admin id.
     * 
     * @return the admin id
     */
    public String getAdminId() {
        return adminId;
    }
    
    /**
     * Update answer.
     * 
     * @param clientAndAnswer
     *            the client and answer
     */
    public synchronized void updateAnswer(final String clientAndAnswer) {
        for (int i = 0; i < displays.size(); i++) {
            displays.get(i).sendMessage(clientAndAnswer);
        }
        System.out.println("Sending: " + clientAndAnswer + " to " + displays.size() + " displays");
        sendMessage(clientAndAnswer);
    }
    
    /**
     * Checks if is paused.
     * 
     * @return true, if is paused
     */
    public boolean isPaused() {
        return commandHandler.isPaused();
    }
    
    /**
     * The Class PauseTask.
     */
    private class PauseTask extends TimerTask {
        
        /*
         * (non-Javadoc)
         * 
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            commandHandler.pause();
            System.out.println("No heartbeat from admin, pausing now");
        }
    }
    
    /**
     * The Class CommandHandler.
     */
    private class CommandHandler implements Runnable {
        
        
        /** The paused. */
        private boolean paused = false;
        
        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
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
                    } else if (command.equals(Constants.CLIENT_HEARTBEAT_REQUEST)) {
                        timer.cancel();
                        timer = new Timer();
                        timer.schedule(new PauseTask(), heartbeatTimeSeconds * 1000);
                        sendMessage(Constants.SERVER_HEARTBEAT_RESPONSE);
                        System.out.println("Received admin heartbeat and reset timer");
                    } else {
                        System.out.println(command);
                        final String[] directiveParts = command.split(PushServer.GRAVE_SEPARATOR);
                        if (directiveParts[0].equals(Constants.CLIENT_COMMAND)) {
                            final String[] clientCommandParts = directiveParts[1].split(PushServer.AMPERSAND_SEPARATOR);
                            final String[] commandParts = clientCommandParts[0].split(PushServer.SEMI_COLON_SEPARATOR);
                            if (commandParts[0].equals(Constants.OPEN) || commandParts[0].equals(Constants.OPEN_CLICK_PAD)) {
                                System.out.println("Got the command to open a question");
                                String displayGroupString = "";
                                final String[] groupsToOpen = clientCommandParts[2].split(PushServer.COMMA_SEPARATOR);
                                
                                if ((groupsToOpen.length == 1) && groupsToOpen[0].equals(Constants.EVERYONE)) {
                                    final Iterator<Map.Entry<String, Client>> clientMapIterator = clientMap.entrySet().iterator();
                                    while (clientMapIterator.hasNext()) {
                                        clientMapIterator.next().getValue().startQuestion(clientCommandParts[0]);
                                    }
                                    displayGroupString = Constants.EVERYONE + PushServer.COLON_SEPARATOR + clientMap.size() + PushServer.COMMA_SEPARATOR;
                                    questionForEveryone = clientCommandParts[0];
                                } else {
                                    for (final String gName : groupsToOpen) {
                                        final ArrayList<String> groupList = groupMap.get(gName);
                                        groupQuestions.put(gName, clientCommandParts[0]);
                                        displayGroupString += gName + PushServer.COLON_SEPARATOR + groupList.size() + PushServer.COMMA_SEPARATOR;
                                        for (int j = 0; j < groupList.size(); j++) {
                                            clientMap.get(groupList.get(j)).startQuestion(clientCommandParts[0]);
                                        }
                                    }
                                }
                                for (int i = 0; i < displays.size(); i++) {
                                    displays.get(i).sendMessage(clientCommandParts[0] + PushServer.AMPERSAND_SEPARATOR + clientCommandParts[1] + PushServer.AMPERSAND_SEPARATOR + displayGroupString);
                                }
                            } else if (commandParts[0].equals(Constants.CLOSE)) {
                                for (int i = 0; i < displays.size(); i++) {
                                    displays.get(i).sendMessage(directiveParts[1]);
                                }
                                final String[] groupsToClose = commandParts[1].split(PushServer.COMMA_SEPARATOR);
                                if ((groupsToClose.length == 1) && groupsToClose[0].equals(Constants.EVERYONE)) {
                                    questionForEveryone = "";
                                    final Iterator<Map.Entry<String, Client>> clientMapIterator = clientMap.entrySet().iterator();
                                    while (clientMapIterator.hasNext()) {
                                        clientMapIterator.next().getValue().stopQuestion();
                                    }
                                } else {
                                    for (final String groupName : groupsToClose) {
                                        final ArrayList<String> groupList = groupMap.get(groupName);
                                        groupQuestions.put(groupName, "");
                                        // check 304
                                        for (int j = 0; j < groupList.size(); j++) {
                                            clientMap.get(groupList.get(j)).stopQuestion();
                                        }
                                    }
                                }
                            }
                        } else if (directiveParts[0].equals(Constants.GET_QUESTION_SETS)) {
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
                        } else if (directiveParts[0].equals(Constants.ADD_QUESTION_SET)) {
                            server.addQuestionSet(directiveParts[1], directiveParts[2]);
                        } else if (directiveParts[0].equals(Constants.DELETE_QUESTION_SET)) {
                            if (directiveParts.length > 1) {
                                server.deleteQuestionSet(directiveParts[1]);
                            }
                        } else if (directiveParts[0].equals(Constants.GET_ALL_QUESTIONS)) {
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
                        } else if (directiveParts[0].equals(Constants.GET_CLIENT_LIST)) {
                            final Iterator<String> iter = groupMap.keySet().iterator();
                            String output = "";
                            while (iter.hasNext()) {
                                final String groupName = iter.next();
                                output = output + groupName + PushServer.SEMI_COLON_SEPARATOR;
                                final ArrayList<String> groupMembers = groupMap.get(groupName);
                                for (int i = 0; i < groupMembers.size(); i++) {
                                    if (i != 0) {
                                        output = output + PushServer.COMMA_SEPARATOR;
                                    }
                                    output = output + groupMembers.get(i);
                                }
                                output = output + PushServer.AMPERSAND_SEPARATOR;
                            }
                            final Iterator<Map.Entry<String, Client>> clientIterator = clientMap.entrySet().iterator();
                            String notGrouped = Constants.NOT_GROUPED + PushServer.SEMI_COLON_SEPARATOR;
                            while (clientIterator.hasNext()) {
                                final Map.Entry<String, Client> next = clientIterator.next();
                                if (next.getValue().getGroup().equals("")) {
                                    notGrouped += next.getKey() + PushServer.SEMI_COLON_SEPARATOR;
                                }
                            }
                            notGrouped += PushServer.AMPERSAND_SEPARATOR;
                            output = Constants.CLIENT_LIST + PushServer.GRAVE_SEPARATOR + notGrouped + output;
                            output = output.substring(0, output.length() - 3);
                            System.out.println("Returning: " + output);
                            sendMessage(output);
                        } else if (directiveParts[0].equals(Constants.UPDATE_CLIENT_LIST)) {
                            groupMap.clear();
                            final String[] groupStrings = directiveParts[1].split(PushServer.AMPERSAND_SEPARATOR);
                            for (final String groupString : groupStrings) {
                                final String[] groupParts = groupString.split(PushServer.SEMI_COLON_SEPARATOR);
                                if (groupParts[0].equals(Constants.NOT_GROUPED)) {
                                    if (groupParts.length > 1) {
                                        final String[] nogroupClients = groupParts[1].split(PushServer.COMMA_SEPARATOR);
                                        for (final String nogroupClient : nogroupClients) {
                                            clientMap.get(nogroupClient).unsetGroup();
                                        }
                                    }
                                } else {
                                    final ArrayList<String> newGroupListing = new ArrayList<String>();
                                    if (groupParts.length > 1) {
                                        final String[] newMembers = groupParts[1].split(PushServer.COMMA_SEPARATOR);
                                        Collections.addAll(newGroupListing, newMembers);
                                        for (final String newMember : newMembers) {
                                            clientMap.get(newMember).setGroup(groupParts[0]);
                                        }
                                    }
                                    groupMap.put(groupParts[0], newGroupListing);
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
        
        /**
         * Unpause.
         */
        public void unpause() {
            paused = false;
            System.out.println("Admin just got unpaused " + System.currentTimeMillis());
        }
        
        /**
         * Pause.
         */
        public void pause() {
            paused = true;
            timer.cancel();
            System.out.println("Just paused admin " + System.currentTimeMillis());
        }
        
        /**
         * Checks if is paused.
         * 
         * @return true, if is paused
         */
        public boolean isPaused() {
            return paused;
        }
    }
    
}
