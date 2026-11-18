package construtrack.util;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

public final class Base32 {
    private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private Base32() {
    }

    public static String codificar(byte[] datos) {
        StringBuilder texto = new StringBuilder();
        int bufer = 0;
        int bits = 0;
        for (byte b : datos) {
            bufer = (bufer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                texto.append(ALFABETO.charAt((bufer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) {
            texto.append(ALFABETO.charAt((bufer << (5 - bits)) & 31));
        }
        return texto.toString();
    }

    public static byte[] decodificar(String texto) {
        String limpio = texto.replace(" ", "").replace("=", "").toUpperCase(Locale.ROOT);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        int bufer = 0;
        int bits = 0;
        for (char c : limpio.toCharArray()) {
            int valor = ALFABETO.indexOf(c);
            if (valor < 0) {
                throw new IllegalArgumentException("Texto Base32 no válido.");
            }
            bufer = (bufer << 5) | valor;
            bits += 5;
            if (bits >= 8) {
                salida.write((bufer >> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return salida.toByteArray();
    }
}
