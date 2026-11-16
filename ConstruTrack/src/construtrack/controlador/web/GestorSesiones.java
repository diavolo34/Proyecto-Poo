package construtrack.controlador.web;

import construtrack.modelo.Cuenta;
import construtrack.modelo.Usuario;
import construtrack.servicio.Ingreso;
import construtrack.servicio.ServicioAutenticacion;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GestorSesiones {
    private static final String CUENTA = "c:";
    private static final String USUARIO = "u:";

    private final Map<String, String> sesiones = new ConcurrentHashMap<>();
    private final ServicioAutenticacion autenticacion;

    public GestorSesiones(ServicioAutenticacion autenticacion) {
        this.autenticacion = autenticacion;
    }

    public String iniciarSesion(Ingreso ingreso) {
        String token = UUID.randomUUID().toString();
        sesiones.put(token, ingreso.cuenta() != null ? CUENTA + ingreso.cuenta().getCorreo()
                : USUARIO + ingreso.usuario().getCedula());
        return token;
    }

    public Usuario obtenerUsuario(String token) {
        String id = token == null ? null : sesiones.get(token);
        if (id == null) {
            return null;
        }
        if (id.startsWith(USUARIO)) {
            return autenticacion.buscar(id.substring(USUARIO.length()));
        }
        Cuenta cuenta = autenticacion.buscarCuenta(id.substring(CUENTA.length()));
        return cuenta == null ? null : cuenta.getUsuario();
    }

    public Cuenta obtenerCuenta(String token) {
        String id = token == null ? null : sesiones.get(token);
        if (id == null) {
            return null;
        }
        if (id.startsWith(CUENTA)) {
            return autenticacion.buscarCuenta(id.substring(CUENTA.length()));
        }
        Usuario usuario = autenticacion.buscar(id.substring(USUARIO.length()));
        return usuario == null ? null : usuario.getCuenta();
    }

    public void cerrarSesion(String token) {
        if (token != null) {
            sesiones.remove(token);
        }
    }
}
