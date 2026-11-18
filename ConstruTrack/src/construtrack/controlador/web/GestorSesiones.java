package construtrack.controlador.web;

import construtrack.modelo.CifradoClave;
import construtrack.modelo.Cuenta;
import construtrack.modelo.Usuario;
import construtrack.servicio.Ingreso;
import construtrack.servicio.ServicioAutenticacion;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GestorSesiones {
    public static final Duration DURACION = Duration.ofHours(12);
    private static final String CUENTA = "c:";
    private static final String USUARIO = "u:";

    private record Sesion(String id, Instant vence) {
    }

    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();
    private final ServicioAutenticacion autenticacion;
    private final Clock reloj;

    public GestorSesiones(ServicioAutenticacion autenticacion) {
        this(autenticacion, Clock.systemUTC());
    }

    public GestorSesiones(ServicioAutenticacion autenticacion, Clock reloj) {
        this.autenticacion = autenticacion;
        this.reloj = reloj;
    }

    public String iniciarSesion(Ingreso ingreso) {
        Instant ahora = reloj.instant();
        sesiones.values().removeIf(s -> ahora.isAfter(s.vence()));
        String token = CifradoClave.tokenAleatorio();
        String id = ingreso.cuenta() != null ? CUENTA + ingreso.cuenta().getCorreo() : USUARIO + ingreso.usuario().getCedula();
        sesiones.put(CifradoClave.huella(token), new Sesion(id, ahora.plus(DURACION)));
        return token;
    }

    public Usuario obtenerUsuario(String token) {
        String id = idDe(token);
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
        String id = idDe(token);
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
            sesiones.remove(CifradoClave.huella(token));
        }
    }

    public void cerrarSesionesDe(Cuenta cuenta) {
        String porCuenta = CUENTA + cuenta.getCorreo();
        String porUsuario = cuenta.getUsuario() == null ? null : USUARIO + cuenta.getUsuario().getCedula();
        sesiones.values().removeIf(s -> s.id().equals(porCuenta) || s.id().equals(porUsuario));
    }

    private String idDe(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String huella = CifradoClave.huella(token);
        Sesion sesion = sesiones.get(huella);
        if (sesion == null) {
            return null;
        }
        if (reloj.instant().isAfter(sesion.vence())) {
            sesiones.remove(huella);
            return null;
        }
        return sesion.id();
    }
}
