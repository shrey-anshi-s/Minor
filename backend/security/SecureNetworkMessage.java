package backend.security;
import java.io.Serializable;
import java.math.BigInteger;

public class SecureNetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        DATA, 
        DH_PUBLIC_KEY, 
        ACK, 
        NACK, 
        CONFIRM, 
        CONFIRM_RESPONSE, HEARTBEAT
    }
    
    private MessageType type;
    private byte[] encryptedData;
    private int sequenceNumber;
    private BigInteger dhPublicKey;
    
    public SecureNetworkMessage(MessageType type, byte[] encryptedData, int sequenceNumber) {
        this.type = type;
        this.encryptedData = encryptedData;
        this.sequenceNumber = sequenceNumber;
    }
    
    public SecureNetworkMessage(MessageType type, BigInteger dhPublicKey) {
        this.type = type;
        this.dhPublicKey = dhPublicKey;
    }
    
    // Getters
    public MessageType getType() { return type; }
    public byte[] getEncryptedData() { return encryptedData; }
    public int getSequenceNumber() { return sequenceNumber; }
    public BigInteger getDhPublicKey() { return dhPublicKey; }
}