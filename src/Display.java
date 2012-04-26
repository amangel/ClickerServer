import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

// TODO: Auto-generated Javadoc
/**
 * The Class Display.
 */
public class Display {
    
    

    private static final int heartbeatTime = 32;
    /** The out. */
    private final PrintWriter out;
    
    /** The i consume. */
    private String iConsume;
    
    /**
     * Instantiates a new display.
     * 
     * @param id
     *            the id
     * @param socket
     *            the socket
     * @param in
     *            the in
     * @param out
     *            the out
     * @param admin
     *            the admin
     * @param name
     *            the name
     */
    public Display(final int id,
            final Socket socket,
            final BufferedReader in,
            final PrintWriter out,
            final PushAdmin admin,
            final String name) {
        this.out = out;
        boolean ctRead = false;
        while (!ctRead) {
            try {
                final String consumeInfo = in.readLine();
                ctRead = true;
                final String[] consumeParts = consumeInfo.split(PushServer.SEMI_COLON_SEPARATOR);
                if (consumeParts[0].equals(Constants.I_CONSUME)) {
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
    
    /**
     * Send message.
     * 
     * @param msg
     *            the msg
     */
    public void sendMessage(final String msg) {
        out.println(msg);
        out.flush();
    }
    
    /**
     * Gets the i consume.
     * 
     * @return the i consume
     */
    public String getIConsume() {
        return iConsume;
    }
}
