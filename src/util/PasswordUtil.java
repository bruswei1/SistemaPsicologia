package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generarSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return toHex(salt);
    }

    public static String hash(String password, String saltHex) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update((saltHex + password).getBytes("UTF-8"));
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verificar(String password, String saltHex, String hashEsperado) {
        String hashCalculado = hash(password, saltHex);
        return constantTimeEquals(hashCalculado, hashEsperado);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
