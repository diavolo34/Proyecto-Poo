package construtrack.persistencia;

import construtrack.modelo.ArchivoAdjunto;
import construtrack.modelo.Area;
import construtrack.modelo.CodigoInvitacion;
import construtrack.modelo.Cuenta;
import construtrack.modelo.Empleado;
import construtrack.modelo.Empresa;
import construtrack.modelo.EstadoHorasExtra;
import construtrack.modelo.EstadoInvitacion;
import construtrack.modelo.EstadoObra;
import construtrack.modelo.EstadoVerificacion;
import construtrack.modelo.Firma;
import construtrack.modelo.Gerente;
import construtrack.modelo.Horario;
import construtrack.modelo.Invitacion;
import construtrack.modelo.Obra;
import construtrack.modelo.Plataforma;
import construtrack.modelo.RegistroAsistencia;
import construtrack.modelo.RetrasoObra;
import construtrack.modelo.Supervisor;
import construtrack.modelo.Usuario;
import construtrack.modelo.inventario.Articulo;
import construtrack.modelo.inventario.Asignacion;
import construtrack.modelo.inventario.CategoriaArticulo;
import construtrack.modelo.inventario.CondicionDevolucion;
import construtrack.modelo.inventario.EstadoArticulo;
import construtrack.modelo.inventario.Inventario;
import construtrack.modelo.novedades.DatosNovedad;
import construtrack.modelo.novedades.EstadoNovedad;
import construtrack.modelo.novedades.FabricaNovedades;
import construtrack.modelo.novedades.Novedad;
import construtrack.modelo.talento.Egreso;
import construtrack.modelo.talento.TipoEgreso;
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
    private static final String NULO = "-";

    private final Almacen almacen;
    private final Supplier<Empresa> datosIniciales;
    private final FabricaNovedades fabrica;
    private int contadorIds = 1;

    public RepositorioTexto(Almacen almacen, Supplier<Empresa> datosIniciales, FabricaNovedades fabrica) {
        this.almacen = almacen;
        this.datosIniciales = datosIniciales;
        this.fabrica = fabrica;
    }

    public RepositorioTexto(Path archivo, Supplier<Empresa> datosIniciales, FabricaNovedades fabrica) {
        this(new AlmacenArchivo(archivo), datosIniciales, fabrica);
    }

    public Almacen getAlmacen() {
        return almacen;
    }

    public int siguienteIdNovedad() {
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
                case "K" -> leerCatalogo(empresa, c);
                case "AR" -> leerArea(empresa, c);
                case "U" -> empresa.agregarUsuario(leerUsuario(c));
                case "S" -> leerSupervision(empresa, c);
                case "A" -> empresa.buscarUsuario(c[1]).agregarAsistencia(leerAsistencia(c));
                case "O" -> empresa.agregarObra(leerObra(c));
                case "R" -> leerRetraso(empresa, c);
                case "Y" -> leerAsignacionObra(empresa, c);
                case "I" -> leerIncidenciaAnterior(empresa, c);
                case "N" -> leerNovedad(empresa, c);
                case "H" -> leerArticulo(empresa.getInventario(), c);
                case "G" -> leerEntrega(empresa.getInventario(), c);
                case "E" -> leerEgreso(empresa, c);
                case "F" -> leerArchivo(empresa, c);
                case "C" -> cuentas.add(c);
                case "V" -> invitaciones.put(c, empresa);
                default -> {
                }
            }
        }
        cuentas.forEach(c -> leerCuenta(plataforma, c));
        invitaciones.forEach((c, e) -> leerInvitacion(e, c));
        plataforma.listarEmpresas().forEach(Empresa::depurarAreas);
        return plataforma;
    }

    private void actualizarContador(Plataforma plataforma) {
        for (Empresa empresa : plataforma.listarEmpresas()) {
            for (Usuario u : empresa.listarTodos()) {
                for (Novedad n : u.getHistorial().getNovedades()) {
                    contadorIds = Math.max(contadorIds, n.getId() + 1);
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
                    c.getTelefono(), c.getRegistrada(), c.getUsuario() == null ? NULO : c.getUsuario().getCedula(),
                    c.estaVerificada()));
        }
        almacen.escribir(String.join("\n", lineas) + "\n");
    }

    private void escribirEmpresa(Empresa empresa, List<String> lineas) {
        lineas.add("EMPRESA" + SEPARADOR + Campos.escapar(empresa.getNombre()));
        lineas.add(unir("K", NULO));
        empresa.getCatalogo().getPuestos().forEach(p -> lineas.add(unir("K", "P", Campos.escapar(p))));
        for (Usuario u : empresa.listarTodos()) {
            Horario h = u.getHorario();
            lineas.add(unir("U", u.getRol(), u.getCedula(), u.getNombre(), u.getClaveResumen(),
                    h.getDias(), h.getHoraEntrada(), h.getHoraSalida(), u.estaPresente(), nulo(u.getFechaIngreso()),
                    u.tieneDosPasos() ? u.getSecretoDosPasos() : NULO,
                    u.getRespaldosDosPasos().isEmpty() ? NULO : String.join(",", u.getRespaldosDosPasos()),
                    u.estaActivo(), nulo(u.getFechaEgreso()), texto(u.getPuesto()), texto(u.getArea())));
        }
        for (Area a : empresa.listarAreas()) {
            lineas.add(unir("AR", Campos.escapar(a.getNombre()), a.getSupervisor() == null ? NULO : a.getSupervisor().getCedula()));
        }
        for (Obra o : empresa.listarObras()) {
            lineas.add(unir("O", o.getCodigo(), o.getNombre(), o.getUbicacion(), o.getFechaInicio(), o.getFechaFinEstimada(),
                    nulo(o.getLatitud()), nulo(o.getLongitud()), o.getRadioMetros(), o.exigeCodigo(), nulo(o.getSecretoCodigo()),
                    o.getEstado().name(), nulo(o.getFechaInicioReal()), nulo(o.getFechaConclusion())));
            for (RetrasoObra r : o.getRetrasos()) {
                lineas.add(unir("R", o.getCodigo(), r.getId(), r.getFecha(), firma(r.getReportadoPor()),
                        Campos.escapar(r.getMotivo()), r.getFinAnterior(), r.getFinNuevo(), firma(r.getRevisadoPor()),
                        nulo(r.getFechaRevision())));
            }
        }
        for (Usuario u : empresa.listarTodos()) {
            if (u.getObra() != null) {
                lineas.add(unir("Y", u.getCedula(), u.getObra().getCodigo()));
            }
            if (u instanceof Empleado e && e.getSupervisor() != null) {
                lineas.add(unir("S", u.getCedula(), e.getSupervisor().getCedula()));
            }
            for (RegistroAsistencia a : u.getAsistencias()) {
                lineas.add(unir("A", u.getCedula(), a.getFecha(), a.getHoraEntrada(), nulo(a.getHoraSalida()), a.isTarde(),
                        a.getEstadoExtra().name(), a.getVerificacion().name(), Campos.escapar(a.getMotivoVerificacion()),
                        ubicacion(a.getUbicacionEntrada()), ubicacion(a.getUbicacionSalida())));
            }
        }
        for (Usuario u : empresa.listarTodos()) {
            for (Novedad n : u.getHistorial().getNovedades()) {
                lineas.add(escribirNovedad(n));
            }
        }
        Inventario inventario = empresa.getInventario();
        for (Articulo a : inventario.listar()) {
            lineas.add(unir("H", a.getCodigo(), a.getCategoria().name(), Campos.escapar(a.getNombre()), Campos.escapar(a.getMarca()),
                    Campos.escapar(a.getSerie()), a.getCantidad(), a.getEstado().name(), Campos.escapar(a.getNota())));
        }
        for (Asignacion g : inventario.asignaciones()) {
            lineas.add(unir("G", g.getId(), g.getArticulo().getCodigo(), firma(g.getPersona()), g.getCantidad(), g.getFecha(),
                    firma(g.getEntregadoPor()), texto(g.getObra()), nulo(g.getDevolucionPrevista()), Campos.escapar(g.getNota()),
                    nulo(g.getFechaDevolucion()), firma(g.getRecibidoPor()),
                    g.getCondicion() == null ? NULO : g.getCondicion().name(), Campos.escapar(g.getNotaDevolucion())));
        }
        for (Egreso e : empresa.listarEgresos()) {
            lineas.add(unir("E", e.getId(), e.getCedula(), Campos.escapar(e.getNombre()), e.getRol(), texto(e.getPuesto()),
                    texto(e.getArea()), nulo(e.getFechaIngreso()), e.getTipo().name(), e.getFecha(), Campos.escapar(e.getMotivo()),
                    firma(e.getRegistradoPor()), e.getFechaRegistro(), e.getVoucher(), nulo(e.getCarta()), e.devolvioEquipos(),
                    Campos.escapar(e.getNotaEquipos()), nulo(e.getFichaEliminada()), texto(e.getExpediente())));
        }
        for (ArchivoAdjunto f : empresa.listarArchivos()) {
            lineas.add(unir("F", f.id(), Campos.escapar(f.nombre()), f.tipo(), f.tamano(), f.fecha(), firma(f.subidoPor()),
                    nulo(f.cedulaPersona()), f.confidencial()));
        }
        for (Invitacion v : empresa.listarInvitaciones()) {
            lineas.add(unir("V", v.getCodigo(), v.getRol(), v.getSupervisor() == null ? NULO : v.getSupervisor().getCedula(),
                    v.getObra() == null ? NULO : v.getObra().getCodigo(), v.getCreadaPor().getCedula(), v.getCreada(),
                    v.getVence(), v.getEstadoGuardado().name(), v.getUsadaPor() == null ? NULO : v.getUsadaPor().getCorreo(),
                    nulo(v.getUsada()), texto(v.getPuesto()), texto(v.getArea())));
        }
    }

    private static String nulo(Object valor) {
        return valor == null ? NULO : valor.toString();
    }

    private static String texto(String valor) {
        return valor == null ? NULO : Campos.escapar(valor);
    }

    private static String leerTexto(String[] c, int i) {
        return c.length <= i || NULO.equals(c[i]) ? null : Campos.desescapar(c[i]);
    }

    private static String firma(Firma f) {
        return f == null ? NULO + SEPARADOR + NULO : f.cedula() + SEPARADOR + Campos.escapar(f.nombre());
    }

    private static Firma leerFirma(String[] c, int i) {
        return NULO.equals(c[i]) ? null : new Firma(c[i], Campos.desescapar(c[i + 1]));
    }

    private static String ubicacion(RegistroAsistencia.Ubicacion u) {
        return u == null ? String.join(SEPARADOR, NULO, NULO, NULO)
                : String.join(SEPARADOR, String.valueOf(u.latitud()), String.valueOf(u.longitud()), nulo(u.precision()));
    }

    private static Double numero(String[] c, int i) {
        return c.length <= i || NULO.equals(c[i]) ? null : Double.valueOf(c[i]);
    }

    private static LocalDate fecha(String[] c, int i) {
        return c.length <= i || NULO.equals(c[i]) || c[i].isEmpty() ? null : LocalDate.parse(c[i]);
    }

    private static LocalDateTime momento(String[] c, int i) {
        return c.length <= i || NULO.equals(c[i]) || c[i].isEmpty() ? null : LocalDateTime.parse(c[i]);
    }

    private void leerCatalogo(Empresa empresa, String[] c) {
        switch (c[1]) {
            case NULO -> empresa.getCatalogo().vaciar();
            case "P" -> empresa.getCatalogo().agregarPuesto(Campos.desescapar(c[2]));
            default -> {
            }
        }
    }

    private void leerArea(Empresa empresa, String[] c) {
        Supervisor supervisor = NULO.equals(c[2]) || !(empresa.buscarUsuario(c[2]) instanceof Supervisor s) ? null : s;
        empresa.restaurarArea(Campos.desescapar(c[1]), supervisor);
    }

    private Usuario leerUsuario(String[] c) {
        Usuario usuario = Usuario.restaurar(c[1], c[2], c[3], c[4], Horario.desdeTexto(c[5], c[6], c[7]));
        usuario.setFechaIngreso(fecha(c, 9));
        if (c.length > 10 && !NULO.equals(c[10])) {
            usuario.restaurarDosPasos(c[10], c.length > 11 && !NULO.equals(c[11]) ? List.of(c[11].split(",")) : List.of());
        }
        if (c.length > 12 && !Boolean.parseBoolean(c[12])) {
            usuario.restaurarEgreso(fecha(c, 13));
        }
        usuario.setPuesto(leerTexto(c, 14));
        usuario.setArea(leerTexto(c, 15));
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
        if (!NULO.equals(c[4])) {
            registro.registrarSalida(LocalTime.parse(c[4]));
        }
        registro.restaurarEstadoExtra(c.length > 6 ? EstadoHorasExtra.valueOf(c[6]) : EstadoHorasExtra.NO_APLICA);
        if (c.length > 7) {
            registro.restaurarVerificacion(EstadoVerificacion.valueOf(c[7]), c.length > 8 ? Campos.desescapar(c[8]) : "");
        }
        registro.registrarUbicacionEntrada(numero(c, 9), numero(c, 10), numero(c, 11));
        registro.registrarUbicacionSalida(numero(c, 12), numero(c, 13), numero(c, 14));
        return registro;
    }

    private Obra leerObra(String[] c) {
        Obra obra = new Obra(c[1], c[2], c[3], LocalDate.parse(c[4]), LocalDate.parse(c[5]));
        if (c.length > 10) {
            obra.restaurarControl(numero(c, 6), numero(c, 7), Integer.parseInt(c[8]), Boolean.parseBoolean(c[9]),
                    NULO.equals(c[10]) ? null : c[10]);
        }
        EstadoObra estado = c.length > 11 ? EstadoObra.valueOf(c[11]) : EstadoObra.EN_CURSO;
        obra.restaurarCiclo(estado, fecha(c, 12), fecha(c, 13));
        return obra;
    }

    private void leerRetraso(Empresa empresa, String[] c) {
        Obra obra = empresa.buscarObra(c[1]);
        RetrasoObra retraso = new RetrasoObra(Integer.parseInt(c[2]), LocalDateTime.parse(c[3]), leerFirma(c, 4),
                Campos.desescapar(c[6]), LocalDate.parse(c[7]), LocalDate.parse(c[8]));
        retraso.restaurarRevision(leerFirma(c, 9), momento(c, 11));
        obra.agregarRetraso(retraso);
    }

    private void leerAsignacionObra(Empresa empresa, String[] c) {
        Usuario usuario = empresa.buscarUsuario(c[1]);
        Obra obra = empresa.buscarObra(c[2]);
        if (usuario != null && obra != null) {
            usuario.setObra(obra);
        }
    }

    private static Firma firmaDe(Empresa empresa, String cedula) {
        if (cedula == null || cedula.isEmpty() || NULO.equals(cedula)) {
            return null;
        }
        Usuario u = empresa.buscarUsuario(cedula);
        return u == null ? new Firma(cedula, cedula) : Firma.de(u);
    }

    private void leerIncidenciaAnterior(Empresa empresa, String[] c) {
        Usuario afectado = empresa.buscarUsuario(c[3]);
        if (afectado == null) {
            return;
        }
        Firma reporta = firmaDe(empresa, c[4]);
        DatosNovedad datos = new DatosNovedad(Integer.parseInt(c[1]), afectado, reporta == null ? Firma.DESCONOCIDA : reporta,
                LocalDate.parse(c[5]), LocalDate.parse(c[6]), Campos.desescapar(c[7]), LocalDateTime.parse(c[8]),
                Campos.extras(c[13]));
        Novedad novedad = fabrica.crear(c[2], datos);
        EstadoNovedad estado = EstadoNovedad.desdeTexto(c[9]);
        if (estado != EstadoNovedad.PENDIENTE) {
            novedad.restaurarRevision(estado, firmaDe(empresa, c[10]), Campos.desescapar(c[11]), momento(c, 12));
        }
        afectado.getHistorial().agregarNovedad(novedad);
        contadorIds = Math.max(contadorIds, novedad.getId() + 1);
    }

    private String escribirNovedad(Novedad n) {
        return unir("N", n.getId(), n.getCodigoTipo(), n.getAfectado().getCedula(), firma(n.getRegistradaPor()),
                n.getDesde(), n.getHasta(), Campos.escapar(n.getDescripcion()), n.getFechaRegistro(), n.getEstado().name(),
                firma(n.getRevisadaPor()), Campos.escapar(n.getNota()), nulo(n.getFechaRevision()),
                Campos.extrasATexto(n.getExtras()));
    }

    private void leerNovedad(Empresa empresa, String[] c) {
        Usuario afectado = empresa.buscarUsuario(c[3]);
        if (afectado == null) {
            return;
        }
        Firma registra = leerFirma(c, 4);
        DatosNovedad datos = new DatosNovedad(Integer.parseInt(c[1]), afectado, registra == null ? Firma.DESCONOCIDA : registra,
                LocalDate.parse(c[6]), LocalDate.parse(c[7]), Campos.desescapar(c[8]), LocalDateTime.parse(c[9]),
                Campos.extras(c[15]));
        Novedad novedad = fabrica.crear(c[2], datos);
        EstadoNovedad estado = EstadoNovedad.desdeTexto(c[10]);
        if (estado != EstadoNovedad.PENDIENTE) {
            novedad.restaurarRevision(estado, leerFirma(c, 11), Campos.desescapar(c[13]), momento(c, 14));
        }
        afectado.getHistorial().agregarNovedad(novedad);
        contadorIds = Math.max(contadorIds, novedad.getId() + 1);
    }

    private void leerArticulo(Inventario inventario, String[] c) {
        Articulo articulo = new Articulo(c[1], CategoriaArticulo.valueOf(c[2]), Campos.desescapar(c[3]),
                Campos.desescapar(c[4]), Campos.desescapar(c[5]), Integer.parseInt(c[6]), Campos.desescapar(c[8]));
        inventario.restaurar(articulo, EstadoArticulo.valueOf(c[7]));
    }

    private void leerEntrega(Inventario inventario, String[] c) {
        Asignacion asignacion = new Asignacion(Integer.parseInt(c[1]), inventario.buscar(c[2]), leerFirma(c, 3),
                Integer.parseInt(c[5]), LocalDateTime.parse(c[6]), leerFirma(c, 7), leerTexto(c, 9), fecha(c, 10),
                Campos.desescapar(c[11]));
        if (!NULO.equals(c[12])) {
            asignacion.restaurarDevolucion(LocalDateTime.parse(c[12]), leerFirma(c, 13), CondicionDevolucion.valueOf(c[15]),
                    Campos.desescapar(c[16]));
        }
        inventario.restaurarAsignacion(asignacion);
    }

    private void leerEgreso(Empresa empresa, String[] c) {
        Egreso egreso = new Egreso(Integer.parseInt(c[1]), c[2], Campos.desescapar(c[3]), c[4], leerTexto(c, 5),
                leerTexto(c, 6), fecha(c, 7), TipoEgreso.valueOf(c[8]), LocalDate.parse(c[9]), Campos.desescapar(c[10]),
                leerFirma(c, 11), LocalDateTime.parse(c[13]), c[14], NULO.equals(c[15]) ? null : c[15],
                Boolean.parseBoolean(c[16]), Campos.desescapar(c[17]));
        if (!NULO.equals(c[18])) {
            egreso.restaurarEliminacion(LocalDateTime.parse(c[18]), leerTexto(c, 19));
        }
        empresa.restaurarEgreso(egreso);
    }

    private void leerArchivo(Empresa empresa, String[] c) {
        empresa.agregarArchivo(new ArchivoAdjunto(c[1], Campos.desescapar(c[2]), c[3], Long.parseLong(c[4]),
                LocalDateTime.parse(c[5]), leerFirma(c, 6), NULO.equals(c[8]) ? null : c[8], Boolean.parseBoolean(c[9])));
    }

    private void leerCuenta(Plataforma plataforma, String[] c) {
        Cuenta cuenta = Cuenta.restaurar(c[1], c[2], c[3], c[4], c[5], c[6], LocalDateTime.parse(c[7]));
        cuenta.restaurarVerificada(c.length <= 9 || Boolean.parseBoolean(c[9]));
        plataforma.agregarCuenta(cuenta);
        Usuario usuario = NULO.equals(c[8]) ? null : plataforma.buscarUsuario(c[8]);
        if (usuario != null) {
            usuario.vincularCuenta(cuenta);
        }
    }

    private void leerInvitacion(Empresa empresa, String[] c) {
        Supervisor supervisor = NULO.equals(c[3]) || !(empresa.buscarUsuario(c[3]) instanceof Supervisor s) ? null : s;
        Obra obra = NULO.equals(c[4]) ? null : empresa.buscarObra(c[4]);
        if (!(empresa.buscarUsuario(c[5]) instanceof Gerente gerente)) {
            return;
        }
        Invitacion invitacion = new Invitacion(CodigoInvitacion.de(c[1]), c[2], supervisor, obra, gerente,
                LocalDateTime.parse(c[6]), LocalDateTime.parse(c[7]));
        invitacion.restaurar(EstadoInvitacion.valueOf(c[8]), NULO.equals(c[9]) ? null : empresa.buscarCuenta(c[9]),
                momento(c, 10));
        invitacion.asignarPuesto(leerTexto(c, 11), leerTexto(c, 12));
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
