package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.incidencias.EstadoIncidencia;
import construtrack.modelo.incidencias.Incidencia;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Persona registrada en la empresa. Es abstracta: cada usuario es un Empleado, un Supervisor o un Jefe.
 * Las subclases amplían lo que puede hacer, sin romper lo que promete la clase base
 * (cualquiera puede marcar asistencia y reportar sus propias incidencias).
 */
public abstract class Usuario {

    private String cedula;
    private String nombre;
    private String claveResumen;
    private Horario horario;
    private boolean presente;
    private Obra obra;
    private Cuenta cuenta;
    private final Historial historial = new Historial();

    protected Usuario(String cedula, String nombre, String clave, Horario horario) {
        setCedula(cedula);
        setNombre(nombre);
        setClave(clave);
        setHorario(horario);
    }

    /** Fábrica: crea el tipo de usuario que corresponde al rol indicado. */
    public static Usuario crear(String rol, String cedula, String nombre, String clave, Horario horario) {
        if (rol == null) {
            throw new DatoInvalidoException("Selecciona un rol.");
        }
        return switch (rol) {
            case Empleado.ROL -> new Empleado(cedula, nombre, clave, horario);
            case Supervisor.ROL -> new Supervisor(cedula, nombre, clave, horario);
            case Jefe.ROL -> new Jefe(cedula, nombre, clave, horario);
            default -> throw new DatoInvalidoException("Rol no válido: " + rol);
        };
    }

    /** Nombre del rol que desempeña en la empresa. */
    public abstract String getRol();

    // ------------------------------------------------------------------ asistencia

    /** Registra la llegada a la obra y marca al usuario como presente. */
    public RegistroAsistencia marcarEntrada(LocalDateTime ahora) {
        if (presente) {
            throw new OperacionInvalidaException(nombre + " ya registró su entrada y sigue en la obra.");
        }
        LocalTime hora = ahora.toLocalTime().withSecond(0).withNano(0);
        RegistroAsistencia registro = new RegistroAsistencia(ahora.toLocalDate(), hora, horario.esLlegadaTarde(hora));
        historial.agregarAsistencia(registro);
        presente = true;
        return registro;
    }

    /** Registra la salida de la obra y marca al usuario como ausente. */
    public RegistroAsistencia marcarSalida(LocalDateTime ahora) {
        RegistroAsistencia ultima = historial.ultimaAsistencia();
        if (!presente || ultima == null || !ultima.estaAbierto()) {
            throw new OperacionInvalidaException(nombre + " no tiene una entrada abierta.");
        }
        ultima.registrarSalida(ahora.toLocalTime().withSecond(0).withNano(0));
        presente = false;
        return ultima;
    }

    public boolean estaPresente() {
        return presente;
    }

    /** Solo la usan la persistencia y los datos de prueba para restaurar el estado guardado. */
    public void setPresente(boolean presente) {
        this.presente = presente;
    }

    /** Jornada de la fecha indicada, o null si ese día no entró. */
    public RegistroAsistencia getAsistenciaDel(LocalDate fecha) {
        return historial.asistenciaDel(fecha);
    }

    /** Indica si la fecha es un día laborable según su horario. */
    public boolean trabajaEl(LocalDate fecha) {
        return horario.incluyeDia(fecha.getDayOfWeek());
    }

    public void agregarAsistencia(RegistroAsistencia registro) {
        historial.agregarAsistencia(registro);
    }

    public List<RegistroAsistencia> getAsistencias() {
        return historial.getAsistencias();
    }

    // ------------------------------------------------------------------ incidencias

    /**
     * Indica si puede reportar una incidencia que afecta a otra persona.
     * Por defecto cada quien solo reporta las suyas; Supervisor y Jefe amplían este permiso.
     */
    public boolean puedeReportarPara(Usuario afectado) {
        return afectado == this;
    }

    /** Reporta una incidencia y la agrega al historial de la persona afectada. */
    public void reportar(Incidencia incidencia) {
        if (incidencia.getReportadoPor() != this || !puedeReportarPara(incidencia.getAfectado())) {
            throw new PermisoDenegadoException("No puedes reportar incidencias de " + incidencia.getAfectado().getNombre() + ".");
        }
        incidencia.getAfectado().historial.agregarIncidencia(incidencia);
    }

    /** Implementación común para los revisores (Supervisor y Jefe). */
    protected void revisarComo(RevisorIncidencias revisor, Incidencia incidencia, EstadoIncidencia nuevo,
            String comentario, LocalDateTime ahora) {
        if (!revisor.puedeRevisar(incidencia)) {
            throw new PermisoDenegadoException("No tienes autoridad para revisar la incidencia #" + incidencia.getId() + ".");
        }
        incidencia.registrarRevision(nuevo, this, comentario, ahora);
    }

    public Historial getHistorial() {
        return historial;
    }

    /** Fecha en que entró a la empresa (null en datos anteriores a este campo). */
    public LocalDate getFechaIngreso() {
        return historial.getInicio();
    }

    public void setFechaIngreso(LocalDate fecha) {
        historial.setInicio(fecha);
    }

    /** Copia historial y presencia de otro usuario (se usa cuando el Jefe le cambia el rol). */
    void copiarEstadoDe(Usuario otro) {
        historial.copiarDe(otro.historial);
        presente = otro.presente;
        obra = otro.obra;
        claveResumen = otro.claveResumen;
        if (otro.cuenta != null) {
            vincularCuenta(otro.cuenta);
        }
    }

    /** Une la ficha con la cuenta personal: ambas comparten el resumen de la clave. */
    public void vincularCuenta(Cuenta cuenta) {
        if (!cuenta.getDocumento().equals(cedula)) {
            throw new DatoInvalidoException("El documento de la cuenta no coincide con la cédula de la ficha.");
        }
        this.cuenta = cuenta;
        this.claveResumen = cuenta.getClaveResumen();
        cuenta.vincular(this);
    }

    /** Lo usa la empresa al eliminar la ficha: la cuenta queda sin empresa. */
    void desvincularCuenta() {
        if (cuenta != null) {
            cuenta.desvincular();
            cuenta = null;
        }
    }

    public Cuenta getCuenta() {
        return cuenta;
    }

    // ------------------------------------------------------------------ datos personales

    public boolean verificarClave(String clave) {
        return clave != null && claveResumen.equals(CifradoClave.resumir(cedula, clave));
    }

    public String getCedula() {
        return cedula;
    }

    private void setCedula(String cedula) {
        this.cedula = validarDocumento(cedula);
    }

    /**
     * Formato admitido al guardar y cargar: 6 a 12 letras o números. Es más amplio que el de un alta nueva
     * para que los datos guardados antes de exigir solo números (por ejemplo, «V12345678») sigan cargando.
     */
    public static String validarDocumento(String documento) {
        String limpio = documento == null ? "" : documento.trim().toUpperCase();
        if (!limpio.matches("[0-9A-Z]{6,12}")) {
            throw new DatoInvalidoException("La cédula o DNI debe tener solo números, entre 6 y 10 dígitos.");
        }
        return limpio;
    }

    /**
     * Regla para registrar una persona nueva: solo números, entre 6 y 10 dígitos (sin V, E, puntos ni guiones).
     * Evita duplicar a una misma persona escribiendo «V12345678» y «12345678».
     */
    public static String validarCedulaNueva(String documento) {
        String limpio = documento == null ? "" : documento.trim();
        if (!limpio.matches("[0-9]{6,10}")) {
            throw new DatoInvalidoException("La cédula o DNI debe tener solo números, entre 6 y 10 dígitos (sin V, puntos ni guiones).");
        }
        return limpio;
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
            throw new DatoInvalidoException("El nombre debe tener entre 3 y 60 letras.");
        }
        return limpio;
    }

    public final void setClave(String clave) {
        validarClave(clave);
        this.claveResumen = CifradoClave.resumir(cedula, clave);
        if (cuenta != null) {
            cuenta.sincronizarClave(claveResumen);
        }
    }

    public static void validarClave(String clave) {
        if (clave == null || !clave.matches("[^|\\s]{4,30}")) {
            throw new DatoInvalidoException("La clave debe tener entre 4 y 30 caracteres, sin espacios.");
        }
    }

    /** Resumen SHA-256 de la clave: lo usa la capa de persistencia para guardarlo. */
    public String getClaveResumen() {
        return claveResumen;
    }

    /** Restaura un resumen ya calculado (al cargar desde el archivo). */
    public void restaurarClaveResumen(String resumen) {
        if (resumen == null || !resumen.matches("[0-9a-f]{64}")) {
            throw new DatoInvalidoException("Resumen de clave inválido.");
        }
        this.claveResumen = resumen;
    }

    public Horario getHorario() {
        return horario;
    }

    public final void setHorario(Horario horario) {
        if (horario == null) {
            throw new DatoInvalidoException("El horario es obligatorio.");
        }
        this.horario = horario;
    }

    public Obra getObra() {
        return obra;
    }

    public void setObra(Obra obra) {
        this.obra = obra;
    }

    @Override
    public String toString() {
        return nombre + " (" + getRol() + ")";
    }
}
