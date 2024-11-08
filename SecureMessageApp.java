import backend.network.NetworkConfiguration;
import backend.network.SlidingWindowClient;
import backend.network.SlidingWindowServer;
import java.util.Scanner;

public class SecureMessageApp {
    private static final String DIVIDER = "\n" + "=".repeat(50) + "\n";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        NetworkConfiguration.NetworkInfo networkInfo = NetworkConfiguration.getLocalNetworkInfo();

        System.out.println(DIVIDER);
        System.out.println("Secure Message Application");
        System.out.println("Local IP: " + networkInfo.getIpAddress());
        System.out.println("Port: " + networkInfo.getPort());
        System.out.println(DIVIDER);

        System.out.println("Select test mode:");
        System.out.println("1. Start Server");
        System.out.println("2. Start Client");
        System.out.println("3. Start Both (local test)");
        System.out.print("\nEnter choice (1-3): ");

        int choice = scanner.nextInt();
        scanner.nextLine(); // consume newline

        try {
            switch (choice) {
                case 1:
                    runServer(networkInfo.getPort());
                    break;
                case 2:
                    runClient(scanner, networkInfo.getIpAddress(), networkInfo.getPort());
                    break;
                case 3:
                    runBothLocalTest(networkInfo);
                    break;
                default:
                    System.out.println("Invalid choice!");
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runServer(int port) throws Exception {
        System.out.println(DIVIDER);
        System.out.println("Starting server on port " + port);
        System.out.println("Press Ctrl+C to stop");
        System.out.println(DIVIDER);

        try (SlidingWindowServer server = new SlidingWindowServer(4)) {
            server.start();
        }
    }

    private static void runClient(Scanner scanner, String defaultIP, int defaultPort) throws Exception {
        System.out.println(DIVIDER);
        System.out.println("Starting client");
        System.out.print("Enter server IP [" + defaultIP + "]: ");
        String ip = scanner.nextLine().trim();
        if (ip.isEmpty()) ip = defaultIP;

        System.out.print("Enter server port [" + defaultPort + "]: ");
        String portStr = scanner.nextLine().trim();
        int port = portStr.isEmpty() ? defaultPort : Integer.parseInt(portStr);

        System.out.println(DIVIDER);
        System.out.println("Connecting to " + ip + ":" + port);
        System.out.println("Type messages to send (type 'exit' to quit)");
        System.out.println(DIVIDER);

        try (SlidingWindowClient client = new SlidingWindowClient(ip, port, 4)) {
            client.receiveMessages();

            while (true) {
                String message = scanner.nextLine();
                if (message.equalsIgnoreCase("exit")) break;
                client.sendMessage(message);
            }
        }
    }

    private static void runBothLocalTest(NetworkConfiguration.NetworkInfo networkInfo) throws Exception {
        // Start server in separate thread
        Thread serverThread = new Thread(() -> {
            try {
                runServer(networkInfo.getPort());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Wait for server to fully start
        Thread.sleep(2000);

        // Start client in the same application
        System.out.println("\nStarting client to connect to local server...");
        Scanner scanner = new Scanner(System.in);
        runClient(scanner, networkInfo.getIpAddress(), networkInfo.getPort());
    }
}
