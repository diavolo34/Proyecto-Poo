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
        String area = getEmpresa().getCatalogo().areaValida(datos.get("area"));
        Usuario nuevo = Usuario.crear(rol, datos.get("cedula"), datos.get("nombre"), datos.get("clave"), horario);
        nuevo.setFechaIngreso(LocalDate.now(reloj));
        nuevo.setPuesto(puesto);
        nuevo.setArea(area);
        if (nuevo instanceof Empleado) {
            asignarSupervisor((Empleado) nuevo, datos.get("supervisor"));
        }
        getEmpresa().agregarUsuario(nuevo);
        if ("true".equals(datos.get("presente"))) {
            nuevo.marcarEntrada(LocalDateTime.now(reloj));
        }
        return nuevo;
    }

    public Usuario modificarUsuario(String cedula, Map<String, String> datos) {
        Usuario usuario = buscarGestionable(cedula);
        usuario.verificarActivo();
        String puesto = datos.containsKey("puesto") ? getEmpresa().getCatalogo().puestoValido(datos.get("puesto")) : usuario.getPuesto();
        String area = datos.containsKey("area") ? getEmpresa().getCatalogo().areaValida(datos.get("area")) : usuario.getArea();

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
        String cedulaSupervisor = datos.get("supervisor");
        if ("Empleado".equals(rol) && cedulaSupervisor != null && !cedulaSupervisor.isEmpty()) {
            if (cedulaSupervisor.equals(cedula)) {
                throw new DatoInvalidoException("Un empleado no puede ser su propio supervisor.");
            }
            if (!(getEmpresa().buscarUsuario(cedulaSupervisor) instanceof Supervisor)) {
                throw new DatoInvalidoException("La cédula " + cedulaSupervisor + " no pertenece a un supervisor.");
            }
        }

        if (nombre != null) {
            usuario.setNombre(nombre);
        }
        if (clave != null && !clave.isEmpty()) {
            usuario.setClave(clave);
        }
        usuario.setHorario(horario);
        usuario.setPuesto(puesto);
        usuario.setArea(area);
        if (!rol.equals(usuario.getRol())) {
            usuario = cambiarRol(usuario, rol);
        }
        if (usuario instanceof Empleado && datos.containsKey("supervisor")) {
            asignarSupervisor((Empleado) usuario, cedulaSupervisor);
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

    public void asignarSupervisor(Empleado empleado, String cedulaSupervisor) {
        if (cedulaSupervisor == null || cedulaSupervisor.isEmpty()) {
            empleado.setSupervisor(null);
            return;
        }
        Usuario supervisor = getEmpresa().buscarUsuario(cedulaSupervisor);
        if (!(supervisor instanceof Supervisor) || !supervisor.estaActivo()) {
            throw new DatoInvalidoException("La cédula " + cedulaSupervisor + " no pertenece a un supervisor.");
        }
        empleado.setSupervisor((Supervisor) supervisor);
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
        String areaValida = getEmpresa().getCatalogo().areaValida(area);
        if (dias < 1 || dias > 30) {
            throw new DatoInvalidoException("La invitación debe durar entre 1 y 30 días.");
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
                throw new DatoInvalidoException("No existe la obra " + codigoObra + ".");
            }
            if (!obra.admitePersonal()) {
                throw new OperacionInvalidaException("La obra «" + obra.getNombre() + "» ya concluyó.");
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
            throw new DatoInvalidoException("No existe la invitación " + codigo + ".");
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
