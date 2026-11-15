package construtrack.modelo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Convierte una clave en un resumen SHA-256 con "sal" (la cédula), para no guardar claves en texto plano.
 * Es una clase de utilidad: no se instancia.
 */
public final class CifradoClave {

    private CifradoClave() {
    }

    public static String resumir(String sal, String clave) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] bytes = sha.digest((sal + ":" + clave).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible en esta JVM.", e);
        }
    }
}
