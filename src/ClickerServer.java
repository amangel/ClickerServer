
public class ClickerServer {
    
    private static PushServer pushServer;
    
    public static void main(final String[] args) {
        pushServer = new PushServer(4321, 7700, 7171);

    }
}
