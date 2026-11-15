package servidorobra;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persona registrada en la obra. Es abstracta: cada usuario es un Empleado, un Supervisor o un Jefe.
 */
public abstract class Usuario {

    protected String cedula;
    protected String nombre;
    protected String clave;
    protected Horario horario;
    protected boolean presente;
    protected final List<RegistroAsistencia> asistencias = new ArrayList<>();

    public Usuario(String cedula, String nombre, String clave, Horario horario) {
        setCedula(cedula);
        setNombre(nombre);
        setClave(clave);
        setHorario(horario);
        this.presente = false;
    }

    /** Fábrica: crea el tipo de usuario que corresponde al rol indicado. */
    public static Usuario crear(String rol, String cedula, String nombre, String clave, Horario horario) {
        if ("Empleado".equals(rol)) {
            return new Empleado(cedula, nombre, clave, horario);
        }
        if ("Supervisor".equals(rol)) {
            return new Supervisor(cedula, nombre, clave, horario);
        }
        if ("Jefe".equals(rol)) {
            return new Jefe(cedula, nombre, clave, horario);
        }
        throw new IllegalArgumentException("Rol no válido: " + rol);
    }

    /** Nombre del rol que desempeña en la empresa. */
    public abstract String getRol();

    /** Registra la llegada a la obra y marca al usuario como presente. */
    public RegistroAsistencia marcarEntrada() {
        if (presente) {
            throw new IllegalStateException(nombre + " ya registró su entrada y sigue en la obra.");
        }
        LocalTime ahora = LocalTime.now().withSecond(0).withNano(0);
        RegistroAsistencia registro = new RegistroAsistencia(LocalDate.now(), ahora, horario.esLlegadaTarde(ahora));
        asistencias.add(registro);
        presente = true;
        return registro;
    }

    /** Registra la salida de la obra y marca al usuario como ausente. */
    public RegistroAsistencia marcarSalida() {
        RegistroAsistencia ultima = getUltimaAsistencia();
        if (!presente || ultima == null || !ultima.estaAbierto()) {
            throw new IllegalStateException(nombre + " no tiene una entrada abierta.");
        }
        ultima.registrarSalida(LocalTime.now().withSecond(0).withNano(0));
        presente = false;
        return ultima;
    }

    public boolean estaPresente() {
        return presente;
    }

    /** Devuelve la jornada del día de hoy, o null si hoy no ha entrado. */
    public RegistroAsistencia getAsistenciaDeHoy() {
        RegistroAsistencia ultima = getUltimaAsistencia();
        return ultima != null && ultima.getFecha().equals(LocalDate.now()) ? ultima : null;
    }

    public RegistroAsistencia getUltimaAsistencia() {
        return asistencias.isEmpty() ? null : asistencias.get(asistencias.size() - 1);
    }

    /** Indica si hoy es un día laborable según su horario. */
    public boolean trabajaHoy() {
        return horario.incluyeDia(LocalDate.now().getDayOfWeek());
    }

    public boolean verificarClave(String clave) {
        return this.clave.equals(clave);
    }

    /** Copia el estado de asistencia de otro usuario (se usa cuando el Jefe le cambia el rol). */
    public void copiarAsistenciaDe(Usuario otro) {
        asistencias.clear();
        asistencias.addAll(otro.asistencias);
        presente = otro.presente;
    }

    /** Agrega una jornada guardada en la base de datos. */
    public void agregarAsistencia(RegistroAsistencia registro) {
        asistencias.add(registro);
    }

    public List<RegistroAsistencia> getAsistencias() {
        return Collections.unmodifiableList(asistencias);
    }

    public String getCedula() {
        return cedula;
    }

    public final void setCedula(String cedula) {
        if (cedula == null || !cedula.matches("\\d{6,9}")) {
            throw new IllegalArgumentException("La cédula debe tener entre 6 y 9 dígitos, sin puntos ni letras.");
        }
        this.cedula = cedula;
    }

    public String getNombre() {
        return nombre;
    }

    public final void setNombre(String nombre) {
        this.nombre = validarNombre(nombre);
    }

    /** Devuelve el nombre sin espacios sobrantes, o lanza un error si no es válido. */
    public static String validarNombre(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (!limpio.matches("[\\p{L} .'-]{3,60}")) {
            throw new IllegalArgumentException("El nombre debe tener entre 3 y 60 letras.");
        }
        return limpio;
    }

    public final void setClave(String clave) {
        validarClave(clave);
        this.clave = clave;
    }

    public static void validarClave(String clave) {
        if (clave == null || !clave.matches("[^|\\s]{4,30}")) {
            throw new IllegalArgumentException("La clave debe tener entre 4 y 30 caracteres, sin espacios.");
        }
    }

    /** Solo la usa la base de datos para guardar el archivo; nunca se envía a la página. */
    String getClave() {
        return clave;
    }

    public Horario getHorario() {
        return horario;
    }

    public final void setHorario(Horario horario) {
        if (horario == null) {
            throw new IllegalArgumentException("El horario es obligatorio.");
        }
        this.horario = horario;
    }

    public void setPresente(boolean presente) {
        this.presente = presente;
    }

    /** Datos del usuario en JSON. Con detalle incluye su historial de asistencia. */
    public String toJson(boolean detalle) {
        RegistroAsistencia hoy = getAsistenciaDeHoy();
        StringBuilder json = new StringBuilder("{")
                .append("\"cedula\":").append(Json.texto(cedula))
                .append(",\"nombre\":").append(Json.texto(nombre))
                .append(",\"rol\":").append(Json.texto(getRol()))
                .append(",\"presente\":").append(presente)
                .append(",\"trabajaHoy\":").append(trabajaHoy())
                .append(",\"horario\":").append(horario.toJson())
                .append(",\"asistenciaHoy\":").append(hoy == null ? "null" : hoy.toJson())
                .append(datosExtraJson());
        if (detalle) {
            json.append(",\"asistencias\":[");
            // Las más recientes primero, máximo 30
            for (int i = asistencias.size() - 1, n = 0; i >= 0 && n < 30; i--, n++) {
                json.append(n == 0 ? "" : ",").append(asistencias.get(i).toJson());
            }
            json.append("]");
        }
        return json.append("}").toString();
    }

    /** Las subclases agregan aquí sus propios campos JSON, empezando con coma. */
    protected String datosExtraJson() {
        return "";
    }
}
