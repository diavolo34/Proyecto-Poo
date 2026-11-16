package construtrack.controlador.web;

import construtrack.util.Json;

/**
 * Constructor fluido de objetos JSON: evita concatenar comillas y comas a mano.
 *
 * <pre>new JsonObjeto().texto("nombre", "Ana").numero("edad", 30).toString()</pre>
 */
public class JsonObjeto {

    private final StringBuilder json = new StringBuilder("{");

    public JsonObjeto texto(String clave, Object valor) {
        return crudo(clave, valor == null ? "null" : Json.texto(valor.toString()));
    }

    public JsonObjeto numero(String clave, Number valor) {
        return crudo(clave, valor == null ? "null" : valor.toString());
    }

    public JsonObjeto logico(String clave, boolean valor) {
        return crudo(clave, String.valueOf(valor));
    }

    /** Agrega un valor que ya está en formato JSON (otro objeto, un arreglo, null...). */
    public JsonObjeto crudo(String clave, String json) {
        this.json.append(this.json.length() == 1 ? "" : ",").append(Json.texto(clave)).append(':').append(json);
        return this;
    }

    @Override
    public String toString() {
        return json + "}";
    }
}
