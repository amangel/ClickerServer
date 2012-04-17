import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.sql.ConnectionEvent;

// TODO: Auto-generated Javadoc
/**
 * The Class PushServer.
 */
public class PushServer {
    


    /** Listens for incoming client connections. */
    private final ClientConnectionListener clientConnectionChecker;
    
    /** Handles new admin connections. */
    private final AdminListener adminListener;
    
    /** Handles new display connections. */
    private final DisplayListener displayListener;
    
    /** Map of the connected administrators. */
    private final Map<String, PushAdmin> admins;
    
    /** The question sets. <br>
     * Map of QuestionSetName to an ArrayList of Questions */
    private final Map<String, ArrayList<String>> questionSets;
    
    /** The questions. */
    private final Map<String, Question> questions;
    
    /** The server socket. */
    private ServerSocket serverSocket;
    
    // private Connection connect;
    // private Statement clientStatement;
    // private ResultSet clientResultSet;
    // private Statement adminStatement;
    // private ResultSet adminResultSet;
    
    /** Delimiter */
    public static String SEMI_COLON_SEPARATOR = "`/;";
    
    /** Delimiter */
    public static String COLON_SEPARATOR = "`/:";
    
    /** Delimiter */
    public static String GRAVE_SEPARATOR = "`/`";
    
    /** Delimiter */
    public static String AMPERSAND_SEPARATOR = "`/&";
    
    /** Delimiter */
    public static String AT_SEPARATOR = "`/@";
    
    /** Delimiter */
    public static String COMMA_SEPARATOR = "`/,";

    /**
     * Instantiates a new push server.
     * 
     * @param clientPort
     *            the client port
     * @param adminPort
     *            the admin port
     * @param displayPort
     *            the display port
     */
    public PushServer(final int clientPort, final int adminPort, final int displayPort) {
        try {
            serverSocket = new ServerSocket(clientPort);
            serverSocket.setSoTimeout(100);
        } catch (final IOException e) {
            System.out.println("Could not listen on port " + clientPort);
        }
        admins = Collections.synchronizedMap(new HashMap<String, PushAdmin>(50));
        admins.put("frederis", new PushAdmin("frederis", this));
        adminListener = new AdminListener(adminPort);
        new Thread(adminListener).start();
        clientConnectionChecker = new ClientConnectionListener();
        new Thread(clientConnectionChecker).start();
        displayListener = new DisplayListener(displayPort);
        new Thread(displayListener).start();
        
        questions = Collections.synchronizedMap(new HashMap<String, Question>());
        questionSets = Collections.synchronizedMap(new HashMap<String, ArrayList<String>>());
        
        // connectToSQLDatabase();
        
        // clientStatement = null;
        // clientResultSet = null;
        // adminStatement = null;
        // adminResultSet = null;
        
        loadQuestionsFromFile();
    }
    
    /**
     * Load questions from the questions/questions.txt file one line at a time.
     */
    private void loadQuestionsFromFile() {
        try {
            final BufferedReader qIn = new BufferedReader(new FileReader("questions/questions.txt"));
            String str;
            while ((str = qIn.readLine()) != null) {
                parseQuestionSet(str);
            }
            qIn.close();
        } catch (final IOException e) {
            System.out.println("Could not open question file");
        }
    }
    
    /**
     * Parse question sets from each line of the questions/questions.txt file.
     * 
     * @param setString
     *            the set string
     */
    private void parseQuestionSet(final String setString) {
        System.out.println("reading a question set");
        System.out.println(setString);
        final String[] setParts = setString.split(AT_SEPARATOR);
        System.out.println(setParts[0]);
        final ArrayList<String> questionList = new ArrayList<String>();
        if (setParts.length > 1) {
            if (!setParts[1].equals("")) {
                final String[] setQuestions = setParts[1].split(AMPERSAND_SEPARATOR);
                Question newQuestion = new Question(setQuestions[0]);
                String previous = newQuestion.getQuestionId();
                questions.put(previous, newQuestion);
                questionList.add(previous);
                if (setQuestions.length > 1) {
                    for (int i = 1; i < setQuestions.length; i++) {
                        newQuestion = new Question(setQuestions[i]);
                        final String newID = newQuestion.getQuestionId();
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
    
    /**
     * Adds the question set.
     * 
     * @param oldName
     *            the old name
     * @param qSetString
     *            the question set string
     */
    public void addQuestionSet(final String oldName, final String qSetString) {
        if (!oldName.equals("")) {
            questionSets.remove(oldName);
        }
        parseQuestionSet(qSetString);
        saveQuestionSetsToFile();
    }
    
    /**
     * Delete question set.
     * 
     * @param questionSet
     *            the old name
     */
    public void deleteQuestionSet(final String questionSet) {
        questionSets.remove(questionSet);
        saveQuestionSetsToFile();
    }
    
    /**
     * Save question sets.
     */
    public void saveQuestionSetsToFile() {
        try {
            final BufferedWriter fOutput = new BufferedWriter(new FileWriter("questions/questions.txt"));
            final Iterator<String> qsIter = questionSets.keySet().iterator();
            while (qsIter.hasNext()) {
                final String nextQS = qsIter.next();
                String qsLine = nextQS + AT_SEPARATOR;
                final ArrayList<String> setQuestions = questionSets.get(nextQS);
                if (setQuestions.size() > 0) {
                    qsLine += questions.get(setQuestions.get(0)).getQuestionString();
                    for (int i = 1; i < setQuestions.size(); i++) {
                        qsLine += AMPERSAND_SEPARATOR + questions.get(setQuestions.get(i)).getQuestionString();
                    }
                }
                fOutput.write(qsLine + "\n");
            }
            fOutput.close();
        } catch (final IOException e) {
            System.out.println("Could not save questions");
        }
    }
    
    /*
     * private void connectToSQLDatabase(){ try {
     * Class.forName("com.mysql.jdbc.Driver"); connect =
     * DriverManager.getConnection("jdbc:mysql://localhost/clicker?" +
     * "autoReconnect=true&user=clickeruser&password=clickerpassword"); } catch
     * (Exception e) { e.printStackTrace(); } }
     */
    
    /*
     * public synchronized ResultSet runQuery(String query) { try { Statement
     * qStatement = connect.createStatement(); return
     * qStatement.executeQuery(query); } catch (Exception e)
     * {e.printStackTrace();} return null; }
     */
    
    /**
     * Gets the push server.
     * 
     * @return the push server
     */
    public PushServer getPushServer() {
        return this;
    }
    
    /**
     * Gets the question sets.
     * 
     * @return the question sets
     */
    public String getQuestionSets() {
        final Iterator<String> questionSetItererator = questionSets.keySet().iterator();
        final String[] resultArray = new String[questionSets.size()];
        String finalResult = "";
        int index = 0;
        while (questionSetItererator.hasNext()) {
            resultArray[index] = questionSetItererator.next();
            index++;
            // finalResult = finalResult + qSetIter.next() +
            // AMPERSAND_SEPARATOR;
        }
        java.util.Arrays.sort(resultArray);
        for (final String questionString : resultArray) {
            finalResult = finalResult + questionString + AMPERSAND_SEPARATOR;
        }
        return Constants.ALL_SETS + GRAVE_SEPARATOR + finalResult;
    }
    
    /**
     * Gets the questions in set.
     * 
     * @param questionSetKey
     *            the q set key
     * @return the questions in set
     */
    public String getQuestionsInSet(final String questionSetKey) {
        System.out.println("Getting questions for set: " + questionSetKey);
        final ArrayList<String> setQuestions = questionSets.get(questionSetKey);
        String finalResult = "";
        for (int i = 0; i < setQuestions.size(); i++) {
            System.out.println("In a loop");
            final Question nextQuestion = questions.get(setQuestions.get(i));
            finalResult = finalResult + nextQuestion.getQuestionId() + SEMI_COLON_SEPARATOR + 
                    nextQuestion.getQuestionFlags() + SEMI_COLON_SEPARATOR + nextQuestion.getWidgets() + 
                    SEMI_COLON_SEPARATOR + nextQuestion.getBackgroundColor() + AMPERSAND_SEPARATOR;
        }
        System.out.println("Finalresult: " + finalResult);
        return Constants.QUESTION_SET + GRAVE_SEPARATOR + finalResult;
    }
    
    /**
     *  This class listens for incoming client connections.
     *  It runs as a thread and accepts and processes new connections, 
     *  waiting for a new connection as soon as the old connection has been processed.
     * 
     * @see ConnectionEvent
     */
    private class ClientConnectionListener implements Runnable {
        


        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            System.out.println("Listening for clients now on PushServer");
            while (true) {
                try {
                    final Socket client = serverSocket.accept();
                    System.out.println("accepted a client connection");
                    client.setKeepAlive(true);
                    client.setSoTimeout(100);
                    final BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    final PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                    String idString = "";
                    boolean isIdSet = false;
                    while (!isIdSet) {
                        try {
                            idString = in.readLine();
                            System.out.println("id is set");
                            isIdSet = true;
                        } catch (final InterruptedIOException e) {
                        }
                    }
                    final String[] idparts = idString.split(SEMI_COLON_SEPARATOR);
                    if (idparts.length == 3) {
                        System.out.println("length is 3");
                        final String clientId = idparts[0];
                        final String clientMac = idparts[1];
                        final String[] adminParts = idparts[2].split(COMMA_SEPARATOR);
                        final String adminID = adminParts[0];
                        final String groupID = adminParts[1];
                        System.out.println("Client is wanting to connect to admin: " + adminID);
                        if (admins.containsKey(adminID)) {
                            System.out.println("Calling processClient");
                            admins.get(adminID).processClient(client, clientId, clientMac, in, out, groupID);
                        } else {
                            out.println(Constants.INVALID_ADMIN);
                            client.close();
                        }
                    } else if (idparts.length == 4) {
                        /*
                         * try { clientStatement = connect.createStatement();
                         * String query =
                         * "SELECT * FROM questions WHERE questionid='" +
                         * idparts[3] + "' AND admin='" + idparts[2] + "'";
                         * clientResultSet =
                         * clientStatement.executeQuery(query); while
                         * (clientResultSet.next()) { String qString =
                         * clientResultSet.getString("questionstring");
                         * out.println("Open;" + idparts[3] + ";" + qString);
                         * System.out.println("Open;" + idparts[3] + ";" +
                         * qString); } } catch (Exception e)
                         * {e.printStackTrace();}
                         */
                        final Question requested = questions.get(idparts[3]);
                        out.println(Constants.OPEN + SEMI_COLON_SEPARATOR + idparts[3] + SEMI_COLON_SEPARATOR + 
                                requested.getQuestionFlags() + SEMI_COLON_SEPARATOR + requested.getWidgets() + 
                                SEMI_COLON_SEPARATOR + requested.getBackgroundColor());
                    } else if (idparts.length == 5) {
                        /*
                         * System.out.println("Processing response now"); try {
                         * clientStatement = connect.createStatement();
                         * System.out
                         * .println("Looking for followups for question " +
                         * idparts[3]); String query =
                         * "SELECT * FROM questions WHERE questionid=(SELECT followup FROM questions WHERE questionid='"
                         * + idparts[3] + "' AND admin='" + idparts[2] + "')";
                         * clientResultSet =
                         * clientStatement.executeQuery(query); if
                         * (clientResultSet.next()) {
                         * System.out.println("Opening another question");
                         * String qString =
                         * clientResultSet.getString("questionstring");
                         * out.println("Open;" +
                         * clientResultSet.getString("questionid") + ";" +
                         * qString); } else {
                         * System.out.println("No more followups");
                         * out.println("Close;"); } } catch (Exception e)
                         * {e.printStackTrace();}
                         */
                        final Question previous = questions.get(idparts[3]);
                        if (previous.hasFollowUp()) {
                            final Question next = questions.get(previous.getFollowUp());
                            out.println(Constants.OPEN + SEMI_COLON_SEPARATOR + next.getQuestionId() + 
                                    SEMI_COLON_SEPARATOR + next.getQuestionFlags() + SEMI_COLON_SEPARATOR + 
                                    next.getWidgets() + SEMI_COLON_SEPARATOR + next.getBackgroundColor());
                            
                        } else {
                            out.println(Constants.CLOSE + SEMI_COLON_SEPARATOR);
                        }
                    } else {
                        // invalid
                    }
                } catch (final Exception e) {
                }
            }
        }
    }
    
    /**
     * This class listens for incoming administrator connections.
     * If there is no instance of an administrator that has previously connected, 
     * it will create a new administrator.
     * If the admin is reconnecting, it simply updates the old PushAdmin object 
     * with the new connection.
     * 
     * @see AdminEvent
     */
    private class AdminListener implements Runnable {
        
        /** The connection socket. */
        private ServerSocket connectionSocket;
        
        /**
         * Instantiates a new admin listener.
         * 
         * @param port
         *            the port
         */
        public AdminListener(final int port) {
            try {
                connectionSocket = new ServerSocket(port);
            } catch (final IOException e) {
                System.out.println("Could not listen on port " + port);
            }
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            while (true) {
                String userName = "";
                String password = "";
                Socket adminSocket = null;
                BufferedReader adminIn = null;
                PrintWriter adminOut = null;
                
                try {
                    System.out.println("Waiting for next admin connected on push server");
                    adminSocket = connectionSocket.accept();
                    adminSocket.setKeepAlive(true);
                    System.out.println("Admin connected, authenticating now");
                    adminIn = new BufferedReader(new InputStreamReader(adminSocket.getInputStream()));
                    adminOut = new PrintWriter(adminSocket.getOutputStream(), true);
                    userName = adminIn.readLine();
                    password = adminIn.readLine();
                    System.out.println("Admin authenticated");
                } catch (final IOException e) {
                }
                /*
                 * try{ adminStatement = connect.createStatement(); } catch
                 * (com.mysql.jdbc.exceptions.jdbc4.CommunicationsException e){
                 * connectToSQLDatabase(); try { adminStatement =
                 * connect.createStatement(); } catch (Exception derp) {} }
                 * catch (Exception e){} try { String query =
                 * "SELECT * FROM users WHERE username='" + userName +
                 * "' AND password='" + password + "'"; adminResultSet =
                 * adminStatement.executeQuery(query); if
                 * (adminResultSet.next()) { if (admins.containsKey(userName)) {
                 * System.out.println("Already have admin key"); PushAdmin
                 * oldAdmin = admins.get(userName);
                 * System.out.println("Calling admin reconnected");
                 * oldAdmin.adminReconnected(adminSocket, adminIn, adminOut); }
                 * else { admins.put(userName, new PushAdmin(userName,
                 * adminSocket, adminIn, adminOut, getPushServer())); } } }
                 * catch (Exception e) {e.printStackTrace();}
                 */
                if (userName.equals("frederis") && password.equals("testpw")) {
                    if (admins.containsKey(userName)) {
                        System.out.println("Already have admin key ");
                        final PushAdmin oldAdmin = admins.get(userName);
                        System.out.println("Calling admin reconnected ");
                        oldAdmin.reconnectAdmin(adminSocket, adminIn, adminOut);
                    } else {
                        admins.put(userName, new PushAdmin(userName, getPushServer()));
                        admins.get(userName).connectAdmin(adminSocket, adminIn, adminOut);
                    }
                }
            }
        }
    }
    
    /**
     * This class listens for new connections coming in on the display's port.
     * 
     * @see DisplayEvent
     */
    private class DisplayListener implements Runnable {
        
        /** The display socket. */
        private ServerSocket displaySocket;
        
        /**
         * Instantiates a new display listener.
         * 
         * @param port
         *            the port
         */
        public DisplayListener(final int port) {
            try {
                displaySocket = new ServerSocket(port);
            } catch (final IOException e) {
                System.out.println("Could not listen on port " + port);
            }
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            while (true) {
                try {
                    System.out.println("Waiting for display on push server");
                    final Socket display = displaySocket.accept();
                    display.setKeepAlive(true);
                    display.setSoTimeout(100);
                    final BufferedReader displayIn = new BufferedReader(new InputStreamReader(display.getInputStream()));
                    final PrintWriter displayOut = new PrintWriter(display.getOutputStream(), true);
                    final String adminAndName = displayIn.readLine();
                    // final String requestedAdmin = displayIn.readLine();
                    final String[] adminAndNameParts = adminAndName.split(COMMA_SEPARATOR);
                    final String requestedAdmin = adminAndNameParts[0];
                    final String displayName = adminAndNameParts[1];
                    if (admins.containsKey(requestedAdmin)) {
                        admins.get(requestedAdmin).processDisplay(display,
                                displayIn,
                                displayOut,
                                displayName);
                    } else {
                        admins.put(requestedAdmin, new PushAdmin(requestedAdmin, getPushServer()));
                        System.out.println("Display connected and waiting for admin on " + requestedAdmin);
                    }
                } catch (final IOException e) {
                }
            }
        }
    }
    
}
