package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.inventario.Inventario;
import construtrack.modelo.talento.Egreso;
import java.time.LocalDateTime;
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
    private final Catalogo catalogo = new Catalogo();
    private final Inventario inventario = new Inventario();
    private final List<Egreso> egresos = new ArrayList<>();
    private final Map<String, ArchivoAdjunto> archivos = new LinkedHashMap<>();

    public static final int DIAS_CONSERVAR_INVITACIONES_USADAS = 7;

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
        soltarRelaciones(usuario);
    }

    private static void soltarRelaciones(Usuario usuario) {
        if (usuario instanceof Empleado empleado) {
            empleado.setSupervisor(null);
        } else if (usuario instanceof Supervisor supervisor) {
            for (Empleado empleado : supervisor.verEmpleadosACargo()) {
                empleado.setSupervisor(null);
            }
        }
    }

    Egreso registrarEgreso(Usuario persona, Egreso.Datos datos, Firma registra, LocalDateTime ahora) {
        int id = egresos.stream().mapToInt(Egreso::getId).max().orElse(0) + 1;
        Egreso egreso = Egreso.de(id, persona, datos, registra, ahora);
        if (datos.devolvioEquipos()) {
            inventario.devolverTodoDe(persona.getCedula(), registra, ahora);
        }
        soltarRelaciones(persona);
        persona.desactivar(datos.fecha());
        egresos.add(egreso);
        return egreso;
    }

    void eliminarFicha(int idEgreso, String expediente, LocalDateTime ahora) {
        Egreso egreso = buscarEgreso(idEgreso);
        Usuario persona = usuarios.get(egreso.getCedula());
        if (persona != null && persona.estaActivo()) {
            throw new OperacionInvalidaException(persona.getNombre() + " sigue activo: registra el egreso primero.");
        }
        egreso.eliminarFicha(expediente, ahora);
        eliminarUsuario(egreso.getCedula());
    }

    public void restaurarEgreso(Egreso egreso) {
        egresos.add(egreso);
    }

    public Egreso buscarEgreso(int id) {
        for (Egreso e : egresos) {
            if (e.getId() == id) {
                return e;
            }
        }
        throw new DatoInvalidoException("No existe el egreso #" + id + ".");
    }

    public List<Egreso> listarEgresos() {
        return new ArrayList<>(egresos);
    }

    public Catalogo getCatalogo() {
        return catalogo;
    }

    public Inventario getInventario() {
        return inventario;
    }

    public void quitarPuesto(String puesto) {
        for (Usuario u : usuarios.values()) {
            if (u.estaActivo() && puesto.equalsIgnoreCase(u.getPuesto())) {
                throw new OperacionInvalidaException("«" + u.getPuesto() + "» lo tiene " + u.getNombre()
                        + ". Cámbiale el puesto antes de quitarlo del catálogo.");
            }
        }
        catalogo.quitarPuesto(puesto);
    }

    public void quitarArea(String area) {
        for (Usuario u : usuarios.values()) {
            if (u.estaActivo() && area.equalsIgnoreCase(u.getArea())) {
                throw new OperacionInvalidaException("El área «" + u.getArea() + "» la tiene " + u.getNombre()
                        + ". Cámbiale el área antes de quitarla del catálogo.");
            }
        }
        catalogo.quitarArea(area);
    }

    public void agregarArchivo(ArchivoAdjunto archivo) {
        archivos.put(archivo.id(), archivo);
    }

    public ArchivoAdjunto buscarArchivo(String id) {
        return id == null ? null : archivos.get(id);
    }

    public List<ArchivoAdjunto> listarArchivos() {
        return new ArrayList<>(archivos.values());
    }

    public int purgarInvitaciones(LocalDateTime ahora) {
        int antes = invitaciones.size();
        invitaciones.values().removeIf(v -> v.caducadaEn(ahora, DIAS_CONSERVAR_INVITACIONES_USADAS));
        return antes - invitaciones.size();
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
            if (u.estaActivo() && u.getObra() == obra) {
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
        List<Usuario> activos = new ArrayList<>();
        for (Usuario u : usuarios.values()) {
            if (u.estaActivo()) {
                activos.add(u);
            }
        }
        return activos;
    }

    public List<Usuario> listarTodos() {
        return new ArrayList<>(usuarios.values());
    }

    public Usuario autenticar(String cedula, String clave) {
        Usuario usuario = buscarUsuario(cedula);
        return usuario != null && usuario.estaActivo() && usuario.verificarClave(clave) ? usuario : null;
    }

    public boolean incluye(Usuario usuario) {
        return usuario != null && usuario.getEmpresa() == this;
    }

    public int contarPresentes() {
        int presentes = 0;
        for (Usuario usuario : usuarios.values()) {
            if (usuario.estaActivo() && usuario.estaPresente()) {
                presentes++;
            }
        }
        return presentes;
    }

    public String getNombre() {
        return nombre;
    }
}
