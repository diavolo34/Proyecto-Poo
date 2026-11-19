package construtrack.persistencia;

import construtrack.modelo.CodigoInvitacion;
import construtrack.modelo.Cuenta;
import construtrack.modelo.Empleado;
import construtrack.modelo.Empresa;
import construtrack.modelo.EstadoHorasExtra;
import construtrack.modelo.Firma;
import construtrack.modelo.Gerente;
import construtrack.modelo.Horario;
import construtrack.modelo.Invitacion;
import construtrack.modelo.Obra;
import construtrack.modelo.RegistroAsistencia;
import construtrack.modelo.Supervisor;
import construtrack.modelo.Usuario;
import construtrack.modelo.inventario.CategoriaArticulo;
import construtrack.modelo.inventario.CondicionDevolucion;
import construtrack.modelo.inventario.Inventario;
import construtrack.modelo.novedades.DatosNovedad;
import construtrack.modelo.novedades.EstadoNovedad;
import construtrack.modelo.novedades.FabricaNovedades;
import construtrack.modelo.novedades.Inasistencia;
import construtrack.modelo.novedades.Novedad;
import construtrack.modelo.novedades.PermisoLaboral;
import construtrack.modelo.novedades.ReposoMedico;
import construtrack.modelo.novedades.Retardo;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class GeneradorDatosPrueba implements Supplier<Empresa> {
    private final Clock reloj;

    private final FabricaNovedades fabrica;

    public GeneradorDatosPrueba(Clock reloj, FabricaNovedades fabrica) {
        this.reloj = reloj;
        this.fabrica = fabrica;
    }

    @Override
    public Empresa get() {
        Empresa empresa = new Empresa("Constructora Demo");
        Horario oficina = Horario.desdeTexto("LU,MA,MI,JU,VI", "08:00", "17:00");
        Horario obra = Horario.desdeTexto("LU,MA,MI,JU,VI", "07:00", "16:00");
        Horario sabados = Horario.desdeTexto("MA,MI,JU,VI,SA", "07:00", "15:00");

        Gerente carlos = new Gerente("10000001", "Carlos Méndez", "gerente123", oficina);
        carlos.setPuesto("Gerente");
        empresa.agregarUsuario(carlos);
        Supervisor ana = new Supervisor("20000001", "Ana Rodríguez", "super123", obra);
        ana.setPuesto("Jefe de proyecto");
        Supervisor luis = new Supervisor("20000002", "Luis Pérez", "super123", sabados);
        luis.setPuesto("Supervisor del equipo de mantenimiento");
        empresa.agregarUsuario(ana);
        empresa.agregarUsuario(luis);

        String[][] empleados = {
            {"30000001", "José García", "Plomero", "Plomería"}, {"30000002", "María López", "Electricista", "Electricidad"},
            {"30000003", "Pedro Ramírez", "Ayudante de obra", "Plomería"},
            {"30000004", "Laura Torres", "Electricista", "Mantenimiento"},
            {"30000005", "Miguel Herrera", "Chofer", "Transporte"},
            {"30000006", "Sofía Castillo", "Ayudante de obra", "Mantenimiento"}
        };
        LocalDate hoy = LocalDate.now(reloj);
        Obra torre = new Obra("PR-01", "Torre Residencial Este", "Av. Principal, Caracas",
                hoy.minusMonths(2), hoy.plusMonths(6));
        Obra puente = new Obra("PR-02", "Puente Vial del Sur", "Autopista Sur, Los Teques",
                hoy.minusWeeks(3), hoy.plusMonths(9));
        Obra galpon = new Obra("PR-03", "Galpón Industrial Norte", "Zona Industrial, Guarenas",
                hoy.plusWeeks(3), hoy.plusMonths(8));
        torre.iniciar(hoy.minusMonths(2));
        puente.iniciar(hoy.minusWeeks(3));
        puente.reportarRetraso(Firma.de(luis), "Lluvias de la semana inundaron la excavación del estribo sur",
                hoy.plusMonths(9).plusWeeks(2), LocalDateTime.now(reloj).minusHours(20));
        empresa.agregarObra(torre);
        empresa.agregarObra(puente);
        empresa.agregarObra(galpon);
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
                RegistroAsistencia entrada = registro(hoy, "06:40", null, false);
                if (s.getObra() == torre) {
                    entrada.registrarUbicacionEntrada(10.49702, -66.85401, 9.0);
                } else {
                    entrada.registrarUbicacionEntrada(10.34428, -67.04112, 14.0);
                }
                s.agregarAsistencia(entrada);
            }
        }

        LocalDate haceUnaSemana = hoy.minusDays(5);
        for (int i = 0; i < empleados.length; i++) {
            boolean extraHecha = false;
            Empleado e = new Empleado(empleados[i][0], empleados[i][1], "emp123", i < 3 ? obra : sabados);
            e.setPuesto(empleados[i][2]);
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

                boolean deReposo = i == 0 && (dia.equals(hoy.minusDays(4)) || dia.equals(hoy.minusDays(3)))
                        || i == 5 && !dia.isBefore(hoy.minusDays(1));
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
                RegistroAsistencia entrada = registro(hoy, i == 4 ? "07:25" : "06:58", null, i == 4);
                if (i < 3) {
                    entrada.registrarUbicacionEntrada(10.49671 + i * 0.0002, -66.85372, 11.0 + i);
                } else {
                    entrada.registrarUbicacionEntrada(10.34401, -67.04150, 18.0);
                }
                e.agregarAsistencia(entrada);
            }
        }
        for (Usuario u : empresa.listarUsuarios()) {
            u.setFechaIngreso(hoy.minusDays(14));
        }
        empresa.crearArea("Plomería", ana);
        empresa.crearArea("Electricidad", ana);
        empresa.crearArea("Mantenimiento", luis);
        empresa.crearArea("Transporte", luis);
        for (String[] e : empleados) {
            empresa.incluirEnArea(empresa.buscarUsuario(e[0]), e[3]);
        }
        agregarNovedadesDemo(empresa, fabrica, hoy);
        agregarInventarioDemo(empresa, hoy);
        agregarCuentasDemo(empresa, hoy);
        marcaPorConfirmarDemo(empresa, hoy);
        return empresa;
    }

    public static final String CODIGO_DEMO = "CT-RQ7K-4M2P";

    private void marcaPorConfirmarDemo(Empresa empresa, LocalDate hoy) {
        List<RegistroAsistencia> jornadas = empresa.buscarUsuario("30000003").getAsistencias();
        for (int i = jornadas.size() - 1; i >= 0; i--) {
            if (jornadas.get(i).getFecha().isBefore(hoy)) {
                jornadas.get(i).marcarPorConfirmar("Entrada a 1,3 km del proyecto");
                jornadas.get(i).registrarUbicacionEntrada(10.50859, -66.85390, 20.0);
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
        Gerente gerente = (Gerente) empresa.buscarUsuario("10000001");
        Invitacion invitacion = new Invitacion(CodigoInvitacion.de(CODIGO_DEMO), Empleado.ROL,
                (Supervisor) empresa.buscarUsuario("20000001"), empresa.buscarObra("PR-01"), gerente,
                LocalDateTime.now(reloj).minusHours(1), LocalDateTime.now(reloj).plusDays(30));
        empresa.agregarInvitacion(invitacion);
    }

    private static String sinTildes(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private void agregarNovedadesDemo(Empresa empresa, FabricaNovedades fabrica, LocalDate hoy) {
        Usuario jose = empresa.buscarUsuario("30000001");
        Usuario maria = empresa.buscarUsuario("30000002");
        Usuario pedro = empresa.buscarUsuario("30000003");
        Usuario laura = empresa.buscarUsuario("30000004");
        Usuario sofia = empresa.buscarUsuario("30000006");
        Firma ana = Firma.de(empresa.buscarUsuario("20000001"));
        Firma luis = Firma.de(empresa.buscarUsuario("20000002"));
        Firma carlos = Firma.de(empresa.buscarUsuario("10000001"));
        LocalDateTime ahora = LocalDateTime.now(reloj);

        Novedad reposo = fabrica.crear(ReposoMedico.CODIGO, new DatosNovedad(1, jose, ana, hoy.minusDays(4), hoy.minusDays(3),
                "Gripe con fiebre, reposo indicado por el médico", ahora.minusDays(5), Map.of("medico", "Andrés Salazar",
                        "matricula", "MPPS 45821", "centro", "Clínica Santa Sofía", "tipoConsulta", "INTERNA")));
        reposo.restaurarRevision(EstadoNovedad.APROBADA, carlos, "Reposo verificado con la clínica", ahora.minusDays(4));
        jose.getHistorial().agregarNovedad(reposo);

        Novedad incapacidad = fabrica.crear(ReposoMedico.CODIGO, new DatosNovedad(2, sofia, luis, hoy.minusDays(1),
                hoy.plusDays(2), "Esguince de tobillo izquierdo, reposo absoluto", ahora.minusDays(1), Map.of("medico",
                        "Mariela Rojas", "matricula", "MPPS 61207", "centro", "Hospital Domingo Luciani", "tipoConsulta",
                        "EMERGENCIA")));
        incapacidad.restaurarRevision(EstadoNovedad.APROBADA, carlos, "Aprobado: informe de emergencia recibido",
                ahora.minusHours(1));
        sofia.getHistorial().agregarNovedad(incapacidad);

        maria.getHistorial().agregarNovedad(fabrica.crear(PermisoLaboral.CODIGO, new DatosNovedad(3, maria, ana,
                hoy.plusDays(2), hoy.plusDays(2), "Trámite de documentos en el registro civil", ahora.minusHours(6), Map.of())));
        pedro.getHistorial().agregarNovedad(fabrica.crear(Retardo.CODIGO, new DatosNovedad(4, pedro, ana, hoy.minusDays(1),
                null, "Accidente vial en la autopista, se detuvo el tráfico", ahora.minusDays(1),
                Map.of("minutos", "35", "obra", "Torre Residencial Este"))));
        laura.getHistorial().agregarNovedad(fabrica.crear(Inasistencia.CODIGO, new DatosNovedad(5, laura, luis,
                hoy.minusDays(2), null, "No se presentó ni avisó al supervisor", ahora.minusDays(2),
                Map.of("obra", "Puente Vial del Sur"))));
    }

    private void agregarInventarioDemo(Empresa empresa, LocalDate hoy) {
        Inventario inventario = empresa.getInventario();
        Firma carlos = Firma.de(empresa.buscarUsuario("10000001"));
        LocalDateTime ahora = LocalDateTime.now(reloj);
        String taladro = inventario.agregar(CategoriaArticulo.HERRAMIENTA, "Taladro percutor", "Bosch GSB 13 RE", "BSH-882431", 1,
                "").getCodigo();
        String esmeril = inventario.agregar(CategoriaArticulo.HERRAMIENTA, "Esmeriladora angular 4½\"", "DeWalt DWE4120",
                "DW-551902", 1, "").getCodigo();
        inventario.agregar(CategoriaArticulo.HERRAMIENTA, "Nivel láser de líneas", "Stanley FatMax", "ST-77310", 1, "");
        String andamio = inventario.agregar(CategoriaArticulo.EQUIPO, "Andamio tubular (cuerpo)", "", "", 12,
                "Cuerpos de 1,5 m con crucetas").getCodigo();
        String mezcladora = inventario.agregar(CategoriaArticulo.EQUIPO, "Mezcladora de concreto 1 saco", "Menegotti", "MN-3390", 1,
                "").getCodigo();
        String cascos = inventario.agregar(CategoriaArticulo.MATERIAL, "Casco de seguridad", "3M", "", 30, "Talla única")
                .getCodigo();
        inventario.agregar(CategoriaArticulo.MATERIAL, "Cemento gris 42,5 kg", "", "", 120, "Sacos");

        Usuario jose = empresa.buscarUsuario("30000001");
        Usuario maria = empresa.buscarUsuario("30000002");
        Usuario ana = empresa.buscarUsuario("20000001");
        inventario.asignar(taladro, Firma.de(jose), 1, carlos, "Torre Residencial Este", hoy.plusDays(10), "", ahora.minusDays(6));
        inventario.asignar(andamio, Firma.de(ana), 4, carlos, "Torre Residencial Este", null, "Para fachada norte",
                ahora.minusDays(12));
        inventario.asignar(esmeril, Firma.de(maria), 1, carlos, "Torre Residencial Este", hoy.minusDays(2), "",
                ahora.minusDays(9));
        inventario.asignar(cascos, Firma.de(maria), 1, carlos, null, null, "", ahora.minusDays(30));
        int prestada = inventario.asignar(mezcladora, Firma.de(ana), 1, carlos, "Torre Residencial Este", hoy.minusDays(5), "",
                ahora.minusDays(20)).getId();
        inventario.devolver(prestada, CondicionDevolucion.BUENA, carlos, "", ahora.minusDays(6));
    }

    private RegistroAsistencia registro(LocalDate fecha, String entrada, String salida, boolean tarde) {
        RegistroAsistencia registro = new RegistroAsistencia(fecha, LocalTime.parse(entrada), tarde);
        if (salida != null) {
            registro.registrarSalida(LocalTime.parse(salida));
        }
        return registro;
    }
}
