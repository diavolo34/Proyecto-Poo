package construtrack.vista.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public final class TarjetaIncidencia extends JPanel {
    private static final long serialVersionUID = 1L;

    private final JPanel acciones = new JPanel(new java.awt.GridLayout(1, 0, 4, 0));

    public TarjetaIncidencia(String titulo, String persona, String fechas, String descripcion,
            String gravedad, Color colorGravedad, Color fondoGravedad) {
        super(new BorderLayout(0, 6));
        setBackground(Tema.SUPERFICIE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Tema.BORDE),
                        BorderFactory.createMatteBorder(0, 4, 0, 0, colorGravedad)),
                Tema.margen(10, 10, 10, 10)));
        setAlignmentX(LEFT_ALIGNMENT);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        JLabel lblTitulo = Tema.texto(titulo);
        lblTitulo.setFont(Tema.NEGRITA);
        textos.add(lblTitulo);
        textos.add(Tema.texto(persona));
        textos.add(Tema.textoSuave(fechas));
        JLabel lblDescripcion = Tema.textoSuave("<html>" + descripcion + "</html>");
        textos.add(lblDescripcion);
        add(textos, BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chip.setOpaque(false);
        chip.add(Tema.chip("Gravedad " + gravedad.toLowerCase(), fondoGravedad, colorGravedad));
        pie.add(chip, BorderLayout.NORTH);
        acciones.setOpaque(false);
        JPanel separacion = new JPanel(new BorderLayout());
        separacion.setOpaque(false);
        separacion.setBorder(Tema.margen(6, 0, 0, 0));
        separacion.add(acciones, BorderLayout.CENTER);
        pie.add(separacion, BorderLayout.SOUTH);
        add(pie, BorderLayout.SOUTH);
        getAccessibleContext().setAccessibleName(titulo + ", " + persona);
    }

    public void agregarAccion(JButton boton) {
        boton.setBorder(Tema.margen(4, 4, 4, 4));
        boton.setFont(Tema.PEQUENA.deriveFont(java.awt.Font.BOLD));
        acciones.add(boton);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
