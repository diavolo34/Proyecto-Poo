package construtrack.persistencia;

import construtrack.modelo.Empleado;
import construtrack.modelo.Obra;
import construtrack.modelo.incidencias.DatosIncidencia;
import construtrack.modelo.incidencias.EstadoIncidencia;
import construtrack.modelo.incidencias.FabricaIncidencias;
import construtrack.modelo.incidencias.Incidencia;
import construtrack.modelo.Empresa;
import construtrack.modelo.Horario;
import construtrack.modelo.RegistroAsistencia;
import construtrack.modelo.Supervisor;
import construtrack.modelo.Usuario;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Guarda y carga la empresa en un archivo de texto (un registro por línea, campos separados por |).
 *
 * <pre>
 *   EMPRESA|nombre
 *   U|rol|cedula|nombre|resumenClave|dias|horaEntrada|horaSalida|presente
 *   S|cedulaEmpleado|cedulaSupervisor
 *   A|cedula|fecha|horaEntrada|horaSalida(o -)|tarde
 *   O|codigo|nombre|ubicacion|fechaInicio|fechaFinEstimada
 *   Y|cedulaEmpleado|cedulaObra
 *   I|id|tipo|cedulaAfectado|cedulaReporta|desde|hasta|descripcion|fechaReporte|estado|cedulaRevisor|comentario|fechaRevision|extras
 * </pre>
 *
 * Los campos de texto libre se escapan (\\p, \\n, \\\\) para que el separador | y los saltos de línea no rompan el archivo.
 *
 * Si el archivo no existe, pide los datos iniciales a un proveedor externo (por ejemplo, GeneradorDatosPrueba).
 */
public class RepositorioArchivo implements LectorDatos, EscritorDatos {

    private static final String SEPARADOR = "|";

    private final Path archivo;
    private final Supplier<Empresa> datosIniciales;
    private final FabricaIncidencias fabrica;
    private int contadorIds = 1;

    public RepositorioArchivo(Path archivo, Supplier<Empresa> datosIniciales, FabricaIncidencias fabrica) {
        this.archivo = archivo;
        this.datosIniciales = datosIniciales;
        this.fabrica = fabrica;
    }

    /** Siguiente número de incidencia libre según lo cargado (evita repetir ids al reiniciar). */
    public int siguienteIdIncidencia() {
        return contadorIds;
    }

    @Override
    public Empresa cargar() {
        if (!Files.exists(archivo)) {
            Empresa empresa = datosIniciales.get();
            guardar(empresa);
            return empresa;
        }
        try {
            Empresa empresa = null;
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                String[] c = linea.split("\\|", -1);
                switch (c[0]) {
                    case "EMPRESA" -> empresa = new Empresa(c[1]);
                    case "U" -> empresa.agregarUsuario(leerUsuario(c));
                    case "S" -> leerSupervision(empresa, c);
                    case "A" -> empresa.buscarUsuario(c[1]).agregarAsistencia(leerAsistencia(c));
                    case "O" -> empresa.agregarObra(leerObra(c));
                    case "Y" -> leerAsignacionObra(empresa, c);
                    case "I" -> leerIncidencia(empresa, c);
                    default -> {
                        // Líneas vacías o de versiones futuras: se ignoran
                    }
                }
            }
            return empresa;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + archivo.toAbsolutePath(), e);
        }
    }

    @Override
    public void guardar(Empresa empresa) {
        List<String> lineas = new ArrayList<>();
        lineas.add("EMPRESA" + SEPARADOR + empresa.getNombre());
        for (Usuario u : empresa.listarUsuarios()) {
            Horario h = u.getHorario();
            lineas.add(unir("U", u.getRol(), u.getCedula(), u.getNombre(), u.getClaveResumen(),
                    h.getDias(), h.getHoraEntrada(), h.getHoraSalida(), u.estaPresente()));
        }
        for (Obra o : empresa.listarObras()) {
            lineas.add(unir("O", o.getCodigo(), o.getNombre(), o.getUbicacion(), o.getFechaInicio(), o.getFechaFinEstimada()));
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
                        a.getHoraSalida() == null ? "-" : a.getHoraSalida(), a.isTarde()));
            }
        }
        for (Usuario u : empresa.listarUsuarios()) {
            for (Incidencia i : u.getHistorial().getIncidencias()) {
                lineas.add(escribirIncidencia(i));
            }
        }
        try {
            Files.write(archivo, lineas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar " + archivo.toAbsolutePath(), e);
        }
    }

    private Usuario leerUsuario(String[] c) {
        Usuario usuario = Usuario.crear(c[1], c[2], c[3], "temporal", Horario.desdeTexto(c[5], c[6], c[7]));
        usuario.restaurarClaveResumen(c[4]);
        usuario.setPresente(Boolean.parseBoolean(c[8]));
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
        return registro;
    }

    private Obra leerObra(String[] c) {
        return new Obra(c[1], c[2], c[3], LocalDate.parse(c[4]), LocalDate.parse(c[5]));
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

    private static String unir(Object... campos) {
        StringBuilder linea = new StringBuilder();
        for (Object campo : campos) {
            linea.append(linea.length() == 0 ? "" : SEPARADOR).append(campo);
        }
        return linea.toString();
    }
}
