import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class SecurityUtils {
    private KeyPair keyPair;
    private PublicKey partnerPublicKey;
    private SecretKey aesKey;
    private final SecureRandom secureRandom;

    public SecurityUtils() throws NoSuchAlgorithmException {
        this.keyPair = generateKeyPair();
        this.secureRandom = new SecureRandom();
        this.aesKey = generateAESKey();
    }

    // Generowanie pary kluczy RSA
    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    // Generowanie klucza AES
    private SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // używamy 256-bitowego klucza
        return keyGen.generateKey();
    }

    // Generowanie wyzwania do uwierzytelnienia
    public byte[] generateChallenge() {
        byte[] challenge = new byte[32];
        secureRandom.nextBytes(challenge);
        return challenge;
    }

    // Podpisywanie wyzwania
    public byte[] signChallenge(byte[] challenge) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(challenge);
        return signature.sign();
    }

    // Weryfikacja podpisu wyzwania
    public boolean verifyChallenge(byte[] challenge, byte[] signedChallenge, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(challenge);
        return signature.verify(signedChallenge);
    }

    // Szyfrowanie danych za pomocą AES
    public byte[] encryptAES(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        return cipher.doFinal(data.getBytes());
    }

    // Deszyfrowanie danych za pomocą AES
    public String decryptAES(byte[] encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] decryptedBytes = cipher.doFinal(encryptedData);
        return new String(decryptedBytes);
    }

    // Szyfrowanie klucza AES za pomocą klucza publicznego RSA
    public byte[] encryptAESKey(PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(aesKey.getEncoded());
    }

    // Ustawianie klucza AES z zaszyfrowanego klucza
    public void setAESKeyFromEncrypted(byte[] encryptedKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        byte[] decryptedKey = cipher.doFinal(encryptedKey);
        this.aesKey = new SecretKeySpec(decryptedKey, "AES");
    }

    // Ustawianie klucza publicznego partnera
    public void setPartnerPublicKey(PublicKey publicKey) {
        this.partnerPublicKey = publicKey;
    }

    // Pobieranie klucza publicznego
    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    // Kodowanie wiadomości z podpisem
    public String encodeMessage(String message, byte[] signature) throws Exception {
        byte[] encryptedData = encryptAES(message);
        String encodedData = Base64.getEncoder().encodeToString(encryptedData);
        String encodedSignature = Base64.getEncoder().encodeToString(signature);
        return encodedData + "#signature:" + encodedSignature;
    }

    // Dekodowanie wiadomości z podpisem
    public String[] decodeMessage(String encodedMessage) throws Exception {
        String[] parts = encodedMessage.split("#signature:");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid message format");
        }
        // Deszyfrujemy wiadomość używając AES
        byte[] encryptedData = Base64.getDecoder().decode(parts[0]);
        String decryptedMessage = decryptAES(encryptedData);
        System.out.println("Odszyfrowana wiadomość: " + decryptedMessage);
        return new String[]{decryptedMessage, parts[1]};
    }

    // Podpisywanie danych
    public byte[] sign(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashedData = digest.digest(data.getBytes(StandardCharsets.UTF_8));

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(hashedData);
        return signature.sign();
    }

    // Weryfikacja podpisu danych
    public boolean verify(String data, byte[] signedData, PublicKey publicKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashedData = digest.digest(data.getBytes(StandardCharsets.UTF_8));

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(hashedData);
        return signature.verify(signedData);
    }
}