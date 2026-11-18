package construtrack.modelo.inventario;

import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.util.ArrayList;
import java.util.List;

public final class Articulo {
    public static final int CANTIDAD_MAXIMA = 100_000;

    private final String codigo;
    private final CategoriaArticulo categoria;
    private String nombre;
    private String marca;
    private String serie;
    private int cantidad;
    private EstadoArticulo estado = EstadoArticulo.OPERATIVO;
    private String nota;
    private final List<Asignacion> asignaciones = new ArrayList<>();

    public Articulo(String codigo, CategoriaArticulo categoria, String nombre, String marca, String serie, int cantidad,
            String nota) {
        if (categoria == null) {
            throw new DatoInvalidoException("Elige la categoría del artículo.");
        }
        this.codigo = codigo;
        this.categoria = categoria;
        actualizar(nombre, marca, serie, cantidad, nota);
    }

    public final void actualizar(String nombre, String marca, String serie, int cantidad, String nota) {
        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (limpio.length() < 3 || limpio.length() > 80) {
            throw new DatoInvalidoException("El nombre del artículo debe tener entre 3 y 80 caracteres.");
        }
        if (cantidad < 1 || cantidad > CANTIDAD_MAXIMA) {
            throw new DatoInvalidoException("La cantidad debe estar entre 1 y " + CANTIDAD_MAXIMA + ".");
        }
        String serieLimpia = texto(serie, 40);
        if (!serieLimpia.isEmpty() && cantidad != 1) {
            throw new DatoInvalidoException("Un artículo con número de serie es una sola unidad: deja la cantidad en 1.");
        }
        int enUso = enUso();
        if (cantidad < enUso) {
            throw new OperacionInvalidaException("Hay " + enUso + " unidad(es) entregadas: la cantidad no puede ser menor.");
        }
        this.nombre = limpio;
        this.marca = texto(marca, 60);
        this.serie = serieLimpia;
        this.cantidad = cantidad;
        this.nota = texto(nota, 300);
    }

    private static String texto(String valor, int maximo) {
        String limpio = valor == null ? "" : valor.trim();
        if (limpio.length() > maximo) {
            throw new DatoInvalidoException("Un campo del artículo supera los " + maximo + " caracteres.");
        }
        return limpio;
    }

    public void cambiarEstado(EstadoArticulo nuevo) {
        if (nuevo != EstadoArticulo.OPERATIVO && enUso() > 0) {
            throw new OperacionInvalidaException("«" + nombre + "» tiene unidades entregadas. Registra la devolución primero.");
        }
        this.estado = nuevo;
    }

    void restaurarEstado(EstadoArticulo estado) {
        this.estado = estado;
    }

    public int enUso() {
        int total = 0;
        for (Asignacion a : asignaciones) {
            if (a.estaAbierta()) {
                total += a.getCantidad();
            }
        }
        return total;
    }

    public int disponibles() {
        return estado == EstadoArticulo.OPERATIVO ? cantidad - enUso() : 0;
    }

    void agregarAsignacion(Asignacion asignacion) {
        asignaciones.add(asignacion);
    }

    void verificarDisponible(int pedida) {
        if (estado != EstadoArticulo.OPERATIVO) {
            throw new OperacionInvalidaException("«" + nombre + "» está " + estado.getNombre().toLowerCase() + ".");
        }
        if (pedida > disponibles()) {
            throw new OperacionInvalidaException("Solo hay " + disponibles() + " unidad(es) disponibles de «" + nombre + "».");
        }
    }

    void registrarPerdida(int unidades) {
        cantidad = Math.max(0, cantidad - unidades);
        if (cantidad == 0) {
            cantidad = 1;
            estado = EstadoArticulo.DE_BAJA;
        }
    }

    public List<Asignacion> getAsignaciones() {
        return List.copyOf(asignaciones);
    }

    public String getCodigo() {
        return codigo;
    }

    public CategoriaArticulo getCategoria() {
        return categoria;
    }

    public String getNombre() {
        return nombre;
    }

    public String getMarca() {
        return marca;
    }

    public String getSerie() {
        return serie;
    }

    public int getCantidad() {
        return cantidad;
    }

    public EstadoArticulo getEstado() {
        return estado;
    }

    public String getNota() {
        return nota;
    }
}
