package backend.network;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class NetworkManager {
    private static final Logger LOGGER = Logger.getLogger(NetworkManager.class.getName());
    private static final int MAX_CLIENTS = 50;
    private final ConcurrentHashMap<String, SlidingWindowClient> clients;
    private final ConcurrentHashMap<String, ConnectionStatus> clientStatus;
    private final ScheduledExecutorService connectionMonitor;
    private final ConcurrentHashMap<String, MessageHandler> messageHandlers;

    public enum ConnectionStatus {
        CONNECTED,
        DISCONNECTED,
        RECONNECTING,
        PERMANENTLY_DISCONNECTED
    }

    public interface MessageHandler {
        void onMessageReceived(String clientId, String message);
        void onConnectionStatusChanged(String clientId, ConnectionStatus status);
    }

    public NetworkManager() {
        this.clients = new ConcurrentHashMap<>();
        this.clientStatus = new ConcurrentHashMap<>();
        this.messageHandlers = new ConcurrentHashMap<>();
        this.connectionMonitor = Executors.newScheduledThreadPool(1);
        startConnectionMonitoring();
    }

    private void startConnectionMonitoring() {
        connectionMonitor.scheduleAtFixedRate(() -> {
            clients.forEach((clientId, client) -> {
                if (clientStatus.get(clientId) == ConnectionStatus.DISCONNECTED) {
                    attemptReconnection(clientId, 3);  // Maximum 3 reconnection attempts
                }
            });
        }, 0, 30, TimeUnit.SECONDS);
    }

    public String connectToClient(String address, int port, MessageHandler handler) throws IOException {
        if (clients.size() >= MAX_CLIENTS) {
            throw new IOException("Maximum number of clients reached");
        }
        
        try {
            SlidingWindowClient client = new SlidingWindowClient(address, port, 4, message -> {
                String clientId = generateClientId(address, port);
                handler.onMessageReceived(clientId, message);
            });
            
            String clientId = generateClientId(address, port);
            clients.put(clientId, client);
            messageHandlers.put(clientId, handler);
            clientStatus.put(clientId, ConnectionStatus.CONNECTED);
            handler.onConnectionStatusChanged(clientId, ConnectionStatus.CONNECTED);
            
            client.receiveMessages();
            monitorClientConnection(clientId, client);
            return clientId;
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to client: " + e.getMessage(), e);
            throw new IOException("Failed to connect to client: " + e.getMessage(), e);
        }
    }

    public void broadcastMessage(String message) {
        clients.forEach((clientId, client) -> {
            try {
                if (clientStatus.get(clientId) == ConnectionStatus.CONNECTED) {
                    client.sendMessage(message);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to send message to client " + clientId, e);
                handleDisconnection(clientId);
            }
        });
    }

    public void sendMessageToClient(String clientId, String message) throws IOException {
        SlidingWindowClient client = clients.get(clientId);
        if (client != null && clientStatus.get(clientId) == ConnectionStatus.CONNECTED) {
            try {
                client.sendMessage(message);
            } catch (Exception e) {
                handleDisconnection(clientId);
                throw new IOException("Failed to send message: " + e.getMessage(), e);
            }
        } else {
            throw new IOException("Client not connected: " + clientId);
        }
    }

    public Set<String> getConnectedClients() {
        Set<String> connectedClients = new HashSet<>();
        clientStatus.forEach((clientId, status) -> {
            if (status == ConnectionStatus.CONNECTED) {
                connectedClients.add(clientId);
            }
        });
        return connectedClients;
    }

    public ConnectionStatus getClientStatus(String clientId) {
        return clientStatus.getOrDefault(clientId, ConnectionStatus.DISCONNECTED);
    }

    public void shutdown() {
        connectionMonitor.shutdownNow();
        clients.forEach((clientId, client) -> {
            try {
                client.close();
                MessageHandler handler = messageHandlers.get(clientId);
                if (handler != null) {
                    handler.onConnectionStatusChanged(clientId, ConnectionStatus.DISCONNECTED);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error closing client " + clientId, e);
            }
        });
        clients.clear();
        clientStatus.clear();
        messageHandlers.clear();
    }

    private String generateClientId(String address, int port) {
        return address + ":" + port;
    }

    private void notifyConnectionStatusChange(String clientId, ConnectionStatus status) {
        MessageHandler handler = messageHandlers.get(clientId);
        if (handler != null) {
            handler.onConnectionStatusChanged(clientId, status);
        }
        LOGGER.info("Client " + clientId + " status changed to: " + status);
    }

    private void monitorClientConnection(String clientId, SlidingWindowClient client) {
        Thread monitor = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted() && 
                       clientStatus.get(clientId) == ConnectionStatus.CONNECTED) {
                    if (!isClientConnected(client)) {
                        handleDisconnection(clientId);
                        break;
                    }
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        monitor.setDaemon(true);
        monitor.start();
    }

    private void handleDisconnection(String clientId) {
        clientStatus.put(clientId, ConnectionStatus.DISCONNECTED);
        notifyConnectionStatusChange(clientId, ConnectionStatus.DISCONNECTED);
        attemptReconnection(clientId, 3); // Specify max attempts here as well
    }

    private void attemptReconnection(String clientId, int maxAttempts) {
        SlidingWindowClient client = clients.get(clientId);
        if (client == null) return;
        
        clientStatus.put(clientId, ConnectionStatus.RECONNECTING);
        notifyConnectionStatusChange(clientId, ConnectionStatus.RECONNECTING);
        
        CompletableFuture.runAsync(() -> {
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    client.close();
                    String[] parts = clientId.split(":");
                    String address = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    
                    MessageHandler existingHandler = messageHandlers.get(clientId);
                    SlidingWindowClient newClient = new SlidingWindowClient(address, port, 4, message -> {
                    if (existingHandler != null) {
                        existingHandler.onMessageReceived(clientId, message);
                    }
                78});
                    
                    clients.put(clientId, newClient);
                    clientStatus.put(clientId, ConnectionStatus.CONNECTED);
                    notifyConnectionStatusChange(clientId, ConnectionStatus.CONNECTED);
                    newClient.receiveMessages();
                    return;
                    
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Reconnection attempt " + attempt + 
                               " failed for client " + clientId);
                    try {
                        TimeUnit.SECONDS.sleep(5); // Retry delay between attempts
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            clientStatus.put(clientId, ConnectionStatus.PERMANENTLY_DISCONNECTED);
            notifyConnectionStatusChange(clientId, ConnectionStatus.PERMANENTLY_DISCONNECTED);
        });
    }

    private boolean isClientConnected(SlidingWindowClient client) {
        try {
            client.sendMessage("PING");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}