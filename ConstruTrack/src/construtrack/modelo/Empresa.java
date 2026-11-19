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
    private final Map<String, Area> areas = new LinkedHashMap<>();
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

    private void soltarRelaciones(Usuario usuario) {
        if (usuario instanceof Empleado empleado) {
            empleado.setSupervisor(null);
        } else if (usuario instanceof Supervisor supervisor) {
            soltarAreasDe(supervisor);
            for (Empleado empleado : supervisor.verEmpleadosACargo()) {
                empleado.setSupervisor(null);
            }
        }
    }

    public Area crearArea(String nombre, Usuario supervisor) {
        String limpio = Area.validarNombre(nombre);
        if (buscarArea(limpio) != null) {
            throw new OperacionInvalidaException("Ya existe el área «" + buscarArea(limpio).getNombre() + "».");
        }
        Supervisor responsable = supervisorActivo(supervisor);
        Area area = new Area(limpio, responsable);
        areas.put(clave(limpio), area);
        ubicarResponsable(responsable, area);
        return area;
    }

    public void restaurarArea(String nombre, Supervisor supervisor) {
        Area area = new Area(nombre, supervisor);
        areas.put(clave(area.getNombre()), area);
    }

    public void cambiarSupervisorDeArea(String nombre, Usuario nuevo) {
        Area area = areaExistente(nombre);
        Supervisor responsable = supervisorActivo(nuevo);
        Supervisor anterior = area.getSupervisor();
        area.asignarSupervisor(responsable);
        ubicarResponsable(responsable, area);
        if (anterior != null && anterior != responsable && area.seLlama(anterior.getArea())) {
            areasDirigidasPor(anterior).stream().findFirst().ifPresent(otra -> anterior.setArea(otra.getNombre()));
        }
        for (Usuario u : integrantes(area)) {
            if (u instanceof Empleado empleado) {
                empleado.setSupervisor(responsable);
            }
        }
    }

    public void incluirEnArea(Usuario persona, String nombre) {
        Area area = areaExistente(nombre);
        if (persona instanceof Gerente) {
            throw new OperacionInvalidaException("El gerente está por encima de todas las áreas: no se incluye en ninguna.");
        }
        persona.verificarActivo();
        List<Area> dirigidas = areasDirigidasPor(persona);
        if (!dirigidas.isEmpty() && !dirigidas.contains(area)) {
            throw new OperacionInvalidaException(persona.getNombre() + " dirige " + nombres(dirigidas)
                    + ": solo puede pertenecer a un área que dirige. Asígnale otro supervisor a esa área primero.");
        }
        persona.setArea(area.getNombre());
        if (persona instanceof Empleado empleado) {
            empleado.setSupervisor(area.getSupervisor());
        }
    }

    public void sacarDeArea(Usuario persona) {
        List<Area> dirigidas = areasDirigidasPor(persona);
        if (!dirigidas.isEmpty()) {
            throw new OperacionInvalidaException(persona.getNombre() + " dirige " + nombres(dirigidas)
                    + ": para sacarlo, asígnale otro supervisor a esa área primero.");
        }
        persona.setArea(null);
        if (persona instanceof Empleado empleado) {
            empleado.setSupervisor(null);
        }
    }

    public List<Usuario> eliminarArea(String nombre) {
        Area area = areaExistente(nombre);
        List<Usuario> integrantes = integrantes(area);
        areas.remove(clave(area.getNombre()));
        for (Usuario u : usuarios.values()) {
            if (area.seLlama(u.getArea())) {
                u.setArea(areasDirigidasPor(u).stream().findFirst().map(Area::getNombre).orElse(null));
                if (u instanceof Empleado empleado) {
                    empleado.setSupervisor(null);
                }
            }
        }
        return integrantes.stream().filter(u -> u.getArea() == null).toList();
    }

    public void soltarAreasDe(Supervisor supervisor) {
        for (Area area : areas.values()) {
            if (area.esDirigidaPor(supervisor)) {
                area.asignarSupervisor(null);
                for (Usuario u : integrantes(area)) {
                    if (u instanceof Empleado empleado) {
                        empleado.setSupervisor(null);
                    }
                }
            }
        }
    }

    public Area buscarArea(String nombre) {
        return nombre == null ? null : areas.get(clave(nombre));
    }

    public Area areaDe(Usuario persona) {
        return buscarArea(persona.getArea());
    }

    public List<Area> listarAreas() {
        return new ArrayList<>(areas.values());
    }

    public List<Area> areasDirigidasPor(Usuario supervisor) {
        List<Area> resultado = new ArrayList<>();
        for (Area area : areas.values()) {
            if (area.esDirigidaPor(supervisor)) {
                resultado.add(area);
            }
        }
        return resultado;
    }

    public List<Usuario> integrantes(Area area) {
        List<Usuario> resultado = new ArrayList<>();
        for (Usuario u : usuarios.values()) {
            if (u.estaActivo() && area.seLlama(u.getArea())) {
                resultado.add(u);
            }
        }
        return resultado;
    }

    public List<Usuario> sinArea() {
        List<Usuario> resultado = new ArrayList<>();
        for (Usuario u : usuarios.values()) {
            if (u.estaActivo() && !(u instanceof Gerente) && buscarArea(u.getArea()) == null) {
                resultado.add(u);
            }
        }
        return resultado;
    }

    public void depurarAreas() {
        for (Usuario u : usuarios.values()) {
            if (u.getArea() != null && buscarArea(u.getArea()) == null) {
                u.setArea(null);
            } else if (u.getArea() != null) {
                u.setArea(buscarArea(u.getArea()).getNombre());
            }
        }
    }

    private Area areaExistente(String nombre) {
        Area area = buscarArea(nombre);
        if (area == null) {
            throw new DatoInvalidoException("No existe el área «" + (nombre == null ? "" : nombre.trim()) + "».");
        }
        return area;
    }

    private void ubicarResponsable(Supervisor responsable, Area area) {
        Area propia = areaDe(responsable);
        if (propia == null || !propia.esDirigidaPor(responsable)) {
            responsable.setArea(area.getNombre());
        }
    }

    private static String nombres(List<Area> lista) {
        return String.join(", ", lista.stream().map(a -> "«" + a.getNombre() + "»").toList());
    }

    private Supervisor supervisorActivo(Usuario usuario) {
        if (!(usuario instanceof Supervisor supervisor) || !supervisor.estaActivo() || !incluye(supervisor)) {
            throw new DatoInvalidoException("Cada área necesita un supervisor activo de la empresa como responsable.");
        }
        return supervisor;
    }

    private static String clave(String nombre) {
        return nombre.trim().toLowerCase(java.util.Locale.ROOT);
    }

    Egreso registrarEgreso(Usuario persona, Egreso.Datos datos, Firma registra, LocalDateTime ahora) {
        int id = egresos.stream().mapToInt(Egreso::getId).max().orElse(0) + 1;
        Egreso egreso = Egreso.de(id, persona, datos, registra, ahora);
        if (datos.devolvioEquipos()) {
            inventario.devolverTodoDe(persona.getCedula(), registra, ahora);
        }
        soltarRelaciones(persona);
        persona.desactivar(datos.fecha());
        persona.setArea(null);
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
            throw new OperacionInvalidaException("Ya existe un proyecto con el código " + obra.getCodigo() + ".");
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
        return String.format("PR-%02d", mayor + 1);
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
            throw new OperacionInvalidaException("No existe el proyecto " + codigo + ".");
        }
        int personas = personasEn(obra).size();
        if (personas > 0) {
            throw new OperacionInvalidaException("El proyecto «" + obra.getNombre() + "» tiene " + personas
                    + " persona(s) asignada(s). Cámbialas de proyecto antes de eliminarlo.");
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
            throw new OperacionInvalidaException("Ya existe el código de registro " + invitacion.getCodigo() + ".");
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
