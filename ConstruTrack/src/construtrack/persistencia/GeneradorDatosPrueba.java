package construtrack.persistencia;

import construtrack.modelo.Empleado;
import construtrack.modelo.Empresa;
import construtrack.modelo.Horario;
import construtrack.modelo.Jefe;
import construtrack.modelo.Obra;
import construtrack.modelo.RegistroAsistencia;
import construtrack.modelo.Supervisor;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Map;
import construtrack.modelo.Usuario;
import construtrack.modelo.incidencias.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.Supplier;

/**
 * Crea el personal de demostración: 1 jefe, 2 supervisores y 6 empleados con algo de historial.
 * Está separado del repositorio (responsabilidad única): el repositorio solo lee y escribe.
 */
public class GeneradorDatosPrueba implements Supplier<Empresa> {

    private final Clock reloj;

    private final FabricaIncidencias fabrica;

    public GeneradorDatosPrueba(Clock reloj, FabricaIncidencias fabrica) {
        this.reloj = reloj;
        this.fabrica = fabrica;
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
        Obra torre = new Obra("OB-01", "Torre Residencial Este", "Av. Principal, Caracas",
                hoy.minusMonths(2), hoy.plusMonths(6));
        Obra puente = new Obra("OB-02", "Puente Vial del Sur", "Autopista Sur, Los Teques",
                hoy.minusWeeks(3), hoy.plusMonths(9));
        empresa.agregarObra(torre);
        empresa.agregarObra(puente);
        ana.setObra(torre);
        luis.setObra(puente);
        // Los supervisores también marcan asistencia (son personal de la obra)
        for (Supervisor s : new Supervisor[] {ana, luis}) {
            for (LocalDate dia : hoy.minusDays(14).datesUntil(hoy).toList()) {
                if (s.trabajaEl(dia)) {
                    s.agregarAsistencia(registro(dia, "06:45", "16:10", false));
                }
            }
            if (s.trabajaEl(hoy)) {
                s.agregarAsistencia(registro(hoy, "06:40", null, false));
                s.setPresente(true);
            }
        }

        for (int i = 0; i < empleados.length; i++) {
            Empleado e = new Empleado(empleados[i][0], empleados[i][1], "emp123", i < 3 ? obra : sabados);
            e.setSupervisor(i < 3 ? ana : luis);
            e.setObra(i < 3 ? torre : puente);
            empresa.agregarUsuario(e);
            // Historial de dos semanas laborables: asistencia puntual con algunos retardos y faltas
            for (LocalDate dia : hoy.minusDays(14).datesUntil(hoy).toList()) {
                if (!e.trabajaEl(dia) || dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    continue;
                }
                // Pedro (tercer empleado) llega tarde tres veces esta semana: caso de demostración de la alerta
                boolean retardo = i == 2
                        ? !dia.isBefore(hoy.with(DayOfWeek.MONDAY)) && dia.getDayOfWeek().getValue() <= 3
                        : (dia.getDayOfMonth() + i) % 5 == 0;
                // José (primer empleado) estuvo de reposo médico hace 3 y 4 días: no asistió esos días
                boolean deReposo = i == 0 && (dia.equals(hoy.minusDays(4)) || dia.equals(hoy.minusDays(3)));
                boolean falta = deReposo || (dia.getDayOfMonth() * 3 + i) % 11 == 0;
                if (!falta) {
                    String entrada = retardo ? "07:25" : "06:5" + (i % 10);
                    e.agregarAsistencia(registro(dia, entrada, "16:0" + (i % 9), retardo));
                }
            }
            if (i % 2 == 0) {
                e.agregarAsistencia(registro(hoy, i == 4 ? "07:25" : "06:58", null, i == 4));
                e.setPresente(true);
            }
        }
        agregarIncidenciasDemo(empresa, fabrica, hoy);
        return empresa;
    }

    /** Incidencias de ejemplo: un reposo aprobado, un permiso pendiente, un retardo y una ausencia sin justificar. */
    private static void agregarIncidenciasDemo(Empresa empresa, FabricaIncidencias fabrica, LocalDate hoy) {
        Usuario jose = empresa.buscarUsuario("30000001");
        Usuario maria = empresa.buscarUsuario("30000002");
        Usuario pedro = empresa.buscarUsuario("30000003");
        Usuario ana = empresa.buscarUsuario("20000001");
        Usuario luis = empresa.buscarUsuario("20000002");
        Usuario laura = empresa.buscarUsuario("30000004");

        LocalDateTime ahora = LocalDateTime.now();
        ReposoMedico reposo = (ReposoMedico) fabrica.crear(ReposoMedico.CODIGO, new DatosIncidencia(1, jose, ana,
                hoy.minusDays(4), hoy.minusDays(3), "Gripe con fiebre, reposo indicado por el médico",
                ahora.minusDays(5), Map.of("centroMedico", "Clínica Santa Sofía", "certificado", "CM-4471")));
        reposo.restaurarRevision(EstadoIncidencia.APROBADA, ana, "Certificado válido", ahora.minusDays(4));
        jose.getHistorial().agregarIncidencia(reposo);

        Incidencia permiso = fabrica.crear(PermisoPersonal.CODIGO, new DatosIncidencia(2, maria, maria,
                hoy.plusDays(2), hoy.plusDays(2), "Trámite en el registro civil", ahora.minusHours(6), Map.of()));
        maria.getHistorial().agregarIncidencia(permiso);

        Incidencia retardo = fabrica.crear(Retardo.CODIGO, new DatosIncidencia(3, pedro, pedro,
                hoy.minusDays(1), null, "Accidente vial en la autopista, se detuvo el tráfico",
                ahora.minusDays(1), Map.of("minutos", "35")));
        pedro.getHistorial().agregarIncidencia(retardo);

        Incidencia ausencia = fabrica.crear(Ausencia.CODIGO, new DatosIncidencia(4, laura, luis,
                hoy.minusDays(2), null, "No se presentó ni avisó al supervisor", ahora.minusDays(2), Map.of()));
        laura.getHistorial().agregarIncidencia(ausencia);
    }

    private RegistroAsistencia registro(LocalDate fecha, String entrada, String salida, boolean tarde) {
        RegistroAsistencia registro = new RegistroAsistencia(fecha, LocalTime.parse(entrada), tarde);
        if (salida != null) {
            registro.registrarSalida(LocalTime.parse(salida));
        }
        return registro;
    }
}
