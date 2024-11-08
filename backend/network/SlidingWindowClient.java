package backend.network;
import java.io.*;
import java.net.*;
import java.util.*;
import java.math.BigInteger;
import java.util.concurrent.*;

import backend.security.AES;
import backend.security.DiffieHellman;
import backend.security.SecureNetworkMessage;

import java.nio.charset.StandardCharsets;
public class SlidingWindowClient implements AutoCloseable {
    private final Socket peerSocket;
    private final int windowSize;
    private final BitSet window;
    private final Queue<String> messageQueue;
    private final ObjectOutputStream objOut;
    private final ObjectInputStream objIn;
    private final AES aesEncryption;
    private final DiffieHellman diffieHellman;
    private volatile boolean isRunning;
    private volatile int base;
    private volatile int nextSeqNum;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<Integer, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> retransmissionCount = new ConcurrentHashMap<>();
    
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    // Default constructor that retrieves IP and Port using NetworkConfiguration
    public SlidingWindowClient() throws IOException {
        this(NetworkConfiguration.getLocalNetworkInfo().getIpAddress(), NetworkConfiguration.getLocalNetworkInfo().getPort(), 4);
    }

    public SlidingWindowClient(String peerAddress, int peerPort, int windowSize) throws IOException {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be positive");
        }

        this.windowSize = windowSize;
        this.window = new BitSet(windowSize);
        this.messageQueue = new LinkedList<>();
        
        // Initialize DiffieHellman for key exchange
        try {
            diffieHellman = new DiffieHellman();
        } catch (Exception e) {
            throw new IOException("Error initializing DiffieHellman", e);
        }

        this.peerSocket = new Socket(peerAddress, peerPort);
        this.objOut = new ObjectOutputStream(peerSocket.getOutputStream());
        this.objIn = new ObjectInputStream(peerSocket.getInputStream());

        // Perform key exchange
        this.aesEncryption = performKeyExchange();
        this.isRunning = true;
    }

    private AES performKeyExchange() throws IOException {
        try {
            SecureNetworkMessage clientKeyMessage = new SecureNetworkMessage(
                SecureNetworkMessage.MessageType.DH_PUBLIC_KEY,
                diffieHellman.getPublicKey()
            );
            objOut.writeObject(clientKeyMessage);
            objOut.flush();

            SecureNetworkMessage serverKeyMessage = (SecureNetworkMessage) objIn.readObject();
            if (serverKeyMessage.getType() != SecureNetworkMessage.MessageType.DH_PUBLIC_KEY) {
                throw new IOException("Unexpected message type during key exchange");
            }

            BigInteger serverPublicKey = serverKeyMessage.getDhPublicKey();
            byte[] sharedKey = diffieHellman.generateSharedSecret(serverPublicKey);
            AES aes = new AES(sharedKey);

            byte[] encryptedConfirmation;
            try {
                encryptedConfirmation = diffieHellman.confirmKeyExchange(serverPublicKey);
            } catch (Exception e) {
                throw new IOException("Error during key exchange confirmation", e);
            }
            
            SecureNetworkMessage confirmationMessage = new SecureNetworkMessage(
                SecureNetworkMessage.MessageType.CONFIRM,
                encryptedConfirmation,
                0
            );
            objOut.writeObject(confirmationMessage);
            objOut.flush();

            SecureNetworkMessage serverConfirmation = (SecureNetworkMessage) objIn.readObject();
            if (serverConfirmation.getType() != SecureNetworkMessage.MessageType.CONFIRM_RESPONSE) {
                throw new IOException("Invalid server confirmation");
            }

            boolean confirmed = (serverConfirmation.getEncryptedData()[0] == 1);
            if (!confirmed) {
                throw new IOException("Key exchange confirmation failed");
            }

            return aes;
        } catch (ClassNotFoundException e) {
            throw new IOException("Error during key exchange", e);
        }
    }

    public void sendMessage(String message) {
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        if (!isRunning) {
            throw new IllegalStateException("Client is not running");
        }

        messageQueue.offer(message);
        trySendNextMessage();
    }

    private synchronized void trySendNextMessage() {
        while (nextSeqNum < base + windowSize && !messageQueue.isEmpty()) {
            String message = messageQueue.peek();
            if (message != null) {
                try {
                    byte[] encrypted = aesEncryption.encrypt(message.getBytes(StandardCharsets.UTF_8));
                    SecureNetworkMessage secureMsg = new SecureNetworkMessage(
                        SecureNetworkMessage.MessageType.DATA,
                        encrypted,
                        nextSeqNum
                    );
                    synchronized (objOut) {
                        objOut.writeObject(secureMsg);
                        objOut.flush();
                    }

                    System.out.println("Sent message with sequence number: " + nextSeqNum);
                    messageQueue.poll();
                    window.set(nextSeqNum % windowSize);
                    startTimer(nextSeqNum);
                    nextSeqNum++;
                } catch (IOException e) {
                    System.err.println("Failed to send message: " + e.getMessage());
                    // Don't remove from queue on failure
                    break;
                }
            }
        }
    }

    private void startTimer(int seqNum) {
        ScheduledFuture<?> existingTimer = timers.get(seqNum);
        if (existingTimer != null) {
            existingTimer.cancel(false);
        }

        ScheduledFuture<?> timer = scheduler.schedule(() -> {
            handleTimeout(seqNum);
        }, DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        timers.put(seqNum, timer);
    }

    private void handleTimeout(int seqNum) {
        int retries = retransmissionCount.getOrDefault(seqNum, 0);
        if (retries < 3) {
            System.out.println("Timeout for sequence number: " + seqNum + ", retry: " + (retries + 1));
            retransmissionCount.put(seqNum, retries + 1);
            synchronized (this) {
                // Only retransmit if the message hasn't been acknowledged
                if (seqNum >= base && seqNum < nextSeqNum && window.get(seqNum % windowSize)) {
                    messageQueue.offer("Retransmit | SeqNum: " + seqNum);
                    trySendNextMessage();
                }
            }
            startTimer(seqNum);  // Restart timer for next attempt
        } else {
            System.err.println("Maximum retransmissions reached for sequence number: " + seqNum);
            synchronized (this) {
                if (seqNum == base) {
                    // Move the window forward only if we're at the base
                    base = seqNum + 1;
                    window.clear(seqNum % windowSize);
                    trySendNextMessage();
                }
            }
        }
    }

    public void receiveMessages() {
        Thread receiverThread = new Thread(() -> {
            while (isRunning) {
                try {
                    SecureNetworkMessage message = (SecureNetworkMessage) objIn.readObject();
                    if (message != null) {
                        handleMessage(message);
                    }
                } catch (EOFException e) {
                    System.err.println("Connection closed by peer");
                    break;
                } catch (IOException | ClassNotFoundException e) {
                    if (isRunning) {
                        System.err.println("Error receiving message: " + e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }, "Receiver-Thread");
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    private void handleMessage(SecureNetworkMessage message) throws IOException {
        if (message == null) {
            throw new IOException("Received null message");
        }

        try {
            switch (message.getType()) {
                case ACK:
                    handleAck(message.getSequenceNumber());
                    break;
                case NACK:
                    handleNack(message.getSequenceNumber());
                    break;
                case DATA:
                    handleData(message);
                    // Send acknowledgment for received data
                    sendAcknowledgment(message.getSequenceNumber(), true);
                    break;
                default:
                    System.err.println("Unexpected message type: " + message.getType());
            }
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
            // If there's an error processing the message, send NACK
            if (message.getType() == SecureNetworkMessage.MessageType.DATA) {
                sendAcknowledgment(message.getSequenceNumber(), false);
            }
        }
    }

    private void sendAcknowledgment(int seqNum, boolean isAck) throws IOException {
        SecureNetworkMessage ackMessage = new SecureNetworkMessage(
            isAck ? SecureNetworkMessage.MessageType.ACK : SecureNetworkMessage.MessageType.NACK,
            new byte[0],  // No data needed for ACK/NACK
            seqNum
        );
        synchronized (objOut) {
            objOut.writeObject(ackMessage);
            objOut.flush();
        }
    }

    private synchronized void handleAck(int ackNum) {
        System.out.println("Received ACK for sequence number: " + ackNum);  // Add logging
        if (ackNum >= base && ackNum < base + windowSize) {
            window.clear(ackNum % windowSize);
            retransmissionCount.remove(ackNum);
            cancelTimer(ackNum);
            
            if (ackNum == base) {
                int oldBase = base;
                while (!window.get(base % windowSize) && base < nextSeqNum) {
                    base++;
                }
                System.out.println("Window base moved from " + oldBase + " to " + base);  // Add logging
            }
            trySendNextMessage();
        } else {
            System.out.println("Ignored ACK " + ackNum + " (outside window: " + base + " to " + (base + windowSize - 1) + ")");
        }
    }

    private void handleNack(int nackNum) {
        if (nackNum >= base && nackNum < nextSeqNum) {
            cancelTimer(nackNum);
            messageQueue.offer("Retransmit | SeqNum: " + nackNum);
            trySendNextMessage();
        }
    }

    private void handleData(SecureNetworkMessage message) throws IOException {
        byte[] decrypted = aesEncryption.decrypt(message.getEncryptedData());
        System.out.println("Received: " + new String(decrypted, StandardCharsets.UTF_8));
    }

    private void cancelTimer(int seqNum) {
        ScheduledFuture<?> timer = timers.remove(seqNum);
        if (timer != null) {
            timer.cancel(false);
        }
    }

    @Override
    public void close() {
        if (!isRunning) {
            return;
        }

        isRunning = false;
        scheduler.shutdownNow();

        for (ScheduledFuture<?> timer : timers.values()) {
            timer.cancel(false);
        }
        timers.clear();

        try {
            if (!peerSocket.isClosed()) {
                peerSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try (SlidingWindowClient client = new SlidingWindowClient()) {
            System.out.println("Client connected to IP: " + NetworkConfiguration.getLocalNetworkInfo().getIpAddress());
            // Additional client operations can go here
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
