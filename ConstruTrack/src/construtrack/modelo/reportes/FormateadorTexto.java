package construtrack.modelo.reportes;

import java.util.List;

/** Tabla de texto alineada, legible en cualquier editor. */
public class FormateadorTexto implements FormateadorReporte {

    @Override
    public String formatear(Documento documento) {
        List<String> columnas = documento.getColumnas();
        int[] anchos = new int[columnas.size()];
        for (int c = 0; c < anchos.length; c++) {
            anchos[c] = columnas.get(c).length();
        }
        for (List<String> fila : documento.getFilas()) {
            for (int c = 0; c < anchos.length; c++) {
                anchos[c] = Math.max(anchos[c], fila.get(c).length());
            }
        }
        StringBuilder sb = new StringBuilder(documento.getTitulo()).append("\n\n");
        sb.append(linea(columnas, anchos)).append('\n');
        for (int c = 0; c < anchos.length; c++) {
            sb.append("-".repeat(anchos[c])).append(c < anchos.length - 1 ? "-+-" : "");
        }
        sb.append('\n');
        for (List<String> fila : documento.getFilas()) {
            sb.append(linea(fila, anchos)).append('\n');
        }
        return sb.toString();
    }

    private static String linea(List<String> celdas, int[] anchos) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < anchos.length; c++) {
            sb.append(String.format("%-" + anchos[c] + "s", celdas.get(c)));
            if (c < anchos.length - 1) {
                sb.append(" | ");
            }
        }
        return sb.toString();
    }

    @Override
    public String mime() {
        return "text/plain";
    }

    @Override
    public String extension() {
        return "txt";
    }
}
