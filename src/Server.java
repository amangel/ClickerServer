import java.io.IOException;
import java.net.ServerSocket;

// TODO: Auto-generated Javadoc
/**
 * The Class Server.
 */
abstract class Server {
    
    /** The server socket. */
    private ServerSocket serverSocket;
    
    /**
     * Instantiates a new server.
     * 
     * @param port
     *            the port
     */
    public Server(final int port) {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(100);
        } catch (final IOException e) {
            System.out.println("Could not listen on port " + port);
        }
    }
    
    /**
     * Server socket.
     * 
     * @return the server socket
     */
    public ServerSocket serverSocket() {
        return serverSocket;
    }
}
