package construtrack.modelo.inventario;

import construtrack.modelo.Firma;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Asignacion {
    private final int id;
    private final Articulo articulo;
    private final Firma persona;
    private final int cantidad;
    private final LocalDateTime fecha;
    private final Firma entregadoPor;
    private final String obra;
    private final LocalDate devolucionPrevista;
    private final String nota;
    private LocalDateTime fechaDevolucion;
    private Firma recibidoPor;
    private CondicionDevolucion condicion;
    private String notaDevolucion = "";

    public Asignacion(int id, Articulo articulo, Firma persona, int cantidad, LocalDateTime fecha, Firma entregadoPor,
            String obra, LocalDate devolucionPrevista, String nota) {
        if (cantidad < 1) {
            throw new DatoInvalidoException("La cantidad debe ser al menos 1.");
        }
        if (devolucionPrevista != null && devolucionPrevista.isBefore(fecha.toLocalDate())) {
            throw new DatoInvalidoException("La devolución prevista no puede ser anterior a la entrega.");
        }
        this.id = id;
        this.articulo = articulo;
        this.persona = persona;
        this.cantidad = cantidad;
        this.fecha = fecha;
        this.entregadoPor = entregadoPor;
        this.obra = obra == null || obra.isBlank() ? null : obra.trim();
        this.devolucionPrevista = devolucionPrevista;
        this.nota = nota == null ? "" : nota.trim();
    }

    void devolver(CondicionDevolucion condicion, Firma recibe, String nota, LocalDateTime ahora) {
        if (!estaAbierta()) {
            throw new OperacionInvalidaException("Esta entrega ya se cerró el " + fechaDevolucion.toLocalDate() + ".");
        }
        if (condicion == null) {
            throw new DatoInvalidoException("Indica cómo se devolvió.");
        }
        String limpia = nota == null ? "" : nota.trim();
        if (condicion != CondicionDevolucion.BUENA && limpia.length() < 3) {
            throw new DatoInvalidoException("Describe el daño o lo que pasó con el artículo.");
        }
        this.condicion = condicion;
        this.recibidoPor = recibe;
        this.notaDevolucion = limpia;
        this.fechaDevolucion = ahora;
    }

    public void restaurarDevolucion(LocalDateTime fecha, Firma recibe, CondicionDevolucion condicion, String nota) {
        this.fechaDevolucion = fecha;
        this.recibidoPor = recibe;
        this.condicion = condicion;
        this.notaDevolucion = nota == null ? "" : nota;
    }

    public boolean estaAbierta() {
        return fechaDevolucion == null;
    }

    public boolean vencidaEn(LocalDate hoy) {
        return estaAbierta() && devolucionPrevista != null && hoy.isAfter(devolucionPrevista);
    }

    public int getId() {
        return id;
    }

    public Articulo getArticulo() {
        return articulo;
    }

    public Firma getPersona() {
        return persona;
    }

    public int getCantidad() {
        return cantidad;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public Firma getEntregadoPor() {
        return entregadoPor;
    }

    public String getObra() {
        return obra;
    }

    public LocalDate getDevolucionPrevista() {
        return devolucionPrevista;
    }

    public String getNota() {
        return nota;
    }

    public LocalDateTime getFechaDevolucion() {
        return fechaDevolucion;
    }

    public Firma getRecibidoPor() {
        return recibidoPor;
    }

    public CondicionDevolucion getCondicion() {
        return condicion;
    }

    public String getNotaDevolucion() {
        return notaDevolucion;
    }
}
