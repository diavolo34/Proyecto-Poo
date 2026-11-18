package construtrack.servicio;

import construtrack.modelo.Plataforma;
import construtrack.modelo.CodigoInvitacion;
import construtrack.modelo.Cuenta;
import construtrack.modelo.Empresa;
import construtrack.modelo.EstadoInvitacion;
import construtrack.modelo.Invitacion;
import construtrack.modelo.Gerente;
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

public class ServicioInvitaciones extends ServicioBase {
    private final Notificador notificador;
    private final SecureRandom azar = new SecureRandom();

    public ServicioInvitaciones(Plataforma plataforma, EscritorDatos escritor, Clock reloj, Notificador notificador) {
        super(plataforma, escritor, reloj);
        this.notificador = notificador;
    }

    public record Creada(Invitacion invitacion, String avisoCorreo, boolean correoEnviado) {
    }

    public Invitacion crear(Usuario actual, String rol, String cedulaSupervisor, String codigoObra, int dias,
            String correoDestino) {
        return crearYEnviar(actual, rol, cedulaSupervisor, codigoObra, dias, correoDestino).invitacion();
    }

    public Creada crearYEnviar(Usuario actual, String rol, String cedulaSupervisor, String codigoObra, int dias,
            String correoDestino) {
        return crearYEnviar(actual, rol, cedulaSupervisor, codigoObra, null, null, dias, correoDestino);
    }

    public Creada crearYEnviar(Usuario actual, String rol, String cedulaSupervisor, String codigoObra, String puesto,
            String area, int dias, String correoDestino) {
        String correo = correoDestino == null || correoDestino.isBlank() ? null : Cuenta.validarCorreo(correoDestino);
        Invitacion invitacion = modificar(() -> {
            empresaDe(actual).purgarInvitaciones(LocalDateTime.now(reloj));
            return comoGerente(actual).crearInvitacion(rol, cedulaSupervisor, codigoObra, puesto, area, dias,
                    LocalDateTime.now(reloj), azar);
        });
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

    public String enviar(Usuario actual, String codigo, String correoDestino) {
        comoGerente(actual);
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

    public boolean correoReal() {
        return notificador.envioReal();
    }

    private String mensajeEnviado(String correo) {
        return notificador.envioReal() ? "Invitación enviada por correo a " + correo + "."
                : "El envío de correos no está configurado: comparte el código con " + correo + " por otro medio.";
    }

    public List<Invitacion> listar(Usuario actual) {
        comoGerente(actual);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        boolean hayCaducadas = leer(() -> empresaDe(actual).listarInvitaciones().stream()
                .anyMatch(v -> v.caducadaEn(ahora, construtrack.modelo.Empresa.DIAS_CONSERVAR_INVITACIONES_USADAS)));
        if (hayCaducadas) {
            modificar(() -> empresaDe(actual).purgarInvitaciones(ahora));
        }
        return leer(() -> empresaDe(actual).listarInvitaciones().stream()
                .sorted(Comparator.comparing(Invitacion::getCreada).reversed()).toList());
    }

    public void cancelar(Usuario actual, String codigo) {
        CodigoInvitacion c = CodigoInvitacion.de(codigo);
        modificar(() -> {
            comoGerente(actual).cancelarInvitacion(c, LocalDateTime.now(reloj));
            empresaDe(actual).purgarInvitaciones(LocalDateTime.now(reloj));
            return null;
        });
    }

    public LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    private Gerente comoGerente(Usuario actual) {
        if (actual instanceof Gerente gerente) {
            return gerente;
        }
        throw new PermisoDenegadoException("Solo el gerente gestiona las invitaciones.");
    }
}
