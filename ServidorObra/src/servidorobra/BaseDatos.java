package servidorobra;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Guarda y carga los datos de la empresa en un archivo de texto.
 *
 * Formato (un registro por línea, campos separados por |):
 *   EMPRESA|nombre
 *   U|rol|cedula|nombre|clave|dias|horaEntrada|horaSalida|presente
 *   S|cedulaEmpleado|cedulaSupervisor
 *   A|cedula|fecha|horaEntrada|horaSalida(o -)|tarde
 */
public class BaseDatos {

    private final Path archivo;

    public BaseDatos(String rutaArchivo) {
        this.archivo = Paths.get(rutaArchivo);
    }

    /** Carga la empresa desde el archivo. Si el archivo no existe, crea los datos de prueba. */
    public Empresa cargar() {
        if (!Files.exists(archivo)) {
            Empresa empresa = crearDatosIniciales();
            guardar(empresa);
            return empresa;
        }
        try {
            List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
            Empresa empresa = null;
            for (String linea : lineas) {
                String[] c = linea.split("\\|", -1);
                switch (c[0]) {
                    case "EMPRESA":
                        empresa = new Empresa(c[1]);
                        break;
                    case "U":
                        Usuario usuario = Usuario.crear(c[1], c[2], c[3], c[4], Horario.desdeTexto(c[5], c[6], c[7]));
                        usuario.setPresente(Boolean.parseBoolean(c[8]));
                        empresa.agregarUsuario(usuario);
                        break;
                    case "S":
                        Usuario empleado = empresa.buscarUsuario(c[1]);
                        Usuario supervisor = empresa.buscarUsuario(c[2]);
                        if (empleado instanceof Empleado && supervisor instanceof Supervisor) {
                            ((Empleado) empleado).setSupervisor((Supervisor) supervisor);
                        }
                        break;
                    case "A":
                        RegistroAsistencia registro = new RegistroAsistencia(
                                LocalDate.parse(c[2]), LocalTime.parse(c[3]), Boolean.parseBoolean(c[5]));
                        if (!"-".equals(c[4])) {
                            registro.registrarSalida(LocalTime.parse(c[4]));
                        }
                        empresa.buscarUsuario(c[1]).agregarAsistencia(registro);
                        break;
                    default:
                        // Líneas vacías o desconocidas se ignoran
                }
            }
            return empresa;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + archivo.toAbsolutePath(), e);
        }
    }

    /** Escribe todos los datos de la empresa en el archivo. */
    public void guardar(Empresa empresa) {
        List<String> lineas = new ArrayList<>();
        lineas.add("EMPRESA|" + empresa.getNombre());
        for (Usuario u : empresa.listarUsuarios()) {
            Horario h = u.getHorario();
            lineas.add(String.join("|", "U", u.getRol(), u.getCedula(), u.getNombre(), u.getClave(),
                    h.getDias(), h.getHoraEntrada().toString(), h.getHoraSalida().toString(),
                    String.valueOf(u.estaPresente())));
        }
        for (Usuario u : empresa.listarUsuarios()) {
            if (u instanceof Empleado && ((Empleado) u).getSupervisor() != null) {
                lineas.add("S|" + u.getCedula() + "|" + ((Empleado) u).getSupervisor().getCedula());
            }
            for (RegistroAsistencia a : u.getAsistencias()) {
                lineas.add(String.join("|", "A", u.getCedula(), a.getFecha().toString(),
                        a.getHoraEntrada().toString(),
                        a.getHoraSalida() == null ? "-" : a.getHoraSalida().toString(),
                        String.valueOf(a.isTarde())));
            }
        }
        try {
            Files.write(archivo, lineas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar " + archivo.toAbsolutePath(), e);
        }
    }

    /** Personal de prueba: 1 jefe, 2 supervisores y 6 empleados (claves de demostración). */
    private Empresa crearDatosIniciales() {
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
        LocalDate hoy = LocalDate.now();
        for (int i = 0; i < empleados.length; i++) {
            Empleado e = new Empleado(empleados[i][0], empleados[i][1], "emp123", i < 3 ? obra : sabados);
            e.setSupervisor(i < 3 ? ana : luis);
            empresa.agregarUsuario(e);
            // Asistencia de ejemplo: ayer completa; hoy unos presentes (uno llegó tarde) y otros ausentes
            e.agregarAsistencia(crearRegistro(hoy.minusDays(1), "06:55", "16:02", false));
            if (i % 2 == 0) {
                e.agregarAsistencia(crearRegistro(hoy, i == 4 ? "07:25" : "06:58", null, i == 4));
                e.setPresente(true);
            }
        }
        return empresa;
    }

    private RegistroAsistencia crearRegistro(LocalDate fecha, String entrada, String salida, boolean tarde) {
        RegistroAsistencia registro = new RegistroAsistencia(fecha, LocalTime.parse(entrada), tarde);
        if (salida != null) {
            registro.registrarSalida(LocalTime.parse(salida));
        }
        return registro;
    }
}
