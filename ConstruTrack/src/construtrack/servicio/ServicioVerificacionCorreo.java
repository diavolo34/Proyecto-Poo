package construtrack.servicio;

import construtrack.modelo.CifradoClave;
import construtrack.modelo.Cuenta;
import construtrack.modelo.Plataforma;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.DemasiadosIntentosException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.persistencia.EscritorDatos;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public class ServicioVerificacionCorreo extends ServicioBase {
    public static final Duration VIGENCIA = Duration.ofMinutes(15);
    public static final int INTENTOS = 5;
    public static final int ENVIOS_POR_HORA = 4;
    private static final Logger LOG = Logger.getLogger(ServicioVerificacionCorreo.class.getName());
    private static final SecureRandom AZAR = new SecureRandom();

    private record Pendiente(String huella, Instant vence, int fallos) {
    }

    private final Notificador notificador;
    private final Executor envio;
    private final LimiteIntentos envios;
    private final Map<String, Pendiente> pendientes = new ConcurrentHashMap<>();

    public ServicioVerificacionCorreo(Plataforma plataforma, EscritorDatos escritor, Clock reloj, Notificador notificador,
            Executor envio) {
        super(plataforma, escritor, reloj);
        this.notificador = notificador;
        this.envio = envio;
        this.envios = new LimiteIntentos(ENVIOS_POR_HORA, Duration.ofHours(1), reloj);
    }

    public void enviarCodigo(Cuenta cuenta) {
        if (cuenta.estaVerificada()) {
            return;
        }
        String correo = cuenta.getCorreo();
        if (envios.bloqueado(correo)) {
            throw new DemasiadosIntentosException(envios.minutosDeEspera(correo));
        }
        envios.registrar(correo);
        String codigo = String.format("%06d", AZAR.nextInt(1_000_000));
        pendientes.put(correo, new Pendiente(huella(correo, codigo), reloj.instant().plus(VIGENCIA), 0));
        envio.execute(() -> {
            try {
                notificador.enviarCodigoVerificacion(correo, codigo, VIGENCIA);
            } catch (RuntimeException e) {
                LOG.warning(() -> "No se pudo enviar el código de verificación: " + e.getMessage());
            }
        });
    }

    public void verificar(Cuenta cuenta, String codigo) {
        if (cuenta.estaVerificada()) {
            return;
        }
        String correo = cuenta.getCorreo();
        Pendiente pendiente = pendientes.get(correo);
        if (pendiente == null || reloj.instant().isAfter(pendiente.vence())) {
            pendientes.remove(correo);
            throw new OperacionInvalidaException("El código venció o no existe. Pide uno nuevo con «Reenviar código».");
        }
        String limpio = codigo == null ? "" : codigo.replaceAll("\\s", "");
        if (!MessageDigest.isEqual(pendiente.huella().getBytes(StandardCharsets.US_ASCII),
                huella(correo, limpio).getBytes(StandardCharsets.US_ASCII))) {
            int fallos = pendiente.fallos() + 1;
            if (fallos >= INTENTOS) {
                pendientes.remove(correo);
                throw new OperacionInvalidaException("Demasiados códigos incorrectos. Pide uno nuevo con «Reenviar código».");
            }
            pendientes.put(correo, new Pendiente(pendiente.huella(), pendiente.vence(), fallos));
            throw new DatoInvalidoException("El código no es correcto. Te quedan " + (INTENTOS - fallos)
                    + (INTENTOS - fallos == 1 ? " intento." : " intentos."));
        }
        pendientes.remove(correo);
        modificar(() -> {
            cuenta.marcarVerificada();
            return null;
        });
    }

    private static String huella(String correo, String codigo) {
        return CifradoClave.huella(correo + ":" + codigo);
    }
}
