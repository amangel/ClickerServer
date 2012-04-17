// TODO: Auto-generated Javadoc
/**
 * The Class ClickerServer.
 */
public class ClickerServer {
    
    private static final int CLIENT_PORT = 4321;
    private static final int ADMIN_PORT = 7700;
    private static final int DISPLAY_PORT = 7171;
    
    /** The push server. */
    private static PushServer pushServer;
    
    /**
     * The main method.
     * 
     * @param args
     *            the arguments
     */
    public static void main(final String[] args) {
        pushServer = new PushServer(CLIENT_PORT, ADMIN_PORT, DISPLAY_PORT);
        
    }
}
