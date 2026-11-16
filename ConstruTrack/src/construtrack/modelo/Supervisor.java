package construtrack.modelo;

import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.incidencias.EstadoIncidencia;
import construtrack.modelo.incidencias.Incidencia;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Supervisa a los empleados que tiene a cargo: revisa su asistencia, reporta sus novedades
 * y aprueba o rechaza sus incidencias. No puede modificar datos del personal (eso es del Jefe).
 */
public class Supervisor extends Usuario implements RevisorIncidencias {

    public static final String ROL = "Supervisor";

    private final List<Empleado> empleadosACargo = new ArrayList<>();

    public Supervisor(String cedula, String nombre, String clave, Horario horario) {
        super(cedula, nombre, clave, horario);
    }

    @Override
    public String getRol() {
        return ROL;
    }

    /** Lo llama Empleado.setSupervisor para mantener la relación en ambos sentidos. */
    void agregarEmpleado(Empleado empleado) {
        if (!empleadosACargo.contains(empleado)) {
            empleadosACargo.add(empleado);
        }
    }

    void quitarEmpleado(Empleado empleado) {
        empleadosACargo.remove(empleado);
    }

    public List<Empleado> verEmpleadosACargo() {
        return new ArrayList<>(empleadosACargo);
    }

    public boolean supervisa(Usuario usuario) {
        return empleadosACargo.contains(usuario);
    }

    /** Historial de asistencia de uno de sus empleados; si no es su empleado, lanza PermisoDenegado. */
    public List<RegistroAsistencia> consultarAsistencia(Empleado empleado) {
        if (!supervisa(empleado)) {
            throw new PermisoDenegadoException("Solo puedes consultar a los empleados que supervisas.");
        }
        return empleado.getAsistencias();
    }

    /** Cuántos de sus empleados están ahora en la obra. */
    public int contarPresentes() {
        int presentes = 0;
        for (Empleado empleado : empleadosACargo) {
            if (empleado.estaPresente()) {
                presentes++;
            }
        }
        return presentes;
    }

    @Override
    public boolean puedeReportarPara(Usuario afectado) {
        return afectado == this || supervisa(afectado);
    }

    @Override
    public boolean puedeGestionarObras() {
        return true;
    }

    /** Su equipo y él mismo (puede moverse de obra junto con su gente). */
    @Override
    public boolean puedeAsignarObraA(Usuario otro) {
        return otro == this || supervisa(otro);
    }

    @Override
    public boolean puedeAprobarHorasDe(Usuario otro) {
        return supervisa(otro) && otro != this;
    }

    @Override
    public boolean puedeRevisar(Incidencia incidencia) {
        return supervisa(incidencia.getAfectado()) && incidencia.getAfectado() != this;
    }

    @Override
    public void revisar(Incidencia incidencia, EstadoIncidencia nuevo, String comentario, LocalDateTime ahora) {
        revisarComo(this, incidencia, nuevo, comentario, ahora);
    }
}
