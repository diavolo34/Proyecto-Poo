package construtrack.vista.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * Ilustración decorativa dibujada con Java2D: edificios en construcción, una grúa y un tablero.
 * No necesita archivos de imagen, así funciona igual en cualquier computadora.
 */
public final class Ilustracion extends JPanel {

    private static final long serialVersionUID = 1L;

    public Ilustracion() {
        getAccessibleContext().setAccessibleDescription("Ilustración de una obra en construcción");
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        g2.setPaint(new GradientPaint(0, 0, Tema.AZUL, w, h, new Color(0x1D7AFC)));
        g2.fillRect(0, 0, w, h);

        // Bloques de color al estilo de las ilustraciones de producto (detrás del tablero, sin tapar la obra)
        g2.setColor(new Color(0xFCA700));
        g2.fillRect(w - 150, 24, 120, 90);
        g2.setColor(new Color(0x09326C));
        g2.fillRect(w - 130, 44, 120, 90);

        // Edificios
        int base = h - 70;
        int[][] edificios = {{40, 150}, {120, 230}, {200, 180}, {330, 120}};
        for (int[] e : edificios) {
            int x = e[0] * w / 420;
            g2.setColor(new Color(255, 255, 255, 230));
            g2.fillRect(x, base - e[1], 60, e[1]);
            g2.setColor(new Color(0xB3D4FF));
            for (int fy = base - e[1] + 12; fy < base - 12; fy += 22) {
                g2.fillRect(x + 10, fy, 14, 10);
                g2.fillRect(x + 36, fy, 14, 10);
            }
        }
        // Grúa (amarillo claro para que contraste con el fondo azul)
        g2.setColor(new Color(0xFFE380));
        g2.setStroke(new BasicStroke(5));
        int gx = w * 290 / 420;
        g2.drawLine(gx, base, gx, base - 260);
        g2.drawLine(gx - 120, base - 250, gx + 60, base - 250);
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(gx - 100, base - 250, gx - 100, base - 180);
        g2.fillRect(gx - 112, base - 180, 24, 18);
        // Suelo
        g2.setColor(new Color(255, 255, 255, 120));
        g2.fillRect(0, base, w, 4);

        // Tablero (como las tarjetas de un tablero Kanban)
        int tx = 40;
        int ty = 40;
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(tx, ty, 200, 110, 10, 10);
        String[] columnas = {"POR HACER", "EN CURSO", "LISTO"};
        Color[] marcas = {Tema.AZUL, new Color(0x6A9A23), new Color(0xFCA700)};
        g2.setFont(Tema.PEQUENA.deriveFont(9f));
        for (int c = 0; c < 3; c++) {
            int cx = tx + 10 + c * 63;
            g2.setColor(Tema.TEXTO_SUAVE);
            g2.drawString(columnas[c], cx, ty + 20);
            g2.setColor(Tema.GRIS_SUAVE);
            g2.fillRoundRect(cx, ty + 28, 56, 30, 6, 6);
            g2.setColor(marcas[c]);
            g2.fillRect(cx + 5, ty + 47, 7, 7);
            if (c < 2) {
                g2.setColor(Tema.GRIS_SUAVE);
                g2.fillRoundRect(cx, ty + 64, 56, 30, 6, 6);
                g2.setColor(marcas[(c + 1) % 3]);
                g2.fillRect(cx + 5, ty + 83, 7, 7);
            }
        }
        g2.dispose();
    }
}
