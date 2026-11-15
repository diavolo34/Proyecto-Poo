package construtrack.modelo.alertas;

import java.time.LocalDateTime;

/** Aviso que el sistema genera para el supervisor sobre un empleado concreto. Es inmutable. */
public final class Alerta {

    private final Nivel nivel;
    private final String cedula;
    private final String nombre;
    private final String mensaje;
    private final LocalDateTime generada;

    private Alerta(Nivel nivel, String cedula, String nombre, String mensaje, LocalDateTime generada) {
        this.nivel = nivel;
        this.cedula = cedula;
        this.nombre = nombre;
        this.mensaje = mensaje;
        this.generada = generada;
    }

    public static Alerta de(Nivel nivel, construtrack.modelo.Usuario empleado, String mensaje, LocalDateTime ahora) {
        return new Alerta(nivel, empleado.getCedula(), empleado.getNombre(), mensaje, ahora);
    }

    public Nivel getNivel() {
        return nivel;
    }

    public String getCedula() {
        return cedula;
    }

    public String getNombre() {
        return nombre;
    }

    public String getMensaje() {
        return mensaje;
    }

    public LocalDateTime getGenerada() {
        return generada;
    }
}
