package construtrack.modelo;

public class Empleado extends Usuario {
    public static final String ROL = "Empleado";

    private Supervisor supervisor;

    public Empleado(String cedula, String nombre, String clave, Horario horario) {
        super(cedula, nombre, clave, horario);
    }

    @Override
    public String getRol() {
        return ROL;
    }

    public Supervisor getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(Supervisor nuevo) {
        if (supervisor != null) {
            supervisor.quitarEmpleado(this);
        }
        supervisor = nuevo;
        if (nuevo != null) {
            nuevo.agregarEmpleado(this);
        }
    }
}
