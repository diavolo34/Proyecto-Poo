package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Plataforma {
    private final List<Empresa> empresas = new ArrayList<>();
    private final Map<String, Cuenta> cuentas = new LinkedHashMap<>();

    public Empresa crearEmpresa(String nombre) {
        return new Empresa(nombre, this);
    }

    void registrar(Empresa empresa) {
        for (Empresa e : empresas) {
            if (e.getNombre().equalsIgnoreCase(empresa.getNombre())) {
                throw new OperacionInvalidaException("Ya existe una empresa llamada «" + empresa.getNombre()
                        + "». Elige otro nombre.");
            }
        }
        empresas.add(empresa);
    }

    public void verificarNombreDisponible(String nombre) {
        String limpio = validarNombreEmpresa(nombre);
        for (Empresa e : empresas) {
            if (e.getNombre().equalsIgnoreCase(limpio)) {
                throw new OperacionInvalidaException("Ya existe una empresa llamada «" + limpio + "». Elige otro nombre.");
            }
        }
    }

    public Jefe fundarEmpresa(String nombre, Cuenta cuenta, LocalDate hoy) {
        if (cuenta.perteneceAEmpresa()) {
            throw new OperacionInvalidaException("Tu cuenta ya pertenece a una empresa.");
        }
        cuenta.exigirVerificada();
        if (existeCedula(cuenta.getDocumento())) {
            throw new OperacionInvalidaException("Ya hay una persona con el documento " + cuenta.getDocumento()
                    + " en ConstruTrack.");
        }
        verificarNombreDisponible(nombre);
        Empresa empresa = crearEmpresa(nombre);
        Jefe jefe = (Jefe) Usuario.restaurar(Jefe.ROL, cuenta.getDocumento(), cuenta.getNombreCompleto(),
                cuenta.getClaveResumen(), Horario.desdeTexto("LU,MA,MI,JU,VI", "08:00", "17:00"));
        jefe.setFechaIngreso(hoy);
        empresa.agregarUsuario(jefe);
        jefe.vincularCuenta(cuenta);
        return jefe;
    }

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

    public Empresa empresaDemo() {
        return empresas.isEmpty() ? null : empresas.get(0);
    }

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

    public Usuario autenticar(String cedula, String clave) {
        Usuario usuario = buscarUsuario(cedula);
        return usuario != null && usuario.verificarClave(clave) ? usuario : null;
    }

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
