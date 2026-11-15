package construtrack.persistencia;

import construtrack.modelo.Empleado;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Guarda y carga la empresa en un archivo de texto (un registro por línea, campos separados por |).
 *
 * <pre>
 *   EMPRESA|nombre
 *   U|rol|cedula|nombre|resumenClave|dias|horaEntrada|horaSalida|presente
 *   S|cedulaEmpleado|cedulaSupervisor
 *   A|cedula|fecha|horaEntrada|horaSalida(o -)|tarde
 * </pre>
 *
 * Si el archivo no existe, pide los datos iniciales a un proveedor externo (por ejemplo, GeneradorDatosPrueba).
 */
public class RepositorioArchivo implements LectorDatos, EscritorDatos {

    private static final String SEPARADOR = "|";

    private final Path archivo;
    private final Supplier<Empresa> datosIniciales;

    public RepositorioArchivo(Path archivo, Supplier<Empresa> datosIniciales) {
        this.archivo = archivo;
        this.datosIniciales = datosIniciales;
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
        for (Usuario u : empresa.listarUsuarios()) {
            if (u instanceof Empleado e && e.getSupervisor() != null) {
                lineas.add(unir("S", u.getCedula(), e.getSupervisor().getCedula()));
            }
            for (RegistroAsistencia a : u.getAsistencias()) {
                lineas.add(unir("A", u.getCedula(), a.getFecha(), a.getHoraEntrada(),
                        a.getHoraSalida() == null ? "-" : a.getHoraSalida(), a.isTarde()));
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

    private static String unir(Object... campos) {
        StringBuilder linea = new StringBuilder();
        for (Object campo : campos) {
            linea.append(linea.length() == 0 ? "" : SEPARADOR).append(campo);
        }
        return linea.toString();
    }
}
