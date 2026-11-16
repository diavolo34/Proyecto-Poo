package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Raíz del modelo: todas las empresas que usan ConstruTrack y las cuentas personales.
 *
 * Las cuentas son de la plataforma (no de una empresa) porque existen antes de unirse a una: la persona se
 * registra con su correo y después crea su empresa (si es jefe) o se une a una con un código de invitación.
 * Cada empresa solo ve sus propios datos; la cédula es única en todo el sistema porque una persona
 * pertenece a una sola empresa.
 */
public class Plataforma {

    private final List<Empresa> empresas = new ArrayList<>();
    private final Map<String, Cuenta> cuentas = new LinkedHashMap<>();

    /** Crea una empresa nueva con nombre único (sin distinguir mayúsculas). */
    public Empresa crearEmpresa(String nombre) {
        return new Empresa(nombre, this);
    }

    /** La empresa se registra sola al construirse (ver Empresa). */
    void registrar(Empresa empresa) {
        for (Empresa e : empresas) {
            if (e.getNombre().equalsIgnoreCase(empresa.getNombre())) {
                throw new OperacionInvalidaException("Ya existe una empresa llamada «" + empresa.getNombre()
                        + "». Elige otro nombre.");
            }
        }
        empresas.add(empresa);
    }

    /** Comprueba, sin crear nada, que el nombre es válido y está libre. */
    public void verificarNombreDisponible(String nombre) {
        String limpio = validarNombreEmpresa(nombre);
        for (Empresa e : empresas) {
            if (e.getNombre().equalsIgnoreCase(limpio)) {
                throw new OperacionInvalidaException("Ya existe una empresa llamada «" + limpio + "». Elige otro nombre.");
            }
        }
    }

    /**
     * Un jefe funda su empresa: se crea la empresa y su ficha de Jefe (cédula = documento de la cuenta,
     * horario de oficina por defecto que luego puede cambiar) vinculada a la cuenta.
     */
    public Jefe fundarEmpresa(String nombre, Cuenta cuenta, LocalDate hoy) {
        if (cuenta.perteneceAEmpresa()) {
            throw new OperacionInvalidaException("Tu cuenta ya pertenece a una empresa.");
        }
        if (existeCedula(cuenta.getDocumento())) {
            throw new OperacionInvalidaException("Ya hay una persona con el documento " + cuenta.getDocumento()
                    + " en ConstruTrack.");
        }
        verificarNombreDisponible(nombre);
        Empresa empresa = crearEmpresa(nombre);
        Jefe jefe = new Jefe(cuenta.getDocumento(), cuenta.getNombreCompleto(), "temporal",
                Horario.desdeTexto("LU,MA,MI,JU,VI", "08:00", "17:00"));
        jefe.setFechaIngreso(hoy);
        empresa.agregarUsuario(jefe);
        jefe.vincularCuenta(cuenta);
        return jefe;
    }

    /** Nombre de empresa: entre 3 y 60 caracteres. */
    public static String validarNombreEmpresa(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (limpio.length() < 3 || limpio.length() > 60) {
            throw new DatoInvalidoException("El nombre de la empresa debe tener entre 3 y 60 caracteres.");
        }
        return limpio;
    }

    public List<Empresa> listarEmpresas() {
        return new ArrayList<>(empresas);
    }

    /** La primera empresa: la de demostración que se muestra en la portada. */
    public Empresa empresaDemo() {
        return empresas.isEmpty() ? null : empresas.get(0);
    }

    // ------------------------------------------------------------------ personas (en todas las empresas)

    public Usuario buscarUsuario(String cedula) {
        for (Empresa e : empresas) {
            Usuario u = e.buscarUsuarioLocal(cedula);
            if (u != null) {
                return u;
            }
        }
        return null;
    }

    public boolean existeCedula(String cedula) {
        return buscarUsuario(cedula) != null;
    }

    /** Devuelve el usuario si la cédula y la clave coinciden; si no, null. */
    public Usuario autenticar(String cedula, String clave) {
        Usuario usuario = buscarUsuario(cedula);
        return usuario != null && usuario.verificarClave(clave) ? usuario : null;
    }

    // ------------------------------------------------------------------ cuentas

    /** Registra una cuenta nueva: el correo y el documento no pueden repetirse. */
    public void agregarCuenta(Cuenta cuenta) {
        if (cuentas.containsKey(cuenta.getCorreo())) {
            throw new OperacionInvalidaException("Ya existe una cuenta con el correo " + cuenta.getCorreo() + ". Inicia sesión.");
        }
        for (Cuenta c : cuentas.values()) {
            if (c.getDocumento().equals(cuenta.getDocumento())) {
                throw new OperacionInvalidaException("Ya existe una cuenta con el documento " + cuenta.getDocumento() + ".");
            }
        }
        cuentas.put(cuenta.getCorreo(), cuenta);
    }

    public Cuenta buscarCuenta(String correo) {
        return correo == null ? null : cuentas.get(correo.trim().toLowerCase());
    }

    public List<Cuenta> listarCuentas() {
        return new ArrayList<>(cuentas.values());
    }

    // ------------------------------------------------------------------ invitaciones (códigos únicos en todo el sistema)

    public Invitacion buscarInvitacion(CodigoInvitacion codigo) {
        for (Empresa e : empresas) {
            Invitacion v = e.buscarInvitacion(codigo);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    public boolean existeInvitacion(CodigoInvitacion codigo) {
        return buscarInvitacion(codigo) != null;
    }
}
