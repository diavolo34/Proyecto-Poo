package construtrack.vista.swing;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Vista: tablero Kanban de incidencias con una columna por estado (Pendiente, En revisión, Aprobada, Rechazada).
 * Inspirado en los tableros ágiles: el supervisor ve de un vistazo qué falta por revisar.
 */
public final class PanelTablero extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient Map<String, JPanel> columnas = new LinkedHashMap<>();
    private final transient Map<String, JLabel> encabezados = new LinkedHashMap<>();
    private final JLabel lblMensaje = Tema.texto(" ");

    public PanelTablero(String... estados) {
        super(new BorderLayout(0, 10));
        setBorder(Tema.margen(12, 4, 4, 4));
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        barra.add(Tema.titulo("Tablero de incidencias"), BorderLayout.WEST);
        barra.add(Tema.textoSuave("Revisa, aprueba o rechaza las novedades de tu equipo"), BorderLayout.SOUTH);
        add(barra, BorderLayout.NORTH);

        JPanel tablero = new JPanel(new GridLayout(1, estados.length, 12, 0));
        tablero.setOpaque(false);
        for (String estado : estados) {
            JPanel columna = new JPanel(new BorderLayout(0, 8));
            columna.setBackground(Tema.GRIS_SUAVE);
            columna.setBorder(Tema.margen(10, 10, 10, 10));
            JLabel encabezado = Tema.texto(estado.toUpperCase());
            encabezado.setFont(Tema.NEGRITA);
            encabezado.setForeground(Tema.TEXTO_SUAVE);
            encabezados.put(estado, encabezado);
            columna.add(encabezado, BorderLayout.NORTH);
            JPanel tarjetas = new JPanel();
            tarjetas.setOpaque(false);
            tarjetas.setLayout(new BoxLayout(tarjetas, BoxLayout.Y_AXIS));
            columnas.put(estado, tarjetas);
            JPanel envoltorio = new AnchoDeColumna();
            envoltorio.setOpaque(false);
            envoltorio.add(tarjetas, BorderLayout.NORTH);
            JScrollPane scroll = new JScrollPane(envoltorio);
            scroll.setBorder(null);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            columna.add(scroll, BorderLayout.CENTER);
            tablero.add(columna);
        }
        add(tablero, BorderLayout.CENTER);
        add(lblMensaje, BorderLayout.SOUTH);
    }

    public void limpiar() {
        columnas.values().forEach(JPanel::removeAll);
    }

    public void agregarTarjeta(String estado, TarjetaIncidencia tarjeta) {
        JPanel columna = columnas.get(estado);
        columna.add(tarjeta);
        columna.add(Box.createVerticalStrut(8));
    }

    /** Actualiza los contadores de cada columna y repinta. */
    public void actualizar() {
        for (Map.Entry<String, JPanel> e : columnas.entrySet()) {
            long n = e.getValue().getComponentCount() / 2;
            encabezados.get(e.getKey()).setText(e.getKey().toUpperCase() + "   " + n);
        }
        revalidate();
        repaint();
    }

    /** Contenedor que siempre toma el ancho visible de la columna, para que las tarjetas no se corten. */
    private static final class AnchoDeColumna extends JPanel implements javax.swing.Scrollable {

        private static final long serialVersionUID = 1L;

        AnchoDeColumna() {
            super(new BorderLayout());
        }

        @Override
        public java.awt.Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visible, int orientacion, int direccion) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visible, int orientacion, int direccion) {
            return visible.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    public void mostrarMensaje(String texto, boolean esError) {
        lblMensaje.setText((esError ? "Error: " : "Listo: ") + texto);
        lblMensaje.setForeground(esError ? Tema.ROJO : Tema.VERDE);
    }
}
