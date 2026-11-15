package construtrack.persistencia;

import construtrack.modelo.Empleado;
import construtrack.modelo.Empresa;
import construtrack.modelo.Horario;
import construtrack.modelo.Jefe;
import construtrack.modelo.RegistroAsistencia;
import construtrack.modelo.Supervisor;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.Supplier;

/**
 * Crea el personal de demostración: 1 jefe, 2 supervisores y 6 empleados con algo de historial.
 * Está separado del repositorio (responsabilidad única): el repositorio solo lee y escribe.
 */
public class GeneradorDatosPrueba implements Supplier<Empresa> {

    private final Clock reloj;

    public GeneradorDatosPrueba(Clock reloj) {
        this.reloj = reloj;
    }

    @Override
    public Empresa get() {
        Empresa empresa = new Empresa("ConstruTrack");
        Horario oficina = Horario.desdeTexto("LU,MA,MI,JU,VI", "08:00", "17:00");
        Horario obra = Horario.desdeTexto("LU,MA,MI,JU,VI", "07:00", "16:00");
        Horario sabados = Horario.desdeTexto("MA,MI,JU,VI,SA", "07:00", "15:00");

        empresa.agregarUsuario(new Jefe("10000001", "Carlos Méndez", "jefe123", oficina));
        Supervisor ana = new Supervisor("20000001", "Ana Rodríguez", "super123", obra);
        Supervisor luis = new Supervisor("20000002", "Luis Pérez", "super123", sabados);
        empresa.agregarUsuario(ana);
        empresa.agregarUsuario(luis);

        String[][] empleados = {
            {"30000001", "José García"}, {"30000002", "María López"}, {"30000003", "Pedro Ramírez"},
            {"30000004", "Laura Torres"}, {"30000005", "Miguel Herrera"}, {"30000006", "Sofía Castillo"}
        };
        LocalDate hoy = LocalDate.now(reloj);
        for (int i = 0; i < empleados.length; i++) {
            Empleado e = new Empleado(empleados[i][0], empleados[i][1], "emp123", i < 3 ? obra : sabados);
            e.setSupervisor(i < 3 ? ana : luis);
            empresa.agregarUsuario(e);
            e.agregarAsistencia(registro(hoy.minusDays(1), "06:55", "16:02", false));
            if (i % 2 == 0) {
                e.agregarAsistencia(registro(hoy, i == 4 ? "07:25" : "06:58", null, i == 4));
                e.setPresente(true);
            }
        }
        return empresa;
    }

    private RegistroAsistencia registro(LocalDate fecha, String entrada, String salida, boolean tarde) {
        RegistroAsistencia registro = new RegistroAsistencia(fecha, LocalTime.parse(entrada), tarde);
        if (salida != null) {
            registro.registrarSalida(LocalTime.parse(salida));
        }
        return registro;
    }
}
