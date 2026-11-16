package construtrack.servicio;

import construtrack.modelo.Plataforma;
import construtrack.modelo.CodigoInvitacion;
import construtrack.modelo.Cuenta;
import construtrack.modelo.Empresa;
import construtrack.modelo.EstadoInvitacion;
import construtrack.modelo.Invitacion;
import construtrack.modelo.Jefe;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.NoEncontradoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
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

    public ServicioInvitaciones(Plataforma plataforma, EscritorDatos escritor, Clock reloj, Notificador notificador) {
        super(plataforma, escritor, reloj);
        this.notificador = notificador;
    }

    /** Invitación recién creada y el resultado del correo (null si no se pidió enviarlo). */
    public record Creada(Invitacion invitacion, String avisoCorreo, boolean correoEnviado) {
    }

    /** Crea una invitación; si se indica un correo, también se envía (o se registra, en modo demostración). */
    public Invitacion crear(Usuario actual, String rol, String cedulaSupervisor, String codigoObra, int dias,
            String correoDestino) {
        return crearYEnviar(actual, rol, cedulaSupervisor, codigoObra, dias, correoDestino).invitacion();
    }

    /**
     * Crea la invitación y, si hay correo, intenta enviarla. Si el correo falla la invitación igual queda creada:
     * el Jefe ve el motivo y puede reenviarla o compartir el código por otro medio.
     */
    public Creada crearYEnviar(Usuario actual, String rol, String cedulaSupervisor, String codigoObra, int dias,
            String correoDestino) {
        String correo = correoDestino == null || correoDestino.isBlank() ? null : Cuenta.validarCorreo(correoDestino);
        Invitacion invitacion = modificar(() -> comoJefe(actual).crearInvitacion(rol, cedulaSupervisor, codigoObra,
                dias, LocalDateTime.now(reloj), azar));
        if (correo == null) {
            return new Creada(invitacion, null, false);
        }
        try {
            notificador.enviarInvitacion(invitacion, correo);
            return new Creada(invitacion, mensajeEnviado(correo), notificador.envioReal());
        } catch (RuntimeException e) {
            return new Creada(invitacion, "La invitación se creó, pero el correo no se pudo enviar: " + e.getMessage(), false);
        }
    }

    /** Envía (o reenvía) por correo una invitación vigente. */
    public String enviar(Usuario actual, String codigo, String correoDestino) {
        comoJefe(actual);
        String correo = Cuenta.validarCorreo(correoDestino);
        Invitacion invitacion = leer(() -> empresaDe(actual).buscarInvitacion(CodigoInvitacion.de(codigo)));
        if (invitacion == null) {
            throw new NoEncontradoException("No existe la invitación " + codigo + ".");
        }
        if (invitacion.estadoEn(LocalDateTime.now(reloj)) != EstadoInvitacion.VIGENTE) {
            throw new OperacionInvalidaException("Solo se pueden enviar invitaciones vigentes.");
        }
        notificador.enviarInvitacion(invitacion, correo);
        return mensajeEnviado(correo);
    }

    /** Indica si los correos llegan de verdad (Brevo configurado) o solo se registran. */
    public boolean correoReal() {
        return notificador.envioReal();
    }

    private String mensajeEnviado(String correo) {
        return notificador.envioReal() ? "Invitación enviada por correo a " + correo + "."
                : "El envío de correos no está configurado: comparte el código con " + correo + " por otro medio.";
    }

    /** Invitaciones de la empresa, las más recientes primero. */
    public List<Invitacion> listar(Usuario actual) {
        comoJefe(actual);
        return leer(() -> empresaDe(actual).listarInvitaciones().stream()
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
