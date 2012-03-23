import java.io.IOException;
import java.net.ServerSocket;

abstract class Server {
    
    private ServerSocket serverSocket;
    
    public Server(final int port) {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(100);
        } catch (final IOException e) {
            System.out.println("Could not listen on port " + port);
        }
    }
    
    public ServerSocket serverSocket() {
        return serverSocket;
    }
}
