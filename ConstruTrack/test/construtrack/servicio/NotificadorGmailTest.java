package construtrack.servicio;

import static construtrack.Datos.AHORA;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import construtrack.Datos;
import construtrack.modelo.Invitacion;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificadorGmailTest {
    private final Datos d = new Datos();
    private final AtomicReference<String> cuerpo = new AtomicReference<>();
    private HttpServer servidor;

    private String levantar(String resultado) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/exec", x -> {
            cuerpo.set(new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            x.getResponseHeaders().add("Location", "/echo");
            x.sendResponseHeaders(302, -1);
            x.close();
        });
        servidor.createContext("/echo", x -> {
            byte[] r = resultado.getBytes(StandardCharsets.UTF_8);
            x.sendResponseHeaders(200, r.length);
            x.getResponseBody().write(r);
            x.close();
        });
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/exec";
    }

    @AfterEach
    void detener() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    @DisplayName("Envía la clave, el destino y el código, y sigue la redirección de Apps Script")
    void envia() throws IOException {
        Invitacion inv = d.jefe.crearInvitacion("Empleado", "", "", 7, AHORA, new Random(8));
        new NotificadorGmail(levantar("{\"ok\":true,\"restantes\":99}"), "secreta", "https://sitio.test")
                .enviarInvitacion(inv, "pablo@correo.com");
        assertTrue(cuerpo.get().contains("\"clave\":\"secreta\""));
        assertTrue(cuerpo.get().contains("\"para\":\"pablo@correo.com\""));
        assertTrue(cuerpo.get().contains(inv.getCodigo().toString()));
    }

    @Test
    @DisplayName("Si el script rechaza el pedido (clave incorrecta), se informa el motivo")
    void rechazo() throws IOException {
        Invitacion inv = d.jefe.crearInvitacion("Empleado", "", "", 7, AHORA, new Random(9));
        NotificadorGmail gmail = new NotificadorGmail(levantar("{\"ok\":false,\"error\":\"Clave incorrecta\"}"), "mala",
                "https://sitio.test");
        OperacionInvalidaException e = assertThrows(OperacionInvalidaException.class,
                () -> gmail.enviarInvitacion(inv, "pablo@correo.com"));
        assertTrue(e.getMessage().contains("Clave incorrecta"));
    }
}
