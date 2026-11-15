package construtrack.modelo;

import construtrack.modelo.excepciones.PermisoDenegadoException;
import java.util.ArrayList;
import java.util.List;

/**
 * Supervisa a los empleados que tiene a cargo: ve su horario y si están presentes en la obra.
 */
public class Supervisor extends Usuario {

    private final List<Empleado> empleadosACargo = new ArrayList<>();

    public Supervisor(String cedula, String nombre, String clave, Horario horario) {
        super(cedula, nombre, clave, horario);
    }

    @Override
    public String getRol() {
        return "Supervisor";
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

    /** Lista de empleados que supervisa, con su horario y su estado de presencia. */
    public List<Empleado> verEmpleadosACargo() {
        return new ArrayList<>(empleadosACargo);
    }

    /** Historial de asistencia de uno de sus empleados. */
    public List<RegistroAsistencia> consultarAsistencia(Empleado empleado) {
        if (!empleadosACargo.contains(empleado)) {
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
}
