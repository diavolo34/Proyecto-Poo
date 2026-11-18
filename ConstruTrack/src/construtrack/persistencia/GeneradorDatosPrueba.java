package construtrack.persistencia;

import construtrack.modelo.Empleado;
import construtrack.modelo.Empresa;
import construtrack.modelo.EstadoHorasExtra;
import construtrack.modelo.Horario;
import construtrack.modelo.CodigoInvitacion;
import construtrack.modelo.Cuenta;
import construtrack.modelo.Invitacion;
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
import java.util.List;
import java.util.function.Supplier;

public class GeneradorDatosPrueba implements Supplier<Empresa> {
    private final Clock reloj;

    private final FabricaIncidencias fabrica;

    public GeneradorDatosPrueba(Clock reloj, FabricaIncidencias fabrica) {
        this.reloj = reloj;
        this.fabrica = fabrica;
    }

    @Override
    public Empresa get() {
        Empresa empresa = new Empresa("Constructora Demo");
        Horario oficina = Horario.desdeTexto("LU,MA,MI,JU,VI", "08:00", "17:00");
        Horario obra = Horario.desdeTexto("LU,MA,MI,JU,VI", "07:00", "16:00");
        Horario sabados = Horario.desdeTexto("MA,MI,JU,VI,SA", "07:00", "15:00");

        Jefe carlos = new Jefe("10000001", "Carlos Méndez", "jefe123", oficina);
        empresa.agregarUsuario(carlos);
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
        torre.configurarControl(10.4969, -66.8539, 150, true);
        luis.setObra(puente);

        for (Usuario lider : new Usuario[] {carlos, ana, luis}) {
            LocalTime entrada = lider.getHorario().getHoraEntrada().minusMinutes(10);
            for (LocalDate dia : hoy.minusDays(14).datesUntil(hoy).toList()) {
                if (lider.trabajaEl(dia)) {
                    lider.agregarAsistencia(registro(dia, entrada.toString(), lider.getHorario().getHoraSalida().toString(), false));
                }
            }
        }
        for (Supervisor s : new Supervisor[] {ana, luis}) {
            if (s.trabajaEl(hoy)) {
                s.agregarAsistencia(registro(hoy, "06:40", null, false));
            }
        }

        LocalDate haceUnaSemana = hoy.minusDays(5);
        for (int i = 0; i < empleados.length; i++) {
            boolean extraHecha = false;
            Empleado e = new Empleado(empleados[i][0], empleados[i][1], "emp123", i < 3 ? obra : sabados);
            e.setSupervisor(i < 3 ? ana : luis);
            e.setObra(i < 3 ? torre : puente);
            empresa.agregarUsuario(e);

            for (LocalDate dia : hoy.minusDays(14).datesUntil(hoy).toList()) {
                if (!e.trabajaEl(dia) || dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    continue;
                }

                boolean retardo = i == 2
                        ? !dia.isBefore(hoy.with(DayOfWeek.MONDAY)) && dia.getDayOfWeek().getValue() <= 3
                        : (dia.getDayOfMonth() + i) % 5 == 0;

                boolean deReposo = i == 0 && (dia.equals(hoy.minusDays(4)) || dia.equals(hoy.minusDays(3)));
                boolean falta = deReposo || (dia.getDayOfMonth() * 3 + i) % 11 == 0;
                if (!falta) {
                    String entrada = retardo ? "07:25" : "06:5" + (i % 10);
                    LocalTime salida = e.getHorario().getHoraSalida().plusMinutes(i % 9);

                    boolean conExtra = !extraHecha && !dia.isBefore(haceUnaSemana) && (i == 0 || i == 3);
                    if (conExtra) {
                        salida = salida.plusMinutes(i == 0 ? 150 : 75);
                        extraHecha = true;
                    }
                    RegistroAsistencia jornada = registro(dia, entrada, salida.toString(), retardo);
                    if (conExtra && i == 0) {
                        jornada.restaurarEstadoExtra(EstadoHorasExtra.APROBADAS);
                    }
                    e.agregarAsistencia(jornada);
                }
            }

            LocalDate sabado = hoy.with(java.time.temporal.TemporalAdjusters.previous(DayOfWeek.SATURDAY));
            if (i == 1 && !e.trabajaEl(sabado)) {
                e.agregarAsistencia(registro(sabado, "08:00", "12:30", false));
            }
            if (i % 2 == 0 && e.trabajaEl(hoy)) {
                e.agregarAsistencia(registro(hoy, i == 4 ? "07:25" : "06:58", null, i == 4));
            }
        }
        agregarIncidenciasDemo(empresa, fabrica, hoy);
        agregarCuentasDemo(empresa, hoy);
        marcaPorConfirmarDemo(empresa, hoy);
        return empresa;
    }

    public static final String CODIGO_DEMO = "CT-RQ7K-4M2P";

    private void marcaPorConfirmarDemo(Empresa empresa, LocalDate hoy) {
        List<RegistroAsistencia> jornadas = empresa.buscarUsuario("30000003").getAsistencias();
        for (int i = jornadas.size() - 1; i >= 0; i--) {
            if (jornadas.get(i).getFecha().isBefore(hoy)) {
                jornadas.get(i).marcarPorConfirmar("Entrada a 1,3 km de la obra");
                return;
            }
        }
    }

    private void agregarCuentasDemo(Empresa empresa, LocalDate hoy) {
        LocalDateTime registro = hoy.minusMonths(2).atTime(9, 0);
        for (Usuario u : empresa.listarUsuarios()) {
            String[] partes = u.getNombre().split(" ", 2);
            String correo = (sinTildes(partes[0]) + "." + sinTildes(partes[1])).toLowerCase() + "@construtrack.demo";
            Cuenta cuenta = Cuenta.restaurar(correo, u.getClaveResumen(), partes[0], partes[1], u.getCedula(),
                    "+58 412 555" + u.getCedula().substring(4), registro);
            empresa.agregarCuenta(cuenta);
            u.vincularCuenta(cuenta);
        }
        Cuenta rosa = new Cuenta("rosa.gutierrez@correo.com", "rosa1234", "Rosa", "Gutiérrez", "25123456",
                "+58 414 7654321", hoy.atTime(8, 0));
        rosa.marcarVerificada();
        empresa.agregarCuenta(rosa);
        Jefe jefe = (Jefe) empresa.buscarUsuario("10000001");
        Invitacion invitacion = new Invitacion(CodigoInvitacion.de(CODIGO_DEMO), Empleado.ROL,
                (Supervisor) empresa.buscarUsuario("20000001"), empresa.buscarObra("OB-01"), jefe,
                LocalDateTime.now(reloj).minusHours(1), LocalDateTime.now(reloj).plusDays(30));
        empresa.agregarInvitacion(invitacion);
    }

    private static String sinTildes(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

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
