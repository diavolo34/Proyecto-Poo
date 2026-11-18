package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.incidencias.EstadoIncidencia;
import construtrack.modelo.incidencias.Incidencia;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class Jefe extends Usuario implements RevisorIncidencias {
    public static final String ROL = "Jefe";

    private java.time.Clock reloj = java.time.Clock.systemDefaultZone();

    public Jefe(String cedula, String nombre, String clave, Horario horario) {
        super(cedula, nombre, clave, horario);
    }

    Jefe(String cedula, String nombre, Horario horario) {
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
        Usuario nuevo = Usuario.crear(rol, datos.get("cedula"), datos.get("nombre"), datos.get("clave"), horario);
        nuevo.setFechaIngreso(LocalDate.now(reloj));
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

    public void eliminarUsuario(String cedula) {
        buscarGestionable(cedula);
        getEmpresa().eliminarUsuario(cedula);
    }

    public void asignarSupervisor(Empleado empleado, String cedulaSupervisor) {
        if (cedulaSupervisor == null || cedulaSupervisor.isEmpty()) {
            empleado.setSupervisor(null);
            return;
        }
        Usuario supervisor = getEmpresa().buscarUsuario(cedulaSupervisor);
        if (!(supervisor instanceof Supervisor)) {
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
            throw new DatoInvalidoException("El jefe solo puede gestionar usuarios con rol Empleado o Supervisor.");
        }
    }

    public Invitacion crearInvitacion(String rol, String cedulaSupervisor, String codigoObra, int dias,
            LocalDateTime ahora, java.util.random.RandomGenerator azar) {
        if (dias < 1 || dias > 30) {
            throw new DatoInvalidoException("La invitación debe durar entre 1 y 30 días.");
        }
        Supervisor supervisor = null;
        if (cedulaSupervisor != null && !cedulaSupervisor.isBlank()) {
            if (!(getEmpresa().buscarUsuario(cedulaSupervisor) instanceof Supervisor s)) {
                throw new DatoInvalidoException("La cédula " + cedulaSupervisor + " no pertenece a un supervisor.");
            }
            supervisor = s;
        }
        Obra obra = null;
        if (codigoObra != null && !codigoObra.isBlank()) {
            obra = getEmpresa().buscarObra(codigoObra);
            if (obra == null) {
                throw new DatoInvalidoException("No existe la obra " + codigoObra + ".");
            }
        }
        CodigoInvitacion codigo;
        do {
            codigo = CodigoInvitacion.generar(azar);
        } while (getEmpresa().existeInvitacion(codigo));
        Invitacion invitacion = new Invitacion(codigo, rol, supervisor, obra, this, ahora, ahora.plusDays(dias));
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
    public boolean puedeReportarPara(Usuario afectado) {
        return mismaEmpresaQue(afectado);
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
        return !(otro instanceof Jefe) && mismaEmpresaQue(otro);
    }

    @Override
    public boolean puedeRevisar(Incidencia incidencia) {
        return !(incidencia.getAfectado() instanceof Jefe) && mismaEmpresaQue(incidencia.getAfectado());
    }

    @Override
    public void revisar(Incidencia incidencia, EstadoIncidencia nuevo, String comentario, LocalDateTime ahora) {
        revisarComo(this, incidencia, nuevo, comentario, ahora);
    }
}
