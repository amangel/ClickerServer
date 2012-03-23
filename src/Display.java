import java.io.BufferedReader;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Display {
    
    private final int ID;
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private String iConsume;
    private final PushAdmin admin;
    
    public Display(final int id, final Socket socket, final BufferedReader in, final PrintWriter out, final PushAdmin admin) {
        ID = id;
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.admin = admin;
        boolean ctRead = false;
        while (!ctRead) {
            try {
                final String consumeInfo = in.readLine();
                ctRead = true;
                final String[] consumeParts = consumeInfo.split(PushServer.SEMI_COLON_SEPARATOR);
                if (consumeParts[0].equals("IConsume")) {
                    iConsume = consumeParts[1];
                } else {
                    System.out.println("Read in: " + consumeParts[0] + " instead of IConsume");
                }
            } catch (final SocketTimeoutException e) {
            } catch (final IOException e) {
                e.printStackTrace();
                break;
            }
        }
        if (!ctRead) {
            System.out.println("Failed to read consumption info");
        }
    }
    
    public void sendMessage(final String msg) {
        out.println(msg);
    }
    
    public String getIConsume() {
        return iConsume;
    }
    
    private class DCChecker implements Runnable {
        
        private final String openMessage;
        private final boolean open;
        
        public DCChecker(final String openMessage) {
            this.openMessage = openMessage;
            open = true;
        }
        
        @Override
        public void run() {
            while (true) {
                try {
                    final String response = in.readLine();
                    if (response == null) {
                        admin.displayDied(ID);
                        break;
                    }
                } catch (final InterruptedIOException e) {
                } catch (final IOException e) {
                    admin.displayDied(ID);
                    break;
                }
            }
            
        }
    }
    
}
