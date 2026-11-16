package construtrack.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utilidades mínimas para escribir y leer JSON sin librerías externas.
 */
public final class Json {

    private Json() {
    }

    /** Convierte un texto en una cadena JSON (con comillas y caracteres especiales escapados). */
    public static String texto(String valor) {
        if (valor == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : valor.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }

    /** Convierte una colección en un arreglo JSON usando la función de conversión de cada elemento. */
    public static <T> String lista(Collection<T> elementos, Function<? super T, String> conversion) {
        StringBuilder json = new StringBuilder("[");
        for (T elemento : elementos) {
            json.append(json.length() == 1 ? "" : ",").append(conversion.apply(elemento));
        }
        return json.append(']').toString();
    }

    /** Lee un objeto JSON plano {"clave": valor, ...}. Los valores se devuelven como texto. */
    public static Map<String, String> leerObjeto(String json) {
        try {
            Map<String, String> datos = new LinkedHashMap<>();
            int[] pos = {0};
            saltarEspacios(json, pos);
            esperar(json, pos, '{');
            saltarEspacios(json, pos);
            if (json.charAt(pos[0]) == '}') {
                return datos;
            }
            while (true) {
                saltarEspacios(json, pos);
                String clave = leerCadena(json, pos);
                saltarEspacios(json, pos);
                esperar(json, pos, ':');
                saltarEspacios(json, pos);
                String valor;
                if (json.charAt(pos[0]) == '"') {
                    valor = leerCadena(json, pos);
                } else {
                    int inicio = pos[0];
                    while (",}".indexOf(json.charAt(pos[0])) < 0) {
                        pos[0]++;
                    }
                    valor = json.substring(inicio, pos[0]).trim();
                    if ("null".equals(valor)) {
                        valor = null;
                    }
                }
                datos.put(clave, valor);
                saltarEspacios(json, pos);
                if (json.charAt(pos[0]) == '}') {
                    return datos;
                }
                esperar(json, pos, ',');
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Los datos enviados no tienen un formato JSON válido.");
        }
    }

    private static String leerCadena(String json, int[] pos) {
        esperar(json, pos, '"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = json.charAt(pos[0]++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char escape = json.charAt(pos[0]++);
            switch (escape) {
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 't': sb.append('\t'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'u':
                    sb.append((char) Integer.parseInt(json.substring(pos[0], pos[0] + 4), 16));
                    pos[0] += 4;
                    break;
                default: sb.append(escape);
            }
        }
    }

    private static void esperar(String json, int[] pos, char esperado) {
        if (json.charAt(pos[0]) != esperado) {
            throw new IllegalArgumentException("Se esperaba '" + esperado + "'");
        }
        pos[0]++;
    }

    private static void saltarEspacios(String json, int[] pos) {
        while (pos[0] < json.length() && Character.isWhitespace(json.charAt(pos[0]))) {
            pos[0]++;
        }
    }
}
