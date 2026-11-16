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

/** Casos de uso públicos: crear una cuenta con correo, crear una empresa (jefe) o unirse a una con un código. */
public class ServicioRegistro extends ServicioBase {

    public ServicioRegistro(Plataforma plataforma, EscritorDatos escritor, Clock reloj) {
        super(plataforma, escritor, reloj);
    }

    /**
     * Crea una cuenta. Datos: correo, clave, confirmacion, nombre, apellido, documento, telefono y tipo:
     * <ul>
     *   <li>«unirme» (por defecto): la cuenta queda sin empresa hasta que canjee una invitación;</li>
     *   <li>«jefe»: además crea la empresa indicada en «empresa» y la persona queda como su Jefe.</li>
     * </ul>
     * Supervisores y empleados no eligen su rol: lo define el código de invitación.
     */
    public Cuenta registrar(Map<String, String> datos) {
        String clave = datos.get("clave");
        if (clave == null || !clave.equals(datos.get("confirmacion"))) {
            throw new DatoInvalidoException("Las dos claves no coinciden. Escríbelas de nuevo.");
        }
        Usuario.validarCedulaNueva(datos.get("documento"));
        boolean esJefe = "jefe".equals(datos.get("tipo"));
        return modificar(() -> {
            if (esJefe) {
                plataforma.verificarNombreDisponible(datos.get("empresa")); // antes de crear nada
            }
            if (plataforma.existeCedula(datos.get("documento"))) {
                throw new OperacionInvalidaException("Ya hay una persona con el documento " + datos.get("documento")
                        + " en ConstruTrack. Inicia sesión.");
            }
            Cuenta cuenta = new Cuenta(datos.get("correo"), clave, datos.get("nombre"), datos.get("apellido"),
                    datos.get("documento"), datos.get("telefono"), LocalDateTime.now(reloj));
            plataforma.agregarCuenta(cuenta);
            if (esJefe) {
                plataforma.fundarEmpresa(datos.get("empresa"), cuenta, LocalDate.now(reloj));
            }
            return cuenta;
        });
    }

    /** Una cuenta ya registrada (sin empresa) crea su empresa y queda como su Jefe. */
    public Jefe crearEmpresa(Cuenta cuenta, String nombre) {
        if (cuenta == null) {
            throw new PermisoDenegadoException("Inicia sesión para crear tu empresa.");
        }
        return modificar(() -> plataforma.fundarEmpresa(nombre, cuenta, LocalDate.now(reloj)));
    }

    /**
     * Canjea un código de invitación: busca la invitación en todas las empresas y crea la ficha de la persona
     * en la empresa del jefe que la emitió, con el rol, supervisor y obra definidos.
     */
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
