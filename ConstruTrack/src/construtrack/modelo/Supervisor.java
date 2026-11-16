package construtrack.modelo;

import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.incidencias.EstadoIncidencia;
import construtrack.modelo.incidencias.Incidencia;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    public List<RegistroAsistencia> consultarAsistencia(Empleado empleado) {
        if (!supervisa(empleado)) {
            throw new PermisoDenegadoException("Solo puedes consultar a los empleados que supervisas.");
        }
        return empleado.getAsistencias();
    }

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
