package construtrack.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class Totp {
    public static final int PASO_SEGUNDOS = 30;
    public static final int DIGITOS = 6;
    public static final long SIN_COINCIDENCIA = -1;
    private static final SecureRandom AZAR = new SecureRandom();

    private Totp() {
    }

    public static String nuevoSecreto() {
        byte[] bytes = new byte[20];
        AZAR.nextBytes(bytes);
        return Base32.codificar(bytes);
    }

    public static long paso(Instant momento) {
        return Math.floorDiv(momento.getEpochSecond(), PASO_SEGUNDOS);
    }

    public static String codigo(String secreto, long paso) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(Base32.decodificar(secreto), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(paso).array());
            int desplazamiento = hash[hash.length - 1] & 0x0F;
            int binario = ((hash[desplazamiento] & 0x7F) << 24) | ((hash[desplazamiento + 1] & 0xFF) << 16)
                    | ((hash[desplazamiento + 2] & 0xFF) << 8) | (hash[desplazamiento + 3] & 0xFF);
            return String.format("%06d", binario % 1_000_000);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HmacSHA1 no está disponible en esta JVM.", e);
        }
    }

    public static long verificar(String secreto, String codigo, Instant ahora, int ventana) {
        String limpio = codigo == null ? "" : codigo.replaceAll("\\s", "");
        if (!limpio.matches("\\d{" + DIGITOS + "}")) {
            return SIN_COINCIDENCIA;
        }
        long actual = paso(ahora);
        long encontrado = SIN_COINCIDENCIA;
        for (long p = actual - ventana; p <= actual + ventana; p++) {
            if (MessageDigest.isEqual(codigo(secreto, p).getBytes(StandardCharsets.US_ASCII),
                    limpio.getBytes(StandardCharsets.US_ASCII))) {
                encontrado = p;
            }
        }
        return encontrado;
    }

    public static long segundosRestantes(Instant ahora) {
        return PASO_SEGUNDOS - Math.floorMod(ahora.getEpochSecond(), PASO_SEGUNDOS);
    }
}
