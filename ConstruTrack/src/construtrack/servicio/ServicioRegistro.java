package construtrack.servicio;

import construtrack.modelo.Plataforma;
import construtrack.modelo.CodigoInvitacion;
import construtrack.modelo.Cuenta;
import construtrack.modelo.Invitacion;
import construtrack.modelo.Jefe;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.NoEncontradoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ServicioRegistro extends ServicioBase {
    private static final Logger LOG = Logger.getLogger(ServicioRegistro.class.getName());

    private final ServicioVerificacionCorreo verificacion;
    private final Map<String, String> empresasPorFundar = new ConcurrentHashMap<>();

    public ServicioRegistro(Plataforma plataforma, EscritorDatos escritor, Clock reloj, ServicioVerificacionCorreo verificacion) {
        super(plataforma, escritor, reloj);
        this.verificacion = verificacion;
    }

    public Cuenta registrar(Map<String, String> datos) {
        String clave = datos.get("clave");
        if (clave == null || !clave.equals(datos.get("confirmacion"))) {
            throw new DatoInvalidoException("Las dos claves no coinciden. Escríbelas de nuevo.");
        }
        Usuario.validarClaveNueva(clave);
        Usuario.validarCedulaNueva(datos.get("documento"));
        boolean esJefe = "jefe".equals(datos.get("tipo"));
        Cuenta registrada = modificar(() -> {
            if (esJefe) {
                plataforma.verificarNombreDisponible(datos.get("empresa"));
            }
            if (plataforma.existeCedula(datos.get("documento"))) {
                throw new OperacionInvalidaException("Ya hay una persona con el documento " + datos.get("documento")
                        + " en ConstruTrack. Inicia sesión.");
            }
            Cuenta cuenta = new Cuenta(datos.get("correo"), clave, datos.get("nombre"), datos.get("apellido"),
                    datos.get("documento"), datos.get("telefono"), LocalDateTime.now(reloj));
            plataforma.agregarCuenta(cuenta);
            return cuenta;
        });
        if (esJefe) {
            empresasPorFundar.put(registrada.getCorreo(), datos.get("empresa"));
        }
        verificacion.enviarCodigo(registrada);
        return registrada;
    }

    public Usuario confirmarCorreo(Cuenta cuenta, String codigo) {
        if (cuenta == null) {
            throw new PermisoDenegadoException("Inicia sesión para confirmar tu correo.");
        }
        verificacion.verificar(cuenta, codigo);
        String empresa = empresasPorFundar.remove(cuenta.getCorreo());
        if (empresa == null || cuenta.perteneceAEmpresa()) {
            return cuenta.getUsuario();
        }
        try {
            return crearEmpresa(cuenta, empresa);
        } catch (OperacionInvalidaException e) {
            LOG.info(() -> "No se pudo crear la empresa pendiente de " + cuenta.getCorreo() + ": " + e.getMessage());
            return null;
        }
    }

    public void reenviarCodigo(Cuenta cuenta) {
        if (cuenta == null) {
            throw new PermisoDenegadoException("Inicia sesión para confirmar tu correo.");
        }
        verificacion.enviarCodigo(cuenta);
    }

    public Jefe crearEmpresa(Cuenta cuenta, String nombre) {
        if (cuenta == null) {
            throw new PermisoDenegadoException("Inicia sesión para crear tu empresa.");
        }
        return modificar(() -> plataforma.fundarEmpresa(nombre, cuenta, LocalDate.now(reloj)));
    }

    public Usuario unirse(Cuenta cuenta, String textoCodigo) {
        CodigoInvitacion codigo = CodigoInvitacion.de(textoCodigo);
        return modificar(() -> {
            Invitacion invitacion = plataforma.buscarInvitacion(codigo);
            if (invitacion == null) {
                throw new NoEncontradoException("No encontramos la invitación " + codigo + ". Revisa el código o pide uno nuevo.");
            }
            return invitacion.canjear(cuenta, LocalDateTime.now(reloj));
        });
    }
}
