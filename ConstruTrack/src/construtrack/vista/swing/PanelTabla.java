package construtrack.vista.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public final class PanelTabla extends JPanel {
    private static final long serialVersionUID = 1L;

    private final DefaultTableModel modelo;
    private final JTable tabla;
    private final JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JLabel lblMensaje = Tema.texto(" ");

    public PanelTabla(String titulo, String ayuda, String... columnas) {
        super(new BorderLayout(0, 8));
        setBackground(Tema.SUPERFICIE);
        setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(Tema.BORDE),
                Tema.margen(14, 14, 14, 14)));
        JPanel cabecera = new JPanel(new BorderLayout(0, 2));
        cabecera.setOpaque(false);
        cabecera.add(Tema.subtitulo(titulo), BorderLayout.NORTH);
        if (ayuda != null) {
            cabecera.add(Tema.textoSuave("<html>" + ayuda + "</html>"), BorderLayout.CENTER);
        }
        add(cabecera, BorderLayout.NORTH);
        modelo = Tema.modeloSoloLectura(columnas);
        tabla = Tema.tabla(modelo);
        add(Tema.desplazable(tabla), BorderLayout.CENTER);
        JPanel pie = new JPanel(new BorderLayout(0, 6));
        pie.setOpaque(false);
        botones.setOpaque(false);
        pie.add(botones, BorderLayout.NORTH);
        pie.add(lblMensaje, BorderLayout.SOUTH);
        add(pie, BorderLayout.SOUTH);
    }

    public JButton agregarBoton(JButton boton) {
        botones.add(boton);
        return boton;
    }

    public void agregarComponente(java.awt.Component componente) {
        botones.add(componente);
    }

    public void anchos(int... anchos) {
        Tema.anchos(tabla, anchos);
    }

    public DefaultTableModel getModelo() {
        return modelo;
    }

    public JTable getTabla() {
        return tabla;
    }

    public int filaSeleccionada() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? -1 : tabla.convertRowIndexToModel(fila);
    }

    public Object valorSeleccionado(int columna) {
        int fila = filaSeleccionada();
        return fila < 0 ? null : modelo.getValueAt(fila, columna);
    }

    public void limpiar() {
        modelo.setRowCount(0);
    }

    public void fila(Object... valores) {
        modelo.addRow(valores);
    }

    public void mostrarMensaje(String texto, boolean esError) {
        lblMensaje.setText("<html>" + (esError ? "Error: " : "Listo: ") + texto + "</html>");
        lblMensaje.setForeground(esError ? Tema.ROJO : Tema.VERDE);
    }
}
