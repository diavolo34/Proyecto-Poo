package construtrack.modelo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class CifradoClave {
    public static final int ITERACIONES = 600_000;
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String PREFIJO = "pbkdf2-sha256";
    private static final String FORMATO = PREFIJO + "\\$[0-9]{3,7}\\$[A-Za-z0-9_-]{22}\\$[A-Za-z0-9_-]{43}";
    private static final SecureRandom AZAR = new SecureRandom();
    private static final Base64.Encoder CODIFICAR = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODIFICAR = Base64.getUrlDecoder();

    private CifradoClave() {
    }

    public static String resumir(String clave) {
        byte[] sal = new byte[16];
        AZAR.nextBytes(sal);
        int iteraciones = iteraciones();
        return PREFIJO + "$" + iteraciones + "$" + CODIFICAR.encodeToString(sal) + "$"
                + CODIFICAR.encodeToString(derivar(clave, sal, iteraciones));
    }

    public static boolean verificar(String resumen, String salAntigua, String clave) {
        if (resumen == null || clave == null) {
            return false;
        }
        if (esAntiguo(resumen)) {
            byte[] calculado = huella(salAntigua + ":" + clave).getBytes(StandardCharsets.US_ASCII);
            return MessageDigest.isEqual(resumen.getBytes(StandardCharsets.US_ASCII), calculado);
        }
        if (!resumen.matches(FORMATO)) {
            return false;
        }
        String[] partes = resumen.split("\\$");
        byte[] esperado = DECODIFICAR.decode(partes[3]);
        return MessageDigest.isEqual(esperado, derivar(clave, DECODIFICAR.decode(partes[2]), Integer.parseInt(partes[1])));
    }

    public static boolean esAntiguo(String resumen) {
        return resumen != null && resumen.matches("[0-9a-f]{64}");
    }

    public static boolean esValido(String resumen) {
        return esAntiguo(resumen) || resumen != null && resumen.matches(FORMATO);
    }

    public static String tokenAleatorio() {
        byte[] bytes = new byte[32];
        AZAR.nextBytes(bytes);
        return CODIFICAR.encodeToString(bytes);
    }

    public static String huella(String texto) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible en esta JVM.", e);
        }
    }

    private static int iteraciones() {
        return Integer.getInteger("construtrack.iteraciones", ITERACIONES);
    }

    private static byte[] derivar(String clave, byte[] sal, int iteraciones) {
        PBEKeySpec especificacion = new PBEKeySpec(clave.toCharArray(), sal, iteraciones, 256);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 no está disponible en esta JVM.", e);
        } finally {
            especificacion.clearPassword();
        }
    }
}
