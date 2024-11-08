package backend.network;
import java.io.*;
import java.net.*;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.concurrent.*;

import backend.security.AES;
import backend.security.DiffieHellman;
import backend.security.SecureNetworkMessage;

public class SlidingWindowServer implements AutoCloseable {
    private final ServerSocket serverSocket;
    private final int windowSize;
    private final ExecutorService clientHandler;

    private volatile boolean isRunning;

    public SlidingWindowServer(int windowSize) throws IOException {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be positive");
        }

        // Use NetworkConfiguration to get local IP and available port
        NetworkConfiguration.NetworkInfo networkInfo = NetworkConfiguration.getLocalNetworkInfo();
        int port = networkInfo.getPort();
        this.serverSocket = new ServerSocket(port);
        this.windowSize = windowSize;
        this.clientHandler = Executors.newCachedThreadPool();

        System.out.println("Server started on IP: " + networkInfo.getIpAddress() + ", Port: " + port);
    }

    public void start() {
        isRunning = true;

        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, windowSize);
                clientHandler.execute(handler);
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (!isRunning) {
            return;
        }

        isRunning = false;
        clientHandler.shutdownNow();
        serverSocket.close();
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final int windowSize;
        private final ObjectInputStream in;
        private final ObjectOutputStream out;
        private final AES aesEncryption;
        private final DiffieHellman diffieHellman;
        private final BitSet receivedWindow;
        private volatile int expectedSeqNum;

        public ClientHandler(Socket socket, int windowSize) throws IOException {
            this.clientSocket = socket;
            this.windowSize = windowSize;
            this.receivedWindow = new BitSet(windowSize);

            // Initialize DiffieHellman for key exchange
            try {
                this.diffieHellman = new DiffieHellman();
                this.out = new ObjectOutputStream(socket.getOutputStream());
                this.in = new ObjectInputStream(socket.getInputStream());
                this.aesEncryption = performKeyExchange();
            } catch (Exception e) {
                closeQuietly(socket);
                throw new IOException("Error initializing DiffieHellman", e);
            }
        }

        private AES performKeyExchange() throws IOException {
            try {
                SecureNetworkMessage clientKeyMessage = (SecureNetworkMessage) in.readObject();
                BigInteger clientPublicKey = clientKeyMessage.getDhPublicKey();

                SecureNetworkMessage serverKeyMessage = new SecureNetworkMessage(
                    SecureNetworkMessage.MessageType.DH_PUBLIC_KEY,
                    diffieHellman.getPublicKey()
                );
                out.writeObject(serverKeyMessage);
                out.flush();

                byte[] sharedKey = diffieHellman.generateSharedSecret(clientPublicKey);
                AES aes = new AES(sharedKey);

                SecureNetworkMessage clientConfirmation = (SecureNetworkMessage) in.readObject();
                try {
                    boolean confirmed = diffieHellman.verifyConfirmation(clientPublicKey, clientConfirmation.getEncryptedData());
                    if (!confirmed) {
                        throw new IOException("Key exchange confirmation failed");
                    }
                } catch (Exception e) {
                    throw new IOException("Key exchange verification failed", e);
                }

                byte[] confirmationData = new byte[1];
                confirmationData[0] = (byte) 1;
                SecureNetworkMessage confirmationResponse = new SecureNetworkMessage(
                    SecureNetworkMessage.MessageType.CONFIRM_RESPONSE,
                    confirmationData,
                    0
                );
                out.writeObject(confirmationResponse);
                out.flush();

                return aes;
            } catch (ClassNotFoundException e) {
                throw new IOException("Error during key exchange", e);
            }
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted() && !clientSocket.isClosed()) {  // Add socket check
                    try {
                        SecureNetworkMessage message = (SecureNetworkMessage) in.readObject();
                        if (message != null) {
                            handleMessage(message);
                        }
                    } catch (EOFException e) {
                        System.out.println("Client disconnected normally");
                        break;
                    } catch (IOException | ClassNotFoundException e) {
                        if (!clientSocket.isClosed()) {
                            System.err.println("Error processing message: " + e.getMessage());
                        }
                        break;
                    }
                }
            } finally {
                closeQuietly(in);
                closeQuietly(out);
                closeQuietly(clientSocket);
            }
        }

        private void closeQuietly(Closeable closeable) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing resource: " + e.getMessage());
            }
        }

        private void handleMessage(SecureNetworkMessage message) throws IOException {
            switch (message.getType()) {
                case DATA:
                    byte[] decryptedData = aesEncryption.decrypt(message.getEncryptedData());
                    int seqNum = message.getSequenceNumber();

                    if (seqNum == expectedSeqNum) {
                        System.out.println("Received: " + new String(decryptedData));
                        sendAck(seqNum);
                        expectedSeqNum++;
                        while (receivedWindow.get((expectedSeqNum) % windowSize)) {
                            expectedSeqNum++;
                            receivedWindow.clear((expectedSeqNum - 1) % windowSize);
                        }
                    } else if (seqNum > expectedSeqNum) {
                        receivedWindow.set(seqNum % windowSize);
                        sendNack(expectedSeqNum - 1);
                    } else {
                        sendAck(seqNum);
                    }
                    break;
                default:
                    System.err.println("Unexpected message type: " + message.getType());
            }
        }

        private void sendAck(int seqNum) throws IOException {
            SecureNetworkMessage ack = new SecureNetworkMessage(
                SecureNetworkMessage.MessageType.ACK,
                new byte[0], 
                seqNum
            );
            synchronized(out) { 
                out.writeObject(ack);
                out.flush(); 
            }
        }
        
        private void sendNack(int seqNum) throws IOException {
            SecureNetworkMessage nack = new SecureNetworkMessage(
                SecureNetworkMessage.MessageType.NACK,
                new byte[0], 
                seqNum
            );
            synchronized(out) {  
                out.writeObject(nack);
                out.flush();
            }
        }
    }

    public static void main(String[] args) {
        try (SlidingWindowServer server = new SlidingWindowServer(4)) {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}