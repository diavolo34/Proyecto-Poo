package construtrack.servicio;

import static construtrack.Datos.AHORA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import construtrack.Datos;
import construtrack.modelo.Invitacion;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificadorBrevoTest {
    private final Datos d = new Datos();
    private final AtomicReference<String> cuerpo = new AtomicReference<>();
    private final AtomicReference<String> clave = new AtomicReference<>();
    private HttpServer servidor;

    private String levantar(int codigoRespuesta) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v3/smtp/email", intercambio -> {
            clave.set(intercambio.getRequestHeaders().getFirst("api-key"));
            cuerpo.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] respuesta = "{\"messageId\":\"<1@brevo>\"}".getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(codigoRespuesta, respuesta.length);
            intercambio.getResponseBody().write(respuesta);
            intercambio.close();
        });
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/v3/smtp/email";
    }

    @AfterEach
    void detener() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    @DisplayName("Envía la invitación con la API key, el código y el enlace para unirse")
    void enviaInvitacion() throws IOException {
        Invitacion inv = d.gerente.crearInvitacion("Empleado", "20000001", "", 7, AHORA, new Random(3));
        NotificadorBrevo brevo = new NotificadorBrevo(levantar(201), "clave-prueba", "gerente@correo.com", "https://sitio.test/");
        brevo.enviarInvitacion(inv, "pablo@correo.com");
        assertEquals("clave-prueba", clave.get());
        assertTrue(cuerpo.get().contains("\"email\":\"pablo@correo.com\""));
        assertTrue(cuerpo.get().contains(inv.getCodigo().toString()), "El correo lleva el código");
        assertTrue(cuerpo.get().contains("https://sitio.test/app.html#registro?correo=pablo%40correo.com"),
                "El botón abre el registro con el correo ya escrito");
        assertTrue(brevo.envioReal());
    }

    @Test
    @DisplayName("El correo para restablecer la clave lleva el enlace con el token y su vencimiento")
    void enviaRestablecimiento() throws IOException {
        NotificadorBrevo brevo = new NotificadorBrevo(levantar(201), "clave-prueba", "gerente@correo.com", "https://sitio.test");
        brevo.enviarRestablecimiento("pablo@correo.com", "tok_123-abc", Duration.ofMinutes(30));
        assertTrue(cuerpo.get().contains("\"email\":\"pablo@correo.com\""));
        assertTrue(cuerpo.get().contains("https://sitio.test/app.html#restablecer?token=tok_123-abc"), cuerpo.get());
        assertTrue(cuerpo.get().contains("30 minutos"));
    }

    @Test
    @DisplayName("Si Brevo rechaza el envío, se informa con un mensaje claro")
    void informaRechazo() throws IOException {
        Invitacion inv = d.gerente.crearInvitacion("Empleado", "", "", 7, AHORA, new Random(4));
        NotificadorBrevo brevo = new NotificadorBrevo(levantar(401), "mala", "gerente@correo.com", "https://sitio.test");
        assertThrows(OperacionInvalidaException.class, () -> brevo.enviarInvitacion(inv, "pablo@correo.com"));
    }
}
