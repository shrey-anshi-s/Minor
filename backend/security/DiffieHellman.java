package backend.security;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;

public class DiffieHellman {
    private static final BigInteger p = new BigInteger(
        "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
        "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
        "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
        "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
        "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
        "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
        "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
        "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B" +
        "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9" +
        "DE2BCBF6955817183995497CEA956AE515D2261898FA0510" +
        "15728E5A8AACAA68FFFFFFFFFFFFFFFF", 16);
    private static final BigInteger g = BigInteger.valueOf(2);
    private final BigInteger privateKey;
    private final BigInteger publicKey;
    
    public DiffieHellman() {
        SecureRandom random = new SecureRandom();
        privateKey = new BigInteger(2048, random);
        publicKey = g.modPow(privateKey, p);
    }
    
    public BigInteger getPublicKey() {
        return publicKey;
    }
    
    public byte[] generateSharedSecret(BigInteger otherPublicKey) {
        BigInteger sharedSecret = otherPublicKey.modPow(privateKey, p);
        return sharedSecret.toByteArray();
    }

    // XOR-based confirmation encryption
    public byte[] confirmKeyExchange(BigInteger otherPublicKey) throws Exception {
        String confirmationMessage = "CONFIRM";
        byte[] confirmationBytes = confirmationMessage.getBytes(StandardCharsets.UTF_8);

        // Generate shared secret using other party's public key (as before)
        byte[] sharedSecret = generateSharedSecret(otherPublicKey);

        // Encrypt using XOR with the shared secret
        byte[] encryptedConfirmation = xorEncryptDecrypt(confirmationBytes, sharedSecret);
        return encryptedConfirmation;
    }

    // XOR-based confirmation verification
    public boolean verifyConfirmation(BigInteger otherPublicKey, byte[] encryptedConfirmation) throws Exception {
        byte[] sharedSecret = generateSharedSecret(otherPublicKey);

        // Decrypt the confirmation message
        byte[] decryptedConfirmation = xorEncryptDecrypt(encryptedConfirmation, sharedSecret);

        // Compare the decrypted message with "CONFIRM"
        String decryptedMessage = new String(decryptedConfirmation, StandardCharsets.UTF_8);
        return "CONFIRM".equals(decryptedMessage);
    }

    // XOR Encryption/Decryption helper (same process for both since XOR is symmetric)
    private byte[] xorEncryptDecrypt(byte[] data, byte[] key) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return result;
    }
}