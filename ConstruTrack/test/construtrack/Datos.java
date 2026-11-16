package construtrack;

import construtrack.modelo.Empleado;
import construtrack.modelo.Empresa;
import construtrack.modelo.Horario;
import construtrack.modelo.Jefe;
import construtrack.modelo.Supervisor;
import construtrack.modelo.Usuario;
import construtrack.modelo.incidencias.DatosIncidencia;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

public final class Datos {
    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 18, 10, 0);
    public static final LocalDate HOY = AHORA.toLocalDate();
    public static final Clock RELOJ = Clock.fixed(AHORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    public final Empresa empresa = new Empresa("Prueba");
    public final Jefe jefe = new Jefe("10000001", "Carlos Méndez", "jefe123", horario());
    public final Supervisor ana = new Supervisor("20000001", "Ana Rodríguez", "super123", horario());
    public final Supervisor luis = new Supervisor("20000002", "Luis Pérez", "super123", horario());
    public final Empleado jose = new Empleado("30000001", "José García", "emp123", horario());
    public final Empleado laura = new Empleado("30000004", "Laura Torres", "emp123", horario());

    public Datos() {
        empresa.agregarUsuario(jefe);
        empresa.agregarUsuario(ana);
        empresa.agregarUsuario(luis);
        empresa.agregarUsuario(jose);
        empresa.agregarUsuario(laura);
        jose.setSupervisor(ana);
        laura.setSupervisor(luis);
    }

    public static Horario horario() {
        return Horario.desdeTexto("LU,MA,MI,JU,VI", "07:00", "16:00");
    }

    public static DatosIncidencia incidencia(int id, Usuario afectado, Usuario reporta, LocalDate desde, LocalDate hasta,
            Map<String, String> extras) {
        return new DatosIncidencia(id, afectado, reporta, desde, hasta, "Descripción de prueba", AHORA, extras);
    }
}
