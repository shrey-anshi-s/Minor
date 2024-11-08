package backend.network;
import java.net.*;
import java.util.*;

public class NetworkConfiguration {
    private static final int DEFAULT_PORT = 8888;
    private static final int PORT_RANGE_START = 8888;
    private static final int PORT_RANGE_END = 9999;
    
    public static class NetworkInfo {
        private final String ipAddress;
        private final int port;
        
        public NetworkInfo(String ipAddress, int port) {
            this.ipAddress = ipAddress;
            this.port = port;
        }
        
        public String getIpAddress() { return ipAddress; }
        public int getPort() { return port; }
    }
    
    public static NetworkInfo getLocalNetworkInfo() {
        String ipAddress = getLocalIpAddress();
        int port = findAvailablePort();
        return new NetworkInfo(ipAddress, port);
    }
    
    private static String getLocalIpAddress() {
        try {
            // Try to get WiFi/Ethernet IP first
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (!ip.startsWith("127.")) {
                            return ip;
                        }
                    }
                }
            }
            
            // Fallback to loopback if no other IP is found
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1"; // Default to localhost if there's an error
        }
    }
    
    private static int findAvailablePort() {
        for (int port = PORT_RANGE_START; port <= PORT_RANGE_END; port++) {
            try (ServerSocket socket = new ServerSocket(port)) {
                return port;
            } catch (Exception e) {
                continue;
            }
        }
        return DEFAULT_PORT;
    }
    
    public static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}