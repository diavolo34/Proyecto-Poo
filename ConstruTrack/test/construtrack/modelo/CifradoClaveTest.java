package construtrack.modelo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.Datos;
import construtrack.modelo.excepciones.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CifradoClaveTest {
    private final Datos d = new Datos();

    @Test
    @DisplayName("Cada clave se guarda con PBKDF2 y una sal aleatoria: la misma clave da resúmenes distintos")
    void pbkdf2ConSal() {
        String a = CifradoClave.resumir("clave123");
        String b = CifradoClave.resumir("clave123");
        assertTrue(a.startsWith("pbkdf2-sha256$"));
        assertNotEquals(a, b);
        assertTrue(CifradoClave.verificar(a, "ignorada", "clave123"));
        assertFalse(CifradoClave.verificar(a, "ignorada", "clave124"));
        assertFalse(CifradoClave.verificar(a, "ignorada", null));
        assertTrue(CifradoClave.esValido(a));
        assertFalse(CifradoClave.esAntiguo(a));
    }

    @Test
    @DisplayName("Los resúmenes SHA-256 antiguos siguen sirviendo para entrar")
    void resumenAntiguo() {
        String antiguo = CifradoClave.huella("30000001:emp123");
        assertTrue(CifradoClave.esAntiguo(antiguo));
        assertTrue(CifradoClave.verificar(antiguo, "30000001", "emp123"));
        assertFalse(CifradoClave.verificar(antiguo, "30000001", "emp124"));
        d.jose.restaurarClaveResumen(antiguo);
        assertTrue(d.jose.verificarClave("emp123"));
        assertTrue(d.jose.tieneCifradoAntiguo());
        assertThrows(DatoInvalidoException.class, () -> d.jose.restaurarClaveResumen("texto-cualquiera"));
    }

    @Test
    @DisplayName("Una clave nueva exige 8 caracteres, una letra y un número")
    void claveNuevaFuerte() {
        for (String debil : new String[] {"abc123", "solo-letras", "12345678", "con espacio1", null}) {
            assertThrows(DatoInvalidoException.class, () -> Usuario.validarClaveNueva(debil), String.valueOf(debil));
        }
        assertDoesNotThrow(() -> Usuario.validarClaveNueva("obra2026"));
        assertDoesNotThrow(() -> Usuario.validarClaveNueva("Contraseña9"));
    }

    @Test
    @DisplayName("Los tokens aleatorios son largos y no se repiten")
    void tokens() {
        String a = CifradoClave.tokenAleatorio();
        assertTrue(a.matches("[A-Za-z0-9_-]{43}"));
        assertNotEquals(a, CifradoClave.tokenAleatorio());
    }
}
