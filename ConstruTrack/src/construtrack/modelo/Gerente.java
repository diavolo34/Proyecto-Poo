package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.novedades.EstadoNovedad;
import construtrack.modelo.novedades.Novedad;
import construtrack.modelo.talento.Egreso;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public class Gerente extends Usuario implements RevisorNovedades {
    public static final String ROL = "Gerente";

    private java.time.Clock reloj = java.time.Clock.systemDefaultZone();

    public Gerente(String cedula, String nombre, String clave, Horario horario) {
        super(cedula, nombre, clave, horario);
    }

    Gerente(String cedula, String nombre, Horario horario) {
        super(cedula, nombre, horario);
    }

    @Override
    public String getRol() {
        return ROL;
    }

    public Usuario registrarUsuario(Map<String, String> datos) {
        String rol = datos.get("rol");
        verificarRolGestionable(rol);
        validarCedulaNueva(datos.get("cedula"));
        validarClaveNueva(datos.get("clave"));
        if (getEmpresa().existeCedula(datos.get("cedula"))) {
            throw new OperacionInvalidaException("Ya existe un usuario con la cédula " + datos.get("cedula") + ".");
        }
        Horario horario = Horario.desdeTexto(datos.get("dias"), datos.get("horaEntrada"), datos.get("horaSalida"));
        String puesto = getEmpresa().getCatalogo().puestoValido(datos.get("puesto"));
        String area = areaValida(datos.get("area"));
        Usuario nuevo = Usuario.crear(rol, datos.get("cedula"), datos.get("nombre"), datos.get("clave"), horario);
        nuevo.setFechaIngreso(LocalDate.now(reloj));
        nuevo.setPuesto(puesto);
        getEmpresa().agregarUsuario(nuevo);
        if (area != null) {
            getEmpresa().incluirEnArea(nuevo, area);
        }
        if ("true".equals(datos.get("presente"))) {
            nuevo.marcarEntrada(LocalDateTime.now(reloj));
        }
        return nuevo;
    }

    public Usuario modificarUsuario(String cedula, Map<String, String> datos) {
        Usuario usuario = buscarGestionable(cedula);
        usuario.verificarActivo();
        String puesto = datos.containsKey("puesto") ? getEmpresa().getCatalogo().puestoValido(datos.get("puesto")) : usuario.getPuesto();
        boolean cambiaArea = datos.containsKey("area");
        String area = cambiaArea ? areaValida(datos.get("area")) : usuario.getArea();

        String rol = datos.getOrDefault("rol", usuario.getRol());
        verificarRolGestionable(rol);
        String nombre = datos.containsKey("nombre") ? Usuario.validarNombre(datos.get("nombre")) : null;
        String clave = datos.get("clave");
        if (clave != null && !clave.isEmpty()) {
            Usuario.validarClaveNueva(clave);
        }
        Horario actual = usuario.getHorario();
        Horario horario = Horario.desdeTexto(
                datos.getOrDefault("dias", actual.getDias()),
                datos.getOrDefault("horaEntrada", actual.getHoraEntrada().toString()),
                datos.getOrDefault("horaSalida", actual.getHoraSalida().toString()));
        if (nombre != null) {
            usuario.setNombre(nombre);
        }
        if (clave != null && !clave.isEmpty()) {
            usuario.setClave(clave);
        }
        usuario.setHorario(horario);
        usuario.setPuesto(puesto);
        boolean cambiaRol = !rol.equals(usuario.getRol());
        if (cambiaRol) {
            usuario = cambiarRol(usuario, rol);
        }
        if (area == null) {
            if (cambiaArea && getEmpresa().areaDe(usuario) != null) {
                getEmpresa().sacarDeArea(usuario);
            }
        } else if (cambiaArea || cambiaRol) {
            getEmpresa().incluirEnArea(usuario, area);
        }
        if (datos.containsKey("presente")) {
            boolean presente = "true".equals(datos.get("presente"));
            if (presente && !usuario.estaPresente()) {
                usuario.marcarEntrada(LocalDateTime.now(reloj));
            } else if (!presente && usuario.estaPresente()) {
                usuario.marcarSalida(LocalDateTime.now(reloj));
            }
        }
        return usuario;
    }

    public Egreso registrarEgreso(String cedula, Egreso.Datos datos, LocalDateTime ahora) {
        Usuario persona = buscarGestionable(cedula);
        persona.verificarActivo();
        return getEmpresa().registrarEgreso(persona, datos, Firma.de(this), ahora);
    }

    public void eliminarFicha(int idEgreso, String expediente, LocalDateTime ahora) {
        getEmpresa().eliminarFicha(idEgreso, expediente, ahora);
    }

    private String areaValida(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        Area area = getEmpresa().buscarArea(nombre);
        if (area == null) {
            throw new DatoInvalidoException("No existe el área «" + nombre.trim() + "».");
        }
        return area.getNombre();
    }

    @Override
    public boolean puedeIncluirEnArea(Usuario persona, Area area) {
        return !(persona instanceof Gerente) && mismaEmpresaQue(persona);
    }

    private Usuario cambiarRol(Usuario anterior, String rol) {
        verificarRolGestionable(rol);
        Usuario nuevo = Usuario.restaurar(rol, anterior.getCedula(), anterior.getNombre(), anterior.getClaveResumen(),
                anterior.getHorario());
        nuevo.copiarEstadoDe(anterior);
        getEmpresa().eliminarUsuario(anterior.getCedula());
        getEmpresa().agregarUsuario(nuevo);
        return nuevo;
    }

    private Usuario buscarGestionable(String cedula) {
        Usuario usuario = getEmpresa().buscarUsuario(cedula);
        if (usuario == null) {
            throw new DatoInvalidoException("No existe un usuario con la cédula " + cedula + ".");
        }
        verificarRolGestionable(usuario.getRol());
        return usuario;
    }

    private void verificarRolGestionable(String rol) {
        if (!Empleado.ROL.equals(rol) && !Supervisor.ROL.equals(rol)) {
            throw new DatoInvalidoException("El gerente solo puede gestionar usuarios con rol Empleado o Supervisor.");
        }
    }

    public Invitacion crearInvitacion(String rol, String cedulaSupervisor, String codigoObra, int dias,
            LocalDateTime ahora, java.util.random.RandomGenerator azar) {
        return crearInvitacion(rol, cedulaSupervisor, codigoObra, null, null, dias, ahora, azar);
    }

    public Invitacion crearInvitacion(String rol, String cedulaSupervisor, String codigoObra, String puesto, String area,
            int dias, LocalDateTime ahora, java.util.random.RandomGenerator azar) {
        String puestoValido = getEmpresa().getCatalogo().puestoValido(puesto);
        String areaValida = areaValida(area);
        if (dias < 1 || dias > 30) {
            throw new DatoInvalidoException("El código de registro debe durar entre 1 y 30 días.");
        }
        Supervisor supervisor = null;
        if (cedulaSupervisor != null && !cedulaSupervisor.isBlank()) {
            if (!(getEmpresa().buscarUsuario(cedulaSupervisor) instanceof Supervisor s) || !s.estaActivo()) {
                throw new DatoInvalidoException("La cédula " + cedulaSupervisor + " no pertenece a un supervisor activo.");
            }
            supervisor = s;
        }
        Obra obra = null;
        if (codigoObra != null && !codigoObra.isBlank()) {
            obra = getEmpresa().buscarObra(codigoObra);
            if (obra == null) {
                throw new DatoInvalidoException("No existe el proyecto " + codigoObra + ".");
            }
            if (!obra.admitePersonal()) {
                throw new OperacionInvalidaException("El proyecto «" + obra.getNombre() + "» ya concluyó.");
            }
        }
        CodigoInvitacion codigo;
        do {
            codigo = CodigoInvitacion.generar(azar);
        } while (getEmpresa().existeInvitacion(codigo));
        Invitacion invitacion = new Invitacion(codigo, rol, supervisor, obra, this, ahora, ahora.plusDays(dias));
        invitacion.asignarPuesto(puestoValido, areaValida);
        getEmpresa().agregarInvitacion(invitacion);
        return invitacion;
    }

    public void cancelarInvitacion(CodigoInvitacion codigo, LocalDateTime ahora) {
        Invitacion invitacion = getEmpresa().buscarInvitacion(codigo);
        if (invitacion == null) {
            throw new DatoInvalidoException("No existe el código de registro " + codigo + ".");
        }
        invitacion.cancelar(ahora);
    }

    @Override
    public boolean puedeVerNovedadesDe(Usuario afectado) {
        return mismaEmpresaQue(afectado);
    }

    @Override
    protected boolean exigeUbicacionAlMarcar() {
        return false;
    }

    @Override
    public boolean admiteDosPasos() {
        return true;
    }

    @Override
    public boolean puedeGestionarObras() {
        return true;
    }

    @Override
    public boolean puedeAsignarObraA(Usuario otro) {
        return mismaEmpresaQue(otro);
    }

    @Override
    public boolean puedeAprobarHorasDe(Usuario otro) {
        return !(otro instanceof Gerente) && mismaEmpresaQue(otro);
    }

    @Override
    public boolean puedeRevisar(Novedad novedad) {
        return !(novedad.getAfectado() instanceof Gerente) && mismaEmpresaQue(novedad.getAfectado());
    }

    @Override
    public void revisar(Novedad novedad, EstadoNovedad nuevo, String nota, LocalDateTime ahora) {
        revisarComo(this, novedad, nuevo, nota, ahora);
    }
}
