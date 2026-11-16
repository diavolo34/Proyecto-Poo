package construtrack.persistencia;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class Campos {
    private Campos() {
    }

    static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n").replace("\r", "");
    }

    static String desescapar(String texto) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '\\' && i + 1 < texto.length()) {
                char siguiente = texto.charAt(++i);
                sb.append(siguiente == 'p' ? '|' : siguiente == 'n' ? '\n' : siguiente);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    static String extrasATexto(Map<String, String> extras) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : extras.entrySet()) {
            sb.append(sb.length() == 0 ? "" : ";")
                    .append(escapar(e.getKey()).replace("=", "\\e").replace(";", "\\s"))
                    .append('=')
                    .append(escapar(e.getValue()).replace(";", "\\s"));
        }
        return sb.toString();
    }

    static Map<String, String> extras(String texto) {
        Map<String, String> extras = new LinkedHashMap<>();
        if (texto == null || texto.isEmpty()) {
            return Collections.unmodifiableMap(extras);
        }
        for (String par : texto.split(";")) {
            int igual = par.indexOf('=');
            if (igual > 0) {
                String clave = desescapar(par.substring(0, igual).replace("\\e", "=").replace("\\s", ";"));
                String valor = desescapar(par.substring(igual + 1).replace("\\s", ";"));
                extras.put(clave, valor);
            }
        }
        return extras;
    }
}
