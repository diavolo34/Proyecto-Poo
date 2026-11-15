package construtrack.vista.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/**
 * Gráfico de barras horizontales dibujado con Java2D (sin librerías externas).
 * Cada barra muestra además su valor en texto, para no depender solo de la longitud o del color.
 */
public final class GraficoBarras extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int ALTO_BARRA = 22;
    private static final int SEPARACION = 12;

    private final transient List<String> etiquetas = new ArrayList<>();
    private final transient List<Double> valores = new ArrayList<>();
    private double maximo = 100;
    private String sufijo = "%";

    public GraficoBarras() {
        setBackground(Tema.SUPERFICIE);
    }

    public void setDatos(List<String> etiquetas, List<Double> valores, double maximo, String sufijo) {
        this.etiquetas.clear();
        this.etiquetas.addAll(etiquetas);
        this.valores.clear();
        this.valores.addAll(valores);
        this.maximo = maximo <= 0 ? 1 : maximo;
        this.sufijo = sufijo;
        getAccessibleContext().setAccessibleDescription(descripcionTextual());
        revalidate();
        repaint();
    }

    private String descripcionTextual() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < etiquetas.size(); i++) {
            sb.append(etiquetas.get(i)).append(": ").append(Math.round(valores.get(i))).append(sufijo).append(". ");
        }
        return sb.toString();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(360, Math.max(120, etiquetas.size() * (ALTO_BARRA + SEPARACION) + 10));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(Tema.PEQUENA);
        FontMetrics fm = g2.getFontMetrics();
        if (etiquetas.isEmpty()) {
            g2.setColor(Tema.TEXTO_SUAVE);
            g2.drawString("No hay datos para este período.", 4, 20);
            g2.dispose();
            return;
        }
        int anchoEtiqueta = 0;
        for (String e : etiquetas) {
            anchoEtiqueta = Math.max(anchoEtiqueta, fm.stringWidth(e));
        }
        anchoEtiqueta += 12;
        int anchoTexto = fm.stringWidth("100.0" + sufijo) + 10;
        int anchoDisponible = Math.max(40, getWidth() - anchoEtiqueta - anchoTexto);
        int y = 4;
        for (int i = 0; i < etiquetas.size(); i++) {
            double valor = valores.get(i);
            int largo = (int) Math.round(anchoDisponible * Math.min(1.0, valor / maximo));
            g2.setColor(Tema.TEXTO);
            g2.drawString(etiquetas.get(i), 0, y + ALTO_BARRA / 2 + fm.getAscent() / 2 - 2);
            g2.setColor(Tema.GRIS_SUAVE);
            g2.fillRoundRect(anchoEtiqueta, y, anchoDisponible, ALTO_BARRA, 6, 6);
            g2.setColor(colorPara(valor));
            g2.fillRoundRect(anchoEtiqueta, y, Math.max(4, largo), ALTO_BARRA, 6, 6);
            g2.setColor(Tema.TEXTO);
            g2.drawString(String.format("%.1f%s", valor, sufijo), anchoEtiqueta + anchoDisponible + 8,
                    y + ALTO_BARRA / 2 + fm.getAscent() / 2 - 2);
            y += ALTO_BARRA + SEPARACION;
        }
        g2.dispose();
    }

    /** Verde si va bien, ámbar si hay que vigilar, rojo si requiere atención. */
    private Color colorPara(double valor) {
        double proporcion = valor / maximo;
        if (proporcion >= 0.9) {
            return new Color(0x22A06B);
        }
        if (proporcion >= 0.75) {
            return new Color(0xE2B203);
        }
        return new Color(0xC9372C);
    }
}
