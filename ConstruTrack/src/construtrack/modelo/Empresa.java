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
        return cedula == null ? null : usuarios.get(cedula);
    }

    public boolean existeCedula(String cedula) {
        return cedula != null && usuarios.containsKey(cedula);
    }

    /** Quita al usuario y deshace sus relaciones de supervisión. */
    public void eliminarUsuario(String cedula) {
        Usuario usuario = usuarios.remove(cedula);
        if (usuario instanceof Empleado) {
            ((Empleado) usuario).setSupervisor(null);
        } else if (usuario instanceof Supervisor) {
            for (Empleado empleado : ((Supervisor) usuario).verEmpleadosACargo()) {
                empleado.setSupervisor(null);
            }
        }
    }

    public List<Usuario> listarUsuarios() {
        return new ArrayList<>(usuarios.values());
    }

    /** Devuelve el usuario si la cédula y la clave coinciden; si no, null. */
    public Usuario autenticar(String cedula, String clave) {
        Usuario usuario = buscarUsuario(cedula);
        return usuario != null && clave != null && usuario.verificarClave(clave) ? usuario : null;
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
