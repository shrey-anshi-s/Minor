import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class SlidingWindowClient {
    private ServerSocket serverSocket;
    private Socket peerSocket;
    private DataOutputStream out;
    private DataInputStream in;
    private int windowSize;
    private BitSet window;
    private int base;
    private int nextSeqNum;
    private ScheduledExecutorService scheduler;
    private Map<Integer, ScheduledFuture<?>> timers;
    private int timeout = 5000;
    private Map<Integer, Integer> retransmissionCount;
    private final int maxRetransmissions = 3;
    private Queue<String> messageQueue;
    private boolean isRunning;

    public SlidingWindowClient(int localPort, String peerAddress, int peerPort, int windowSize) throws IOException {
        this.serverSocket = new ServerSocket(localPort);
        this.peerSocket = new Socket(peerAddress, peerPort);
        this.out = new DataOutputStream(peerSocket.getOutputStream());
        this.in = new DataInputStream(peerSocket.getInputStream());
        this.windowSize = windowSize;
        this.window = new BitSet(windowSize);
        this.base = 0;
        this.nextSeqNum = 0;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.timers = new ConcurrentHashMap<>();
        this.retransmissionCount = new ConcurrentHashMap<>();
        this.messageQueue = new ConcurrentLinkedQueue<>();
        this.isRunning = true;
    }

    public void sendMessage(String message) {
        messageQueue.offer(message);
        trySendNextMessage();
    }

    private void trySendNextMessage() {
        while (nextSeqNum < base + windowSize && !messageQueue.isEmpty()) {
            String message = messageQueue.poll();
            if (message != null) {
                String formattedMessage = message + " | SeqNum: " + nextSeqNum;
                try {
                    out.writeUTF(formattedMessage);
                    window.set(nextSeqNum % windowSize);
                    startTimer(nextSeqNum);
                    nextSeqNum++;
                } catch (IOException e) {
                    e.printStackTrace();
                    messageQueue.offer(message); 
                    break;
                }
            }
        }
    }

    private void startTimer(int seqNum) {
        ScheduledFuture<?> timer = scheduler.schedule(() -> {
            if (retransmissionCount.getOrDefault(seqNum, 0) < maxRetransmissions) {
                try {
                    String retransmitMessage = "Retransmit | SeqNum: " + seqNum;
                    out.writeUTF(retransmitMessage);
                    retransmissionCount.put(seqNum, retransmissionCount.getOrDefault(seqNum, 0) + 1);
                    startTimer(seqNum);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Giving up on packet with SeqNum: " + seqNum);
                base = seqNum + 1;
                trySendNextMessage();
            }
        }, timeout, TimeUnit.MILLISECONDS);
        timers.put(seqNum, timer);
    }

    public void listenForAcks() {
        new Thread(() -> {
            try {
                while (isRunning) {
                    String ack = in.readUTF();
                    if (ack.startsWith("ACK")) {
                        int ackNum = extractAckNum(ack);
                        if (ackNum >= base && ackNum < base + windowSize) {
                            window.clear(ackNum % windowSize);
                            retransmissionCount.remove(ackNum);
                            cancelTimer(ackNum);
                            if (ackNum == base) {
                                while (!window.get(base % windowSize) && base < nextSeqNum) {
                                    base++;
                                }
                            }
                            trySendNextMessage();
                        }
                    } else if (ack.startsWith("NACK")) {
                        int nackNum = extractAckNum(ack);
                        if (nackNum >= base && nackNum < nextSeqNum) {
                            cancelTimer(nackNum);
                            try {
                                String retransmitMessage = "Retransmit | SeqNum: " + nackNum;
                                out.writeUTF(retransmitMessage);
                                startTimer(nackNum);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void cancelTimer(int seqNum) {
        ScheduledFuture<?> timer = timers.remove(seqNum);
        if (timer != null) {
            timer.cancel(false);
        }
    }

    private int extractAckNum(String ack) {
        return Integer.parseInt(ack.split(":")[1].trim());
    }

    public void listenForMessages() {
        new Thread(() -> {
            try {
                while (isRunning) {
                    String message = in.readUTF();
                    System.out.println("Received message: " + message);
                    int seqNum = extractAckNum(message);
                    if (seqNum == base) {
                        out.writeUTF("ACK: " + seqNum);
                        base++;
                        while (base < nextSeqNum && !window.get(base % windowSize)) {
                            base++;
                        }
                    } else if (seqNum > base) {
                        out.writeUTF("NACK: " + (seqNum - 1));
                    } else {
                        out.writeUTF("ACK: " + seqNum);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void closeConnection() throws IOException {
        isRunning = false;
        scheduler.shutdown();
        peerSocket.close();
        serverSocket.close();
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter local port: ");
        int localPort = scanner.nextInt();
        scanner.nextLine(); 
        System.out.print("Enter peer IP address: ");
        String peerAddress = scanner.nextLine();
        System.out.print("Enter peer port: ");
        int peerPort = scanner.nextInt();
        scanner.nextLine(); 

        int windowSize = 4;

        SlidingWindowClient client = new SlidingWindowClient(localPort, peerAddress, peerPort, windowSize);

        client.listenForMessages();
        client.listenForAcks();

        System.out.println("Client started. Type messages to send (or 'exit' to quit):");
        while (true) {
            String message = scanner.nextLine();
            if ("exit".equalsIgnoreCase(message)) {
                break;
            }
            client.sendMessage(message);
        }

        client.closeConnection();
        scanner.close();
    }
}