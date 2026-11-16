package construtrack.modelo.reportes;

import java.util.List;

public class FormateadorCSV implements FormateadorReporte {
    private static final String BOM = "﻿";

    @Override
    public String formatear(Documento documento) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append(linea(documento.getColumnas())).append("\r\n");
        for (List<String> fila : documento.getFilas()) {
            sb.append(linea(fila)).append("\r\n");
        }
        return sb.toString();
    }

    private static String linea(List<String> celdas) {
        StringBuilder sb = new StringBuilder();
        for (String celda : celdas) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(escapar(celda));
        }
        return sb.toString();
    }

    private static String escapar(String valor) {
        if (valor.contains(";") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    @Override
    public String mime() {
        return "text/csv";
    }

    @Override
    public String extension() {
        return "csv";
    }
}
