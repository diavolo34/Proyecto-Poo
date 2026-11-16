package construtrack.controlador.web;

public class Respuesta {
    private final int codigo;
    private final String tipo;
    private final String cuerpo;
    private final String nombreArchivo;

    private Respuesta(int codigo, String tipo, String cuerpo, String nombreArchivo) {
        this.codigo = codigo;
        this.tipo = tipo;
        this.cuerpo = cuerpo;
        this.nombreArchivo = nombreArchivo;
    }

    public static Respuesta json(String json) {
        return new Respuesta(200, "application/json; charset=utf-8", json, null);
    }

    public static Respuesta creado(String json) {
        return new Respuesta(201, "application/json; charset=utf-8", json, null);
    }

    public static Respuesta error(int codigo, String mensaje) {
        return new Respuesta(codigo, "application/json; charset=utf-8", JsonMapper.error(mensaje), null);
    }

    public static Respuesta descarga(String contenido, String tipo, String nombreArchivo) {
        return new Respuesta(200, tipo + "; charset=utf-8", contenido, nombreArchivo);
    }

    public int getCodigo() {
        return codigo;
    }

    public String getTipo() {
        return tipo;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }
}
