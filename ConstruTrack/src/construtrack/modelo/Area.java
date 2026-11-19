package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;

public class Area {
    private final String nombre;
    private Supervisor supervisor;

    public Area(String nombre, Supervisor supervisor) {
        this.nombre = validarNombre(nombre);
        this.supervisor = supervisor;
    }

    public static String validarNombre(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (!limpio.matches("[\\p{L}0-9 .,()/&-]{3,60}")) {
            throw new DatoInvalidoException("El nombre del área debe tener entre 3 y 60 caracteres.");
        }
        return limpio;
    }

    public boolean seLlama(String otro) {
        return otro != null && nombre.equalsIgnoreCase(otro.trim());
    }

    public boolean tieneSupervisor() {
        return supervisor != null;
    }

    public boolean esDirigidaPor(Usuario usuario) {
        return supervisor != null && supervisor == usuario;
    }

    void asignarSupervisor(Supervisor nuevo) {
        this.supervisor = nuevo;
    }

    public String getNombre() {
        return nombre;
    }

    public Supervisor getSupervisor() {
        return supervisor;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
