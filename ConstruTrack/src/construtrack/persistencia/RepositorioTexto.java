package construtrack.persistencia;

import construtrack.modelo.CodigoInvitacion;
import construtrack.modelo.Cuenta;
import construtrack.modelo.Empleado;
import construtrack.modelo.EstadoInvitacion;
import construtrack.modelo.Invitacion;
import construtrack.modelo.Gerente;
import construtrack.modelo.Obra;
import construtrack.modelo.Plataforma;
import construtrack.modelo.incidencias.DatosIncidencia;
import construtrack.modelo.incidencias.EstadoIncidencia;
import construtrack.modelo.incidencias.FabricaIncidencias;
import construtrack.modelo.incidencias.Incidencia;
import construtrack.modelo.Empresa;
import construtrack.modelo.EstadoHorasExtra;
import construtrack.modelo.EstadoVerificacion;
import construtrack.modelo.Horario;
import construtrack.modelo.RegistroAsistencia;
import construtrack.modelo.Supervisor;
import construtrack.modelo.Usuario;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class RepositorioTexto implements LectorDatos, EscritorDatos {
    private static final String SEPARADOR = "|";

    private final Almacen almacen;
    private final Supplier<Empresa> datosIniciales;
    private final FabricaIncidencias fabrica;
    private int contadorIds = 1;

    public RepositorioTexto(Almacen almacen, Supplier<Empresa> datosIniciales, FabricaIncidencias fabrica) {
        this.almacen = almacen;
        this.datosIniciales = datosIniciales;
        this.fabrica = fabrica;
    }

    public RepositorioTexto(Path archivo, Supplier<Empresa> datosIniciales, FabricaIncidencias fabrica) {
        this(new AlmacenArchivo(archivo), datosIniciales, fabrica);
    }

    public Almacen getAlmacen() {
        return almacen;
    }

    public int siguienteIdIncidencia() {
        return contadorIds;
    }

    @Override
    public Plataforma cargar() {
        String contenido = almacen.leer().orElse(null);
        if (contenido == null || contenido.isBlank()) {
            Empresa inicial = datosIniciales.get();
            Plataforma plataforma = inicial == null ? new Plataforma() : inicial.getPlataforma();
            actualizarContador(plataforma);
            guardar(plataforma);
            return plataforma;
        }
        Plataforma plataforma = new Plataforma();
        Empresa empresa = null;

        List<String[]> cuentas = new ArrayList<>();
        Map<String[], Empresa> invitaciones = new LinkedHashMap<>();
        for (String linea : contenido.split("\\R")) {
            String[] c = linea.split("\\|", -1);
            switch (c[0]) {
                case "EMPRESA" -> empresa = plataforma.crearEmpresa(Campos.desescapar(c[1]));
                case "U" -> empresa.agregarUsuario(leerUsuario(c));
                case "S" -> leerSupervision(empresa, c);
                case "A" -> empresa.buscarUsuario(c[1]).agregarAsistencia(leerAsistencia(c));
                case "O" -> empresa.agregarObra(leerObra(c));
                case "Y" -> leerAsignacionObra(empresa, c);
                case "I" -> leerIncidencia(empresa, c);
                case "C" -> cuentas.add(c);
                case "V" -> invitaciones.put(c, empresa);
                default -> {
                }
            }
        }
        cuentas.forEach(c -> leerCuenta(plataforma, c));
        invitaciones.forEach((c, e) -> leerInvitacion(e, c));
        return plataforma;
    }

    private void actualizarContador(Plataforma plataforma) {
        for (Empresa empresa : plataforma.listarEmpresas()) {
            for (Usuario u : empresa.listarUsuarios()) {
                for (Incidencia i : u.getHistorial().getIncidencias()) {
                    contadorIds = Math.max(contadorIds, i.getId() + 1);
                }
            }
        }
    }

    @Override
    public void guardar(Plataforma plataforma) {
        List<String> lineas = new ArrayList<>();
        for (Empresa empresa : plataforma.listarEmpresas()) {
            escribirEmpresa(empresa, lineas);
        }
        for (Cuenta c : plataforma.listarCuentas()) {
            lineas.add(unir("C", c.getCorreo(), c.getClaveResumen(), c.getNombre(), c.getApellido(), c.getDocumento(),
                    c.getTelefono(), c.getRegistrada(), c.getUsuario() == null ? "-" : c.getUsuario().getCedula(),
                    c.estaVerificada()));
        }
        almacen.escribir(String.join("\n", lineas) + "\n");
    }

    private void escribirEmpresa(Empresa empresa, List<String> lineas) {
        lineas.add("EMPRESA" + SEPARADOR + Campos.escapar(empresa.getNombre()));
        for (Usuario u : empresa.listarUsuarios()) {
            Horario h = u.getHorario();
            lineas.add(unir("U", u.getRol(), u.getCedula(), u.getNombre(), u.getClaveResumen(),
                    h.getDias(), h.getHoraEntrada(), h.getHoraSalida(), u.estaPresente(),
                    u.getFechaIngreso() == null ? "-" : u.getFechaIngreso(),
                    u.tieneDosPasos() ? u.getSecretoDosPasos() : "-",
                    u.getRespaldosDosPasos().isEmpty() ? "-" : String.join(",", u.getRespaldosDosPasos())));
        }
        for (Obra o : empresa.listarObras()) {
            lineas.add(unir("O", o.getCodigo(), o.getNombre(), o.getUbicacion(), o.getFechaInicio(), o.getFechaFinEstimada(),
                    o.getLatitud() == null ? "-" : o.getLatitud(), o.getLongitud() == null ? "-" : o.getLongitud(),
                    o.getRadioMetros(), o.exigeCodigo(), o.getSecretoCodigo() == null ? "-" : o.getSecretoCodigo()));
        }
        for (Usuario u : empresa.listarUsuarios()) {
            if (u.getObra() != null) {
                lineas.add(unir("Y", u.getCedula(), u.getObra().getCodigo()));
            }
            if (u instanceof Empleado e && e.getSupervisor() != null) {
                lineas.add(unir("S", u.getCedula(), e.getSupervisor().getCedula()));
            }
            for (RegistroAsistencia a : u.getAsistencias()) {
                lineas.add(unir("A", u.getCedula(), a.getFecha(), a.getHoraEntrada(),
                        a.getHoraSalida() == null ? "-" : a.getHoraSalida(), a.isTarde(), a.getEstadoExtra().name(),
                        a.getVerificacion().name(), Campos.escapar(a.getMotivoVerificacion())));
            }
        }
        for (Usuario u : empresa.listarUsuarios()) {
            for (Incidencia i : u.getHistorial().getIncidencias()) {
                lineas.add(escribirIncidencia(i));
            }
        }
        for (Invitacion v : empresa.listarInvitaciones()) {
            lineas.add(unir("V", v.getCodigo(), v.getRol(), v.getSupervisor() == null ? "-" : v.getSupervisor().getCedula(),
                    v.getObra() == null ? "-" : v.getObra().getCodigo(), v.getCreadaPor().getCedula(), v.getCreada(),
                    v.getVence(), v.getEstadoGuardado().name(), v.getUsadaPor() == null ? "-" : v.getUsadaPor().getCorreo(),
                    v.getUsada() == null ? "-" : v.getUsada()));
        }
    }

    private Usuario leerUsuario(String[] c) {
        Usuario usuario = Usuario.restaurar(c[1], c[2], c[3], c[4], Horario.desdeTexto(c[5], c[6], c[7]));
        if (c.length > 9 && !"-".equals(c[9])) {
            usuario.setFechaIngreso(LocalDate.parse(c[9]));
        }
        if (c.length > 10 && !"-".equals(c[10])) {
            usuario.restaurarDosPasos(c[10], c.length > 11 && !"-".equals(c[11]) ? List.of(c[11].split(",")) : List.of());
        }
        return usuario;
    }

    private void leerSupervision(Empresa empresa, String[] c) {
        if (empresa.buscarUsuario(c[1]) instanceof Empleado empleado
                && empresa.buscarUsuario(c[2]) instanceof Supervisor supervisor) {
            empleado.setSupervisor(supervisor);
        }
    }

    private RegistroAsistencia leerAsistencia(String[] c) {
        RegistroAsistencia registro = new RegistroAsistencia(
                LocalDate.parse(c[2]), LocalTime.parse(c[3]), Boolean.parseBoolean(c[5]));
        if (!"-".equals(c[4])) {
            registro.registrarSalida(LocalTime.parse(c[4]));
        }

        registro.restaurarEstadoExtra(c.length > 6 ? EstadoHorasExtra.valueOf(c[6]) : EstadoHorasExtra.NO_APLICA);
        if (c.length > 7) {
            registro.restaurarVerificacion(EstadoVerificacion.valueOf(c[7]), c.length > 8 ? Campos.desescapar(c[8]) : "");
        }
        return registro;
    }

    private Obra leerObra(String[] c) {
        Obra obra = new Obra(c[1], c[2], c[3], LocalDate.parse(c[4]), LocalDate.parse(c[5]));
        if (c.length > 10) {
            obra.restaurarControl("-".equals(c[6]) ? null : Double.valueOf(c[6]), "-".equals(c[7]) ? null : Double.valueOf(c[7]),
                    Integer.parseInt(c[8]), Boolean.parseBoolean(c[9]), "-".equals(c[10]) ? null : c[10]);
        }
        return obra;
    }

    private void leerAsignacionObra(Empresa empresa, String[] c) {
        Usuario usuario = empresa.buscarUsuario(c[1]);
        Obra obra = empresa.buscarObra(c[2]);
        if (usuario != null && obra != null) {
            usuario.setObra(obra);
        }
    }

    private void leerIncidencia(Empresa empresa, String[] c) {
        Usuario afectado = empresa.buscarUsuario(c[3]);
        Usuario reporta = empresa.buscarUsuario(c[4]);
        Map<String, String> extras = Campos.extras(c[13]);
        DatosIncidencia datos = new DatosIncidencia(Integer.parseInt(c[1]), afectado, reporta,
                LocalDate.parse(c[5]), LocalDate.parse(c[6]), Campos.desescapar(c[7]),
                LocalDateTime.parse(c[8]), extras);
        Incidencia incidencia = fabrica.crear(c[2], datos);
        EstadoIncidencia estado = EstadoIncidencia.valueOf(c[9]);
        if (estado != EstadoIncidencia.PENDIENTE) {
            Usuario revisor = empresa.buscarUsuario(c[10]);
            LocalDateTime fecha = c[12].isEmpty() ? null : LocalDateTime.parse(c[12]);
            incidencia.restaurarRevision(estado, revisor, Campos.desescapar(c[11]), fecha);
        }
        afectado.getHistorial().agregarIncidencia(incidencia);
        contadorIds = Math.max(contadorIds, incidencia.getId() + 1);
    }

    private String escribirIncidencia(Incidencia i) {
        Usuario revisor = i.getRevisor();
        return unir("I", i.getId(), i.getCodigoTipo(), i.getAfectado().getCedula(), i.getReportadoPor().getCedula(),
                i.getDesde(), i.getHasta(), Campos.escapar(i.getDescripcion()), i.getFechaReporte(),
                i.getEstado().name(), revisor == null ? "" : revisor.getCedula(),
                Campos.escapar(i.getComentarioRevision()),
                i.getFechaRevision() == null ? "" : i.getFechaRevision(),
                Campos.extrasATexto(i.getExtras()));
    }

    private void leerCuenta(Plataforma plataforma, String[] c) {
        Cuenta cuenta = Cuenta.restaurar(c[1], c[2], c[3], c[4], c[5], c[6], LocalDateTime.parse(c[7]));
        cuenta.restaurarVerificada(c.length <= 9 || Boolean.parseBoolean(c[9]));
        plataforma.agregarCuenta(cuenta);
        Usuario usuario = "-".equals(c[8]) ? null : plataforma.buscarUsuario(c[8]);
        if (usuario != null) {
            usuario.vincularCuenta(cuenta);
        }
    }

    private void leerInvitacion(Empresa empresa, String[] c) {
        Supervisor supervisor = "-".equals(c[3]) ? null : (Supervisor) empresa.buscarUsuario(c[3]);
        Obra obra = "-".equals(c[4]) ? null : empresa.buscarObra(c[4]);
        Gerente gerente = (Gerente) empresa.buscarUsuario(c[5]);
        Invitacion invitacion = new Invitacion(CodigoInvitacion.de(c[1]), c[2], supervisor, obra, gerente,
                LocalDateTime.parse(c[6]), LocalDateTime.parse(c[7]));
        invitacion.restaurar(EstadoInvitacion.valueOf(c[8]), "-".equals(c[9]) ? null : empresa.buscarCuenta(c[9]),
                "-".equals(c[10]) ? null : LocalDateTime.parse(c[10]));
        empresa.agregarInvitacion(invitacion);
    }

    private static String unir(Object... campos) {
        StringBuilder linea = new StringBuilder();
        for (Object campo : campos) {
            linea.append(linea.length() == 0 ? "" : SEPARADOR).append(campo);
        }
        return linea.toString();
    }
}
