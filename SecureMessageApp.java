import backend.network.NetworkConfiguration;
import backend.network.SlidingWindowClient;
import backend.network.SlidingWindowServer;

public class SecureMessageApp {
    public static void main(String[] args) {
        try {
            // Obtain local network information
            NetworkConfiguration.NetworkInfo networkInfo = NetworkConfiguration.getLocalNetworkInfo();
            System.out.println("Local IP: " + networkInfo.getIpAddress());
            System.out.println("Port: " + networkInfo.getPort());
            
            // Start the server in a new thread
            Thread serverThread = new Thread(() -> {
                try (SlidingWindowServer server = new SlidingWindowServer(4)) {
                    server.start();
                } catch (Exception e) {
                    System.err.println("Server error: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();
            
            // Increase server startup wait time
            Thread.sleep(2000);
            
            // Start the client to send and receive messages
            try (SlidingWindowClient client = new SlidingWindowClient(networkInfo.getIpAddress(), networkInfo.getPort(), 4)) {
                client.receiveMessages();
                
                // Add delay between connection and first message
                Thread.sleep(1000);
                
                client.sendMessage("Hello, this is a test message!");
                Thread.sleep(1000);  // Increase delay between messages
                client.sendMessage("Second test message");
                
                // Increase wait time for message processing
                Thread.sleep(10000);
            }
            
        } catch (Exception e) {
            System.err.println("Application error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
