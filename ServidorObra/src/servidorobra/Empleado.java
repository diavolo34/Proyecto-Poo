package servidorobra;

/**
 * Trabajador de la obra. Puede tener un Supervisor asignado.
 */
public class Empleado extends Usuario {

    private Supervisor supervisor;

    public Empleado(String cedula, String nombre, String clave, Horario horario) {
        super(cedula, nombre, clave, horario);
    }

    @Override
    public String getRol() {
        return "Empleado";
    }

    public Supervisor getSupervisor() {
        return supervisor;
    }

    /** Cambia de supervisor manteniendo actualizada la lista del supervisor anterior y del nuevo. */
    public void setSupervisor(Supervisor nuevo) {
        if (supervisor != null) {
            supervisor.quitarEmpleado(this);
        }
        supervisor = nuevo;
        if (nuevo != null) {
            nuevo.agregarEmpleado(this);
        }
    }

    @Override
    protected String datosExtraJson() {
        return ",\"supervisor\":" + (supervisor == null ? "null"
                : "{\"cedula\":" + Json.texto(supervisor.getCedula())
                + ",\"nombre\":" + Json.texto(supervisor.getNombre()) + "}");
    }
}
