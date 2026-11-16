package construtrack.modelo.alertas;

import java.time.LocalDateTime;

public final class Alerta {
    private final Nivel nivel;
    private final String cedula;
    private final String nombre;
    private final String mensaje;
    private final LocalDateTime generada;

    private final LocalDateTime desde;

    private Alerta(Nivel nivel, String cedula, String nombre, String mensaje, LocalDateTime desde, LocalDateTime generada) {
        this.nivel = nivel;
        this.cedula = cedula;
        this.nombre = nombre;
        this.mensaje = mensaje;
        this.generada = generada;
        this.desde = desde;
    }

    public static Alerta de(Nivel nivel, construtrack.modelo.Usuario empleado, String mensaje, LocalDateTime desde,
            LocalDateTime ahora) {
        return new Alerta(nivel, empleado.getCedula(), empleado.getNombre(), mensaje, desde, ahora);
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

    public LocalDateTime getDesde() {
        return desde;
    }

    public LocalDateTime getGenerada() {
        return generada;
    }
}
