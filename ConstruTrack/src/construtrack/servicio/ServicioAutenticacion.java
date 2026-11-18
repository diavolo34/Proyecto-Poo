package construtrack.servicio;

import construtrack.modelo.CifradoClave;
import construtrack.modelo.Cuenta;
import construtrack.modelo.Empresa;
import construtrack.modelo.Plataforma;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.CredencialesInvalidasException;
import construtrack.modelo.excepciones.DemasiadosIntentosException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;

public class ServicioAutenticacion extends ServicioBase {
    public static final int INTENTOS_MAXIMOS = 5;
    public static final Duration BLOQUEO = Duration.ofMinutes(15);

    private static final class Senuelo {
        private static final String RESUMEN = CifradoClave.resumir("senuelo-sin-cuenta-1");
    }

    private final LimiteIntentos fallidos;

    public ServicioAutenticacion(Plataforma plataforma, EscritorDatos escritor, Clock reloj) {
        super(plataforma, escritor, reloj);
        this.fallidos = new LimiteIntentos(INTENTOS_MAXIMOS, BLOQUEO, reloj);
    }

    public Ingreso ingresar(String identificador, String clave) {
        String id = identificador == null ? "" : identificador.trim();
        String llave = id.toLowerCase(Locale.ROOT);
        if (fallidos.bloqueado(llave)) {
            throw new DemasiadosIntentosException(fallidos.minutosDeEspera(llave));
        }
        Ingreso ingreso = leer(() -> buscarIngreso(id, clave));
        if (ingreso == null) {
            fallidos.registrar(llave);
            throw new CredencialesInvalidasException();
        }
        fallidos.limpiar(llave);
        if (ingreso.usuario() != null && !ingreso.usuario().estaActivo()) {
            throw new PermisoDenegadoException("Tu acceso a " + ingreso.usuario().getEmpresa().getNombre()
                    + " está desactivado desde el " + ingreso.usuario().getFechaEgreso() + ". Si es un error, habla con el gerente.");
        }
        actualizarCifrado(ingreso, clave);
        return ingreso;
    }

    private Ingreso buscarIngreso(String id, String clave) {
        if (id.contains("@")) {
            Cuenta cuenta = plataforma.buscarCuenta(id);
            if (cuenta == null) {
                CifradoClave.verificar(Senuelo.RESUMEN, "", clave);
                return null;
            }
            return cuenta.verificarClave(clave) ? new Ingreso(cuenta, cuenta.getUsuario()) : null;
        }
        Usuario usuario = id.isEmpty() ? null : plataforma.buscarUsuario(id);
        if (usuario == null) {
            CifradoClave.verificar(Senuelo.RESUMEN, "", clave);
            return null;
        }
        return usuario.verificarClave(clave) ? new Ingreso(usuario.getCuenta(), usuario) : null;
    }

    private void actualizarCifrado(Ingreso ingreso, String clave) {
        boolean antiguo = ingreso.usuario() != null ? ingreso.usuario().tieneCifradoAntiguo()
                : ingreso.cuenta().tieneCifradoAntiguo();
        if (!antiguo) {
            return;
        }
        modificar(() -> {
            if (ingreso.usuario() != null) {
                ingreso.usuario().renovarCifrado(clave);
            } else {
                ingreso.cuenta().renovarCifrado(clave);
            }
            return null;
        });
    }

    public void desbloquear(Cuenta cuenta) {
        fallidos.limpiar(cuenta.getCorreo().toLowerCase(Locale.ROOT));
        fallidos.limpiar(cuenta.getDocumento().toLowerCase(Locale.ROOT));
    }

    public Cuenta buscarCuenta(String correo) {
        return leer(() -> plataforma.buscarCuenta(correo));
    }

    public Usuario buscar(String cedula) {
        return leer(() -> plataforma.buscarUsuario(cedula));
    }

    public String getNombreEmpresa() {
        Empresa demo = plataforma.empresaDemo();
        return demo == null ? "ConstruTrack" : demo.getNombre();
    }
}
