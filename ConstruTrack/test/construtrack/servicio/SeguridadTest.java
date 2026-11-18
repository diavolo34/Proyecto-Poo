package construtrack.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.Datos;
import construtrack.RelojAjustable;
import construtrack.controlador.web.GestorSesiones;
import construtrack.modelo.CifradoClave;
import construtrack.modelo.Invitacion;
import construtrack.modelo.excepciones.CredencialesInvalidasException;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.DemasiadosIntentosException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.persistencia.RepositorioMemoria;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SeguridadTest {
    private static final class CorreoFalso implements Notificador {
        private final List<String> destinos = new ArrayList<>();
        private final List<String> tokens = new ArrayList<>();

        @Override
        public void enviarInvitacion(Invitacion invitacion, String correoDestino) {
            destinos.add(correoDestino);
        }

        @Override
        public void enviarRestablecimiento(String correoDestino, String token, Duration vigencia) {
            destinos.add(correoDestino);
            tokens.add(token);
        }

        String ultimoToken() {
            return tokens.get(tokens.size() - 1);
        }
    }

    private final Datos d = new Datos();
    private final RelojAjustable reloj = new RelojAjustable(Datos.RELOJ);
    private final RepositorioMemoria repositorio = new RepositorioMemoria(d.empresa);
    private final CorreoFalso correo = new CorreoFalso();
    private final Servicios servicios = new Servicios(d.empresa.getPlataforma(), repositorio, reloj, 1, correo);

    private void registrarPablo() {
        servicios.registro().registrar(Map.of("correo", "pablo@correo.com", "clave", "pablo123", "confirmacion", "pablo123",
                "nombre", "Pablo", "apellido", "Rivas", "documento", "28555111", "telefono", "04121234567"));
    }

    @Test
    @DisplayName("Al entrar con un resumen antiguo, la clave pasa a PBKDF2 y se guarda")
    void migraClaveAntigua() {
        d.jose.restaurarClaveResumen(CifradoClave.huella("30000001:emp123"));
        int guardadosAntes = repositorio.getGuardados();
        assertSame(d.jose, servicios.autenticacion().ingresar("30000001", "emp123").usuario());
        assertFalse(d.jose.tieneCifradoAntiguo());
        assertTrue(d.jose.getClaveResumen().startsWith("pbkdf2-sha256$"));
        assertEquals(guardadosAntes + 1, repositorio.getGuardados());
        assertSame(d.jose, servicios.autenticacion().ingresar("30000001", "emp123").usuario());
    }

    @Test
    @DisplayName("Tras 5 intentos fallidos se bloquea 15 minutos, aunque luego la clave sea correcta")
    void bloqueoPorIntentos() {
        for (int i = 0; i < ServicioAutenticacion.INTENTOS_MAXIMOS; i++) {
            assertThrows(CredencialesInvalidasException.class, () -> servicios.autenticacion().ingresar("30000001", "mala"));
        }
        DemasiadosIntentosException e = assertThrows(DemasiadosIntentosException.class,
                () -> servicios.autenticacion().ingresar("30000001", "emp123"));
        assertTrue(e.getMessage().contains("15 minutos"), e.getMessage());
        assertNotNull(servicios.autenticacion().ingresar("20000001", "super123"), "No afecta a otras personas");
        reloj.avanzar(ServicioAutenticacion.BLOQUEO);
        assertNotNull(servicios.autenticacion().ingresar("30000001", "emp123"));
    }

    @Test
    @DisplayName("Un ingreso correcto reinicia la cuenta de intentos fallidos")
    void exitoReinicia() {
        for (int i = 0; i < ServicioAutenticacion.INTENTOS_MAXIMOS - 1; i++) {
            assertThrows(CredencialesInvalidasException.class, () -> servicios.autenticacion().ingresar("30000001", "mala"));
        }
        servicios.autenticacion().ingresar("30000001", "emp123");
        assertThrows(CredencialesInvalidasException.class, () -> servicios.autenticacion().ingresar("30000001", "mala"));
        assertNotNull(servicios.autenticacion().ingresar("30000001", "emp123"));
    }

    @Test
    @DisplayName("Restablecer la clave: el enlace llega por correo, sirve una sola vez y cambia la clave")
    void restablecerClave() {
        registrarPablo();
        servicios.recuperacion().solicitar("  Pablo@Correo.com ");
        assertEquals(List.of("pablo@correo.com"), correo.destinos);
        String token = correo.ultimoToken();

        assertThrows(DatoInvalidoException.class, () -> servicios.recuperacion().restablecer(token, "nueva2026", "otra2026"));
        assertThrows(DatoInvalidoException.class, () -> servicios.recuperacion().restablecer(token, "corta1", "corta1"));
        servicios.recuperacion().restablecer(token, "nueva2026", "nueva2026");

        assertNotNull(servicios.autenticacion().ingresar("pablo@correo.com", "nueva2026"));
        assertThrows(CredencialesInvalidasException.class, () -> servicios.autenticacion().ingresar("pablo@correo.com", "pablo123"));
        assertThrows(OperacionInvalidaException.class, () -> servicios.recuperacion().restablecer(token, "otra2027", "otra2027"),
                "El enlace no se puede usar dos veces");
    }

    @Test
    @DisplayName("El enlace vence a los 30 minutos y un enlace nuevo anula el anterior")
    void enlaceVence() {
        registrarPablo();
        servicios.recuperacion().solicitar("pablo@correo.com");
        String primero = correo.ultimoToken();
        servicios.recuperacion().solicitar("pablo@correo.com");
        String segundo = correo.ultimoToken();
        assertThrows(OperacionInvalidaException.class, () -> servicios.recuperacion().restablecer(primero, "nueva2026", "nueva2026"));
        reloj.avanzar(ServicioRecuperacion.VIGENCIA.plusMinutes(1));
        assertThrows(OperacionInvalidaException.class, () -> servicios.recuperacion().restablecer(segundo, "nueva2026", "nueva2026"));
        assertThrows(OperacionInvalidaException.class, () -> servicios.recuperacion().restablecer("inventado", "nueva2026", "nueva2026"));
    }

    @Test
    @DisplayName("Un correo que no existe no recibe nada ni lo delata, y hay un máximo de 3 enlaces por hora")
    void sinFiltracionYConLimite() {
        servicios.recuperacion().solicitar("nadie@correo.com");
        assertTrue(correo.destinos.isEmpty());
        assertThrows(DatoInvalidoException.class, () -> servicios.recuperacion().solicitar("no-es-correo"));

        registrarPablo();
        for (int i = 0; i < ServicioRecuperacion.SOLICITUDES_POR_HORA + 2; i++) {
            servicios.recuperacion().solicitar("pablo@correo.com");
        }
        assertEquals(ServicioRecuperacion.SOLICITUDES_POR_HORA, correo.destinos.size());
        reloj.avanzar(Duration.ofHours(1));
        servicios.recuperacion().solicitar("pablo@correo.com");
        assertEquals(ServicioRecuperacion.SOLICITUDES_POR_HORA + 1, correo.destinos.size());
    }

    @Test
    @DisplayName("Las sesiones vencen a las 12 horas y se cierran todas al restablecer la clave")
    void sesiones() {
        registrarPablo();
        GestorSesiones sesiones = new GestorSesiones(servicios.autenticacion(), reloj);
        String token = sesiones.iniciarSesion(servicios.autenticacion().ingresar("pablo@correo.com", "pablo123"));
        String otro = sesiones.iniciarSesion(servicios.autenticacion().ingresar("pablo@correo.com", "pablo123"));
        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
        assertNotNull(sesiones.obtenerCuenta(token));

        sesiones.cerrarSesionesDe(servicios.autenticacion().buscarCuenta("pablo@correo.com"));
        assertNull(sesiones.obtenerCuenta(token));
        assertNull(sesiones.obtenerCuenta(otro));

        String nuevo = sesiones.iniciarSesion(servicios.autenticacion().ingresar("30000001", "emp123"));
        reloj.avanzar(GestorSesiones.DURACION.minusMinutes(1));
        assertSame(d.jose, sesiones.obtenerUsuario(nuevo));
        reloj.avanzar(Duration.ofMinutes(2));
        assertNull(sesiones.obtenerUsuario(nuevo));
    }
}
