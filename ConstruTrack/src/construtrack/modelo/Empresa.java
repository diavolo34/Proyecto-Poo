package construtrack.modelo;

import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La empresa (la obra) y todo su personal registrado.
 */
public class Empresa {

    private final String nombre;
    private final Map<String, Usuario> usuarios = new LinkedHashMap<>();
    private final Map<String, Obra> obras = new LinkedHashMap<>();
    private final Map<String, Cuenta> cuentas = new LinkedHashMap<>();
    private final Map<CodigoInvitacion, Invitacion> invitaciones = new LinkedHashMap<>();

    public Empresa(String nombre) {
        this.nombre = nombre;
    }

    public void agregarUsuario(Usuario usuario) {
        if (existeCedula(usuario.getCedula())) {
            throw new OperacionInvalidaException("Ya existe un usuario con la cédula " + usuario.getCedula() + ".");
        }
        usuarios.put(usuario.getCedula(), usuario);
        if (usuario instanceof Jefe) {
            ((Jefe) usuario).setEmpresa(this);
        }
    }

    public Usuario buscarUsuario(String cedula) {
        return cedula == null ? null : usuarios.get(cedula.trim().toUpperCase());
    }

    public boolean existeCedula(String cedula) {
        return cedula != null && usuarios.containsKey(cedula.trim().toUpperCase());
    }

    /** Quita al usuario y deshace sus relaciones de supervisión. */
    public void eliminarUsuario(String cedula) {
        Usuario usuario = usuarios.remove(cedula);
        if (usuario != null) {
            usuario.desvincularCuenta();
        }
        if (usuario instanceof Empleado) {
            ((Empleado) usuario).setSupervisor(null);
        } else if (usuario instanceof Supervisor) {
            for (Empleado empleado : ((Supervisor) usuario).verEmpleadosACargo()) {
                empleado.setSupervisor(null);
            }
        }
    }

    public void agregarObra(Obra obra) {
        if (obras.containsKey(obra.getCodigo())) {
            throw new OperacionInvalidaException("Ya existe una obra con el código " + obra.getCodigo() + ".");
        }
        obras.put(obra.getCodigo(), obra);
    }

    /** Código libre para una obra nueva: OB-01, OB-02… (sigue al mayor número usado). */
    public String siguienteCodigoObra() {
        int mayor = 0;
        for (String codigo : obras.keySet()) {
            String numero = codigo.substring(codigo.indexOf('-') + 1);
            if (numero.matches("\\d+")) {
                mayor = Math.max(mayor, Integer.parseInt(numero));
            }
        }
        return String.format("OB-%02d", mayor + 1);
    }

    /** Personas asignadas a la obra. */
    public List<Usuario> personasEn(Obra obra) {
        List<Usuario> personas = new ArrayList<>();
        for (Usuario u : usuarios.values()) {
            if (u.getObra() == obra) {
                personas.add(u);
            }
        }
        return personas;
    }

    /** Elimina una obra; no se permite mientras tenga personas asignadas. */
    public void eliminarObra(String codigo) {
        Obra obra = obras.get(codigo);
        if (obra == null) {
            throw new OperacionInvalidaException("No existe la obra " + codigo + ".");
        }
        int personas = personasEn(obra).size();
        if (personas > 0) {
            throw new OperacionInvalidaException("La obra «" + obra.getNombre() + "» tiene " + personas
                    + " persona(s) asignada(s). Cámbialas de obra antes de eliminarla.");
        }
        obras.remove(codigo);
    }

    public Obra buscarObra(String codigo) {
        return codigo == null ? null : obras.get(codigo);
    }

    public List<Obra> listarObras() {
        return new ArrayList<>(obras.values());
    }

    // ------------------------------------------------------------------ cuentas e invitaciones

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

    public void agregarInvitacion(Invitacion invitacion) {
        if (invitaciones.containsKey(invitacion.getCodigo())) {
            throw new OperacionInvalidaException("Ya existe una invitación con el código " + invitacion.getCodigo() + ".");
        }
        invitaciones.put(invitacion.getCodigo(), invitacion);
    }

    public Invitacion buscarInvitacion(CodigoInvitacion codigo) {
        return invitaciones.get(codigo);
    }

    public boolean existeInvitacion(CodigoInvitacion codigo) {
        return invitaciones.containsKey(codigo);
    }

    public List<Invitacion> listarInvitaciones() {
        return new ArrayList<>(invitaciones.values());
    }

    public List<Usuario> listarUsuarios() {
        return new ArrayList<>(usuarios.values());
    }

    /** Devuelve el usuario si la cédula y la clave coinciden; si no, null. */
    public Usuario autenticar(String cedula, String clave) {
        Usuario usuario = buscarUsuario(cedula);
        return usuario != null && usuario.verificarClave(clave) ? usuario : null;
    }

    public int contarPresentes() {
        int presentes = 0;
        for (Usuario usuario : usuarios.values()) {
            if (usuario.estaPresente()) {
                presentes++;
            }
        }
        return presentes;
    }

    public String getNombre() {
        return nombre;
    }
}
