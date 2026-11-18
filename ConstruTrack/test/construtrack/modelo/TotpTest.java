package construtrack.modelo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.util.Base32;
import construtrack.util.Totp;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TotpTest {
    private static final String SECRETO_RFC = Base32.codificar("12345678901234567890".getBytes(StandardCharsets.US_ASCII));

    @Test
    @DisplayName("Base32 codifica y decodifica como el RFC 4648")
    void base32() {
        assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", SECRETO_RFC);
        assertArrayEquals("12345678901234567890".getBytes(StandardCharsets.US_ASCII), Base32.decodificar("gezd gnbv gy3t qojq gezd gnbv gy3t qojq"));
        assertEquals("MZXW6YQ", Base32.codificar("foob".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    @DisplayName("Los códigos coinciden con los vectores de prueba del RFC 6238 (SHA-1, 6 dígitos)")
    void vectoresRfc() {
        long[][] casos = {{59, 287082}, {1111111109, 81804}, {1111111111, 50471}, {1234567890, 5924}, {2000000000, 279037}};
        for (long[] caso : casos) {
            assertEquals(String.format("%06d", caso[1]), Totp.codigo(SECRETO_RFC, Totp.paso(Instant.ofEpochSecond(caso[0]))));
        }
    }

    @Test
    @DisplayName("Se acepta el código del paso actual y el anterior, no uno viejo ni uno con letras")
    void ventana() {
        String secreto = Totp.nuevoSecreto();
        Instant ahora = Instant.ofEpochSecond(1_800_000_000L);
        long paso = Totp.paso(ahora);
        assertEquals(paso, Totp.verificar(secreto, Totp.codigo(secreto, paso), ahora, 1));
        assertEquals(paso - 1, Totp.verificar(secreto, Totp.codigo(secreto, paso - 1), ahora, 1));
        assertEquals(Totp.SIN_COINCIDENCIA, Totp.verificar(secreto, Totp.codigo(secreto, paso - 3), ahora, 1));
        assertEquals(Totp.SIN_COINCIDENCIA, Totp.verificar(secreto, "12a456", ahora, 1));
        assertTrue(Totp.segundosRestantes(ahora) > 0 && Totp.segundosRestantes(ahora) <= 30);
    }
}
