package construtrack.servicio;

import construtrack.modelo.CodigoInvitacion;
import construtrack.modelo.Cuenta;
import construtrack.modelo.Empresa;
import construtrack.modelo.Invitacion;
import construtrack.modelo.Jefe;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.persistencia.EscritorDatos;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Casos de uso del Jefe: crear, listar y cancelar invitaciones. */
public class ServicioInvitaciones extends ServicioBase {

    private final Notificador notificador;
    private final SecureRandom azar = new SecureRandom();

    public ServicioInvitaciones(Empresa empresa, EscritorDatos escritor, Clock reloj, Notificador notificador) {
        super(empresa, escritor, reloj);
        this.notificador = notificador;
    }

    /** Crea una invitación; si se indica un correo, también se notifica (en la demo, en el registro). */
    public Invitacion crear(Usuario actual, String rol, String cedulaSupervisor, String codigoObra, int dias,
            String correoDestino) {
        Invitacion invitacion = modificar(() -> comoJefe(actual).crearInvitacion(rol, cedulaSupervisor, codigoObra,
                dias, LocalDateTime.now(reloj), azar));
        if (correoDestino != null && !correoDestino.isBlank()) {
            notificador.enviarInvitacion(invitacion, Cuenta.validarCorreo(correoDestino));
        }
        return invitacion;
    }

    /** Invitaciones de la empresa, las más recientes primero. */
    public List<Invitacion> listar(Usuario actual) {
        comoJefe(actual);
        return leer(() -> empresa.listarInvitaciones().stream()
                .sorted(Comparator.comparing(Invitacion::getCreada).reversed()).toList());
    }

    public void cancelar(Usuario actual, String codigo) {
        CodigoInvitacion c = CodigoInvitacion.de(codigo);
        modificar(() -> {
            comoJefe(actual).cancelarInvitacion(c, LocalDateTime.now(reloj));
            return null;
        });
    }

    public LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    private Jefe comoJefe(Usuario actual) {
        if (actual instanceof Jefe jefe) {
            return jefe;
        }
        throw new PermisoDenegadoException("Solo el Jefe puede gestionar invitaciones.");
    }
}
