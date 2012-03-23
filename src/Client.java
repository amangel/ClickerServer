import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

public class Client {
    
    private static final int heartbeatTime = 32;
    
    private final String ID;
    private final String macAddress;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final String currentAnswer;
    private final PushAdmin admin;
    private CommandHandler commandHandler;
    private String group;
    private Timer timer;
    
    public Client(final String ID, final String macAddress, final Socket socket, final BufferedReader in, final PrintWriter out, final PushAdmin admin) {
        this.ID = ID;
        this.macAddress = macAddress;
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.admin = admin;
        currentAnswer = " ";
        group = "";
        commandHandler = new CommandHandler();
        timer = new Timer();
        new Thread(commandHandler).start();
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
    
    public void clientReconnected(final Socket newSocket, final BufferedReader newIn, final PrintWriter newOut) {
        socket = newSocket;
        in = newIn;
        out = newOut;
        commandHandler = new CommandHandler();
        timer = new Timer();
        new Thread(commandHandler).start();
        timer.schedule(new PauseTask(), heartbeatTime * 1000);
    }
    
    public void setSocket(final Socket socket) {
        this.socket = socket;
    }
    
    public void setGroup(final String groupName) {
        group = groupName;
        out.println("System" + PushServer.SEMI_COLON_SEPARATOR + "GROUP" + PushServer.SEMI_COLON_SEPARATOR + groupName);
    }
    
    public void unsetGroup() {
        group = "";
        out.println("System" + PushServer.SEMI_COLON_SEPARATOR + "GROUP" + PushServer.SEMI_COLON_SEPARATOR + "Ungrouped");
    }
    
    public boolean isPaused() {
        return commandHandler.isPaused();
    }
    
    public void startQuestion(final String openMessage) {
        out.println(openMessage);
    }
    
    public void stopQuestion() {
        out.println("Close");
    }
    
    private class PauseTask extends TimerTask {
        
        @Override
        public void run() {
            commandHandler.pause();
            System.out.println("No heartbeat from client " + ID + ", pausing now");
        }
    }
    
    private class CommandHandler implements Runnable {
        
        private boolean paused = false;
        
        @Override
        public void run() {
            while (!paused) {
                try {
                    final String command = in.readLine();
                    if (command == null) {
                        pause();
                        // start a paused timer and when it expires call client
                        // died?
                        System.out.println("Client " + ID + " is paused");
                    } else {
                        if (command.equals("AreYouStillThere")) {
                            timer.cancel();
                            timer = new Timer();
                            timer.schedule(new PauseTask(), heartbeatTime * 1000);
                            out.println("YesImHere");
                            out.flush();
                            System.out.println("Received heartbeat from " + ID + " and reset timer");
                        } else {
                            admin.updateAnswer(ID + PushServer.SEMI_COLON_SEPARATOR + command);
                        }
                    }
                } catch (final SocketTimeoutException e) {
                } catch (final IOException e) {
                    pause();
                    System.out.println(e.getMessage());
                    System.out.println("Client " + ID + " is paused");
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
}
