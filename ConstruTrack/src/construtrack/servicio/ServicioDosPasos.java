package construtrack.servicio;

import construtrack.modelo.CifradoClave;
import construtrack.modelo.Plataforma;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.persistencia.EscritorDatos;
import construtrack.util.Totp;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServicioDosPasos extends ServicioBase {
    public static final int RESPALDOS = 8;
    public static final int INTENTOS = 5;
    public static final Duration VIGENCIA_DESAFIO = Duration.ofMinutes(5);
    private static final Duration VIGENCIA_INSCRIPCION = Duration.ofMinutes(15);
    private static final String ALFABETO_RESPALDO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom AZAR = new SecureRandom();

    public record Inscripcion(String secreto, String enlace) {
    }

    private record Pendiente(String secreto, Instant vence) {
    }

    private record Desafio(Ingreso ingreso, Instant vence, int fallos) {
    }

    private final Map<String, Pendiente> inscripciones = new ConcurrentHashMap<>();
    private final Map<String, Desafio> desafios = new ConcurrentHashMap<>();

    public ServicioDosPasos(Plataforma plataforma, EscritorDatos escritor, Clock reloj) {
        super(plataforma, escritor, reloj);
    }

    public Inscripcion iniciar(Usuario actual) {
        if (!actual.admiteDosPasos()) {
            throw new PermisoDenegadoException("Solo los jefes pueden activar la verificación en dos pasos.");
        }
        if (actual.tieneDosPasos()) {
            throw new OperacionInvalidaException("La verificación en dos pasos ya está activa.");
        }
        String secreto = Totp.nuevoSecreto();
        inscripciones.put(actual.getCedula(), new Pendiente(secreto, reloj.instant().plus(VIGENCIA_INSCRIPCION)));
        String cuenta = actual.getCuenta() != null ? actual.getCuenta().getCorreo() : actual.getCedula();
        String etiqueta = URLEncoder.encode("ConstruTrack:" + cuenta, StandardCharsets.UTF_8).replace("+", "%20");
        return new Inscripcion(secreto, "otpauth://totp/" + etiqueta + "?secret=" + secreto
                + "&issuer=ConstruTrack&algorithm=SHA1&digits=" + Totp.DIGITOS + "&period=" + Totp.PASO_SEGUNDOS);
    }

    public List<String> activar(Usuario actual, String codigo) {
        Pendiente pendiente = inscripciones.get(actual.getCedula());
        if (pendiente == null || reloj.instant().isAfter(pendiente.vence())) {
            inscripciones.remove(actual.getCedula());
            throw new OperacionInvalidaException("La activación venció. Empieza de nuevo.");
        }
        if (Totp.verificar(pendiente.secreto(), codigo, reloj.instant(), 1) == Totp.SIN_COINCIDENCIA) {
            throw new DatoInvalidoException("El código no coincide. Revisa que la hora del teléfono sea automática y "
                    + "escribe el código que muestra ahora la aplicación.");
        }
        List<String> respaldos = new ArrayList<>();
        List<String> huellas = new ArrayList<>();
        for (int i = 0; i < RESPALDOS; i++) {
            String respaldo = respaldoAleatorio();
            respaldos.add(respaldo.substring(0, 4) + "-" + respaldo.substring(4));
            huellas.add(CifradoClave.huella(respaldo));
        }
        modificar(() -> {
            actual.activarDosPasos(pendiente.secreto(), huellas);
            return null;
        });
        inscripciones.remove(actual.getCedula());
        return respaldos;
    }

    public void desactivar(Usuario actual, String clave, String codigo) {
        if (!actual.tieneDosPasos()) {
            return;
        }
        if (!actual.verificarClave(clave)) {
            throw new DatoInvalidoException("La clave no es correcta.");
        }
        modificar(() -> {
            if (!actual.verificarDosPasos(codigo, reloj.instant())) {
                throw new DatoInvalidoException("El código no es correcto.");
            }
            actual.desactivarDosPasos();
            return null;
        });
    }

    public String crearDesafio(Ingreso ingreso) {
        Instant ahora = reloj.instant();
        desafios.values().removeIf(d -> ahora.isAfter(d.vence()));
        String token = CifradoClave.tokenAleatorio();
        desafios.put(CifradoClave.huella(token), new Desafio(ingreso, ahora.plus(VIGENCIA_DESAFIO), 0));
        return token;
    }

    public Ingreso resolverDesafio(String token, String codigo) {
        String huella = token == null ? "" : CifradoClave.huella(token);
        Desafio desafio = desafios.get(huella);
        if (desafio == null || reloj.instant().isAfter(desafio.vence())) {
            desafios.remove(huella);
            throw new OperacionInvalidaException("La verificación venció. Vuelve a iniciar sesión.");
        }
        Usuario usuario = desafio.ingreso().usuario();
        boolean valido = modificar(() -> usuario.verificarDosPasos(codigo, reloj.instant()));
        if (!valido) {
            int fallos = desafio.fallos() + 1;
            if (fallos >= INTENTOS) {
                desafios.remove(huella);
                throw new OperacionInvalidaException("Demasiados códigos incorrectos. Vuelve a iniciar sesión.");
            }
            desafios.put(huella, new Desafio(desafio.ingreso(), desafio.vence(), fallos));
            throw new DatoInvalidoException("El código no es correcto. Escribe el que muestra ahora tu aplicación "
                    + "o uno de tus códigos de respaldo.");
        }
        desafios.remove(huella);
        return desafio.ingreso();
    }

    private static String respaldoAleatorio() {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            texto.append(ALFABETO_RESPALDO.charAt(AZAR.nextInt(ALFABETO_RESPALDO.length())));
        }
        return texto.toString();
    }
}
