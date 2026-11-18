package construtrack.servicio;

import construtrack.modelo.CifradoClave;
import construtrack.modelo.Cuenta;
import construtrack.modelo.Plataforma;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.NoEncontradoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public class ServicioRecuperacion extends ServicioBase {
    public static final Duration VIGENCIA = Duration.ofMinutes(30);
    public static final int SOLICITUDES_POR_HORA = 3;
    private static final Logger LOG = Logger.getLogger(ServicioRecuperacion.class.getName());

    private record Pendiente(String correo, Instant vence) {
    }

    private final Notificador notificador;
    private final Executor envio;
    private final LimiteIntentos solicitudes;
    private final Map<String, Pendiente> pendientes = new ConcurrentHashMap<>();

    public ServicioRecuperacion(Plataforma plataforma, EscritorDatos escritor, Clock reloj, Notificador notificador,
            Executor envio) {
        super(plataforma, escritor, reloj);
        this.notificador = notificador;
        this.envio = envio;
        this.solicitudes = new LimiteIntentos(SOLICITUDES_POR_HORA, Duration.ofHours(1), reloj);
    }

    public void solicitar(String correo) {
        String limpio = Cuenta.validarCorreo(correo);
        pendientes.values().removeIf(p -> reloj.instant().isAfter(p.vence()));
        if (solicitudes.bloqueado(limpio)) {
            return;
        }
        solicitudes.registrar(limpio);
        if (leer(() -> plataforma.buscarCuenta(limpio)) == null) {
            return;
        }
        String token = CifradoClave.tokenAleatorio();
        pendientes.values().removeIf(p -> p.correo().equals(limpio));
        pendientes.put(CifradoClave.huella(token), new Pendiente(limpio, reloj.instant().plus(VIGENCIA)));
        envio.execute(() -> {
            try {
                notificador.enviarRestablecimiento(limpio, token, VIGENCIA);
            } catch (RuntimeException e) {
                LOG.warning(() -> "No se pudo enviar el enlace para restablecer la clave: " + e.getMessage());
            }
        });
    }

    public Cuenta restablecer(String token, String clave, String confirmacion) {
        if (clave == null || !clave.equals(confirmacion)) {
            throw new DatoInvalidoException("Las dos claves no coinciden. Escríbelas de nuevo.");
        }
        Usuario.validarClaveNueva(clave);
        Pendiente pendiente = token == null || token.isBlank() ? null : pendientes.remove(CifradoClave.huella(token.trim()));
        if (pendiente == null || reloj.instant().isAfter(pendiente.vence())) {
            throw new OperacionInvalidaException("El enlace para restablecer la clave no es válido o ya venció. "
                    + "Pide uno nuevo con «¿Olvidaste tu clave?».");
        }
        pendientes.values().removeIf(p -> p.correo().equals(pendiente.correo()));
        return modificar(() -> {
            Cuenta cuenta = plataforma.buscarCuenta(pendiente.correo());
            if (cuenta == null) {
                throw new NoEncontradoException("La cuenta ya no existe.");
            }
            cuenta.cambiarClave(clave);
            cuenta.marcarVerificada();
            return cuenta;
        });
    }
}
