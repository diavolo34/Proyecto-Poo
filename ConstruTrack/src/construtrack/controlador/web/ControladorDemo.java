package construtrack.controlador.web;

import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.CredencialesInvalidasException;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.servicio.Ingreso;
import construtrack.servicio.ServicioAutenticacion;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

public class ControladorDemo implements ControladorWeb {
    private static final Map<String, String> PERSONAS = Map.of(
            "Jefe", "10000001", "Supervisor", "20000001", "Empleado", "30000001");

    private final ServicioAutenticacion autenticacion;
    private final GestorSesiones sesiones;
    private final byte[] clave;

    public ControladorDemo(ServicioAutenticacion autenticacion, GestorSesiones sesiones, String clave) {
        this.autenticacion = autenticacion;
        this.sesiones = sesiones;
        this.clave = clave.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.post("/api/demo/acceso", this::acceder);
    }

    private Respuesta acceder(Peticion p) {
        String recibida = p.cuerpo().getOrDefault("clave", "");
        if (!MessageDigest.isEqual(clave, recibida.getBytes(StandardCharsets.UTF_8))) {
            esperar();
            throw new CredencialesInvalidasException();
        }
        String cedula = PERSONAS.get(p.cuerpo().getOrDefault("rol", "Jefe"));
        if (cedula == null) {
            throw new DatoInvalidoException("Elige el rol: Jefe, Supervisor o Empleado.");
        }
        Usuario usuario = autenticacion.buscar(cedula);
        Ingreso ingreso = new Ingreso(usuario.getCuenta(), usuario);
        return Respuesta.json(JsonMapper.sesion(sesiones.iniciarSesion(ingreso), ingreso));
    }

    private static void esperar() {
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
