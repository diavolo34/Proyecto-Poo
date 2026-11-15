package construtrack.modelo.incidencias;

import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Novedad que afecta la asistencia de un trabajador: una falta, un retardo, un reposo médico...
 * Es abstracta: cada tipo concreto define su nombre, su gravedad y sus datos propios.
 * Todas siguen el mismo ciclo de revisión (ver {@link EstadoIncidencia}).
 */
public abstract class Incidencia {

    private final int id;
    private final Usuario afectado;
    private final Usuario reportadoPor;
    private final LocalDate desde;
    private final LocalDate hasta;
    private final String descripcion;
    private final LocalDateTime fechaReporte;

    private EstadoIncidencia estado = EstadoIncidencia.PENDIENTE;
    private Usuario revisor;
    private String comentarioRevision = "";
    private LocalDateTime fechaRevision;

    protected Incidencia(DatosIncidencia datos) {
        if (datos.afectado == null || datos.reportadoPor == null) {
            throw new DatoInvalidoException("La incidencia debe indicar a quién afecta y quién la reporta.");
        }
        if (datos.desde == null || datos.hasta.isBefore(datos.desde)) {
            throw new DatoInvalidoException("La fecha final de la incidencia no puede ser anterior a la inicial.");
        }
        String texto = datos.descripcion == null ? "" : datos.descripcion.trim();
        if (texto.length() < 5 || texto.length() > 300) {
            throw new DatoInvalidoException("Describe la incidencia con al menos 5 caracteres (máximo 300).");
        }
        this.id = datos.id;
        this.afectado = datos.afectado;
        this.reportadoPor = datos.reportadoPor;
        this.desde = datos.desde;
        this.hasta = datos.hasta;
        this.descripcion = texto;
        this.fechaReporte = datos.fechaReporte;
    }

    /** Código estable del tipo (se usa para guardar y para la fábrica). */
    public abstract String getCodigoTipo();

    /** Nombre legible del tipo. */
    public abstract String getNombreTipo();

    public abstract Gravedad getGravedad();

    /** Datos propios del tipo (por ejemplo, el certificado de un reposo). Vacío si no tiene. */
    public Map<String, String> getExtras() {
        return new LinkedHashMap<>();
    }

    /** Resumen de los datos propios del tipo para mostrarlo en las vistas. */
    public String getDetalle() {
        return "";
    }

    /**
     * Registra la decisión de un revisor. Solo la llaman los revisores (Supervisor o Jefe)
     * después de comprobar que tienen permiso sobre esta incidencia.
     */
    public void registrarRevision(EstadoIncidencia nuevo, Usuario quien, String comentario, LocalDateTime ahora) {
        if (!estado.puedePasarA(nuevo)) {
            throw new OperacionInvalidaException("La incidencia #" + id + " está " + estado.getNombre().toLowerCase()
                    + " y no puede pasar a " + nuevo.getNombre().toLowerCase() + ".");
        }
        if (nuevo == EstadoIncidencia.RECHAZADA && (comentario == null || comentario.isBlank())) {
            throw new DatoInvalidoException("Indica el motivo del rechazo para que el trabajador lo sepa.");
        }
        this.estado = nuevo;
        this.revisor = quien;
        this.comentarioRevision = comentario == null ? "" : comentario.trim();
        this.fechaRevision = ahora;
    }

    /** Restaura el estado guardado (lo usa la capa de persistencia). */
    public void restaurarRevision(EstadoIncidencia estado, Usuario revisor, String comentario, LocalDateTime fecha) {
        this.estado = estado;
        this.revisor = revisor;
        this.comentarioRevision = comentario == null ? "" : comentario;
        this.fechaRevision = fecha;
    }

    public boolean cubre(LocalDate fecha) {
        return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }

    public boolean estaAprobada() {
        return estado == EstadoIncidencia.APROBADA;
    }

    /** Ayuda a las subclases justificables: aprobada y dentro de su rango de fechas. */
    protected boolean aprobadaYCubre(LocalDate fecha) {
        return estaAprobada() && cubre(fecha);
    }

    /** Horas que lleva sin resolverse (0 si ya se resolvió). */
    public long horasSinResolver(LocalDateTime ahora) {
        return estado.esFinal() ? 0 : ChronoUnit.HOURS.between(fechaReporte, ahora);
    }

    public long getDias() {
        return ChronoUnit.DAYS.between(desde, hasta) + 1;
    }

    public int getId() {
        return id;
    }

    public Usuario getAfectado() {
        return afectado;
    }

    public Usuario getReportadoPor() {
        return reportadoPor;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public LocalDateTime getFechaReporte() {
        return fechaReporte;
    }

    public EstadoIncidencia getEstado() {
        return estado;
    }

    public Usuario getRevisor() {
        return revisor;
    }

    public String getComentarioRevision() {
        return comentarioRevision;
    }

    public LocalDateTime getFechaRevision() {
        return fechaRevision;
    }

    @Override
    public String toString() {
        return "#" + id + " " + getNombreTipo() + " · " + afectado.getNombre() + " (" + estado + ")";
    }
}
