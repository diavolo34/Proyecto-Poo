package construtrack.controlador.web;

import construtrack.modelo.Usuario;
import construtrack.servicio.ServicioAutenticacion;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sesiones de la vista web: cada inicio de sesión recibe un token aleatorio que el navegador envía
 * en la cabecera Authorization. La vista Swing no lo necesita (mantiene al usuario en memoria).
 */
public class GestorSesiones {

    private final Map<String, String> sesiones = new ConcurrentHashMap<>();
    private final ServicioAutenticacion autenticacion;

    public GestorSesiones(ServicioAutenticacion autenticacion) {
        this.autenticacion = autenticacion;
    }

    public String iniciarSesion(Usuario usuario) {
        String token = UUID.randomUUID().toString();
        sesiones.put(token, usuario.getCedula());
        return token;
    }

    /** Usuario dueño del token, o null si el token no existe o el usuario fue eliminado. */
    public Usuario obtenerUsuario(String token) {
        String cedula = token == null ? null : sesiones.get(token);
        return cedula == null ? null : autenticacion.buscar(cedula);
    }

    public void cerrarSesion(String token) {
        if (token != null) {
            sesiones.remove(token);
        }
    }
}
