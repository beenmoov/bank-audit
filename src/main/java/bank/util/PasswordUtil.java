package bank.util;

import java.security.MessageDigest;

/**
 * Utilitaire de hashage SHA-256 pour les mots de passe.
 * Pas de sel ici pour simplifier la demo ; en production utiliser BCrypt.
 */
public class PasswordUtil {

    private PasswordUtil() {}

    /**
     * Retourne le hash SHA-256 du mot de passe en hexadecimal minuscule.
     * Retourne null si le mot de passe est null ou vide.
     */
    public static String hash(String password) {
        if (password == null || password.isEmpty()) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 indisponible", e);
        }
    }

    /** Verifie qu'un mot de passe correspond a son hash stocke. */
    public static boolean verify(String password, String storedHash) {
        if (password == null || storedHash == null) return false;
        return storedHash.equals(hash(password));
    }
}
