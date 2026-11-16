package construtrack.modelo;

import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Empresa {
    private final String nombre;
    private final Plataforma plataforma;
    private final Map<String, Usuario> usuarios = new LinkedHashMap<>();
    private final Map<String, Obra> obras = new LinkedHashMap<>();
    private final Map<CodigoInvitacion, Invitacion> invitaciones = new LinkedHashMap<>();

    public Empresa(String nombre) {
        this(nombre, new Plataforma());
    }

    Empresa(String nombre, Plataforma plataforma) {
        this.nombre = Plataforma.validarNombreEmpresa(nombre);
        this.plataforma = plataforma;
        plataforma.registrar(this);
    }

    public Plataforma getPlataforma() {
        return plataforma;
    }

    public void agregarUsuario(Usuario usuario) {
        if (plataforma.existeCedula(usuario.getCedula())) {
            throw new OperacionInvalidaException("Ya existe una persona con la cédula " + usuario.getCedula() + ".");
        }
        usuarios.put(usuario.getCedula(), usuario);
        usuario.setEmpresa(this);
    }

    public Usuario buscarUsuario(String cedula) {
        return buscarUsuarioLocal(cedula);
    }

    Usuario buscarUsuarioLocal(String cedula) {
        return cedula == null ? null : usuarios.get(cedula.trim().toUpperCase());
    }

    public boolean existeCedula(String cedula) {
        return plataforma.existeCedula(cedula);
    }

    public void eliminarUsuario(String cedula) {
        Usuario usuario = usuarios.remove(cedula);
        if (usuario != null) {
            usuario.desvincularCuenta();
            usuario.setEmpresa(null);
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

    public List<Usuario> personasEn(Obra obra) {
        List<Usuario> personas = new ArrayList<>();
        for (Usuario u : usuarios.values()) {
            if (u.getObra() == obra) {
                personas.add(u);
            }
        }
        return personas;
    }

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

    public void agregarCuenta(Cuenta cuenta) {
        plataforma.agregarCuenta(cuenta);
    }

    public Cuenta buscarCuenta(String correo) {
        return plataforma.buscarCuenta(correo);
    }

    public void agregarInvitacion(Invitacion invitacion) {
        if (plataforma.existeInvitacion(invitacion.getCodigo())) {
            throw new OperacionInvalidaException("Ya existe una invitación con el código " + invitacion.getCodigo() + ".");
        }
        invitaciones.put(invitacion.getCodigo(), invitacion);
    }

    public Invitacion buscarInvitacion(CodigoInvitacion codigo) {
        return invitaciones.get(codigo);
    }

    public boolean existeInvitacion(CodigoInvitacion codigo) {
        return plataforma.existeInvitacion(codigo);
    }

    public List<Invitacion> listarInvitaciones() {
        return new ArrayList<>(invitaciones.values());
    }

    public List<Usuario> listarUsuarios() {
        return new ArrayList<>(usuarios.values());
    }

    public Usuario autenticar(String cedula, String clave) {
        Usuario usuario = buscarUsuario(cedula);
        return usuario != null && usuario.verificarClave(clave) ? usuario : null;
    }

    public boolean incluye(Usuario usuario) {
        return usuario != null && usuario.getEmpresa() == this;
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
