package construtrack.vista.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

public final class VentanaPrincipal extends JFrame {
    private static final long serialVersionUID = 1L;

    private final JTabbedPane pestanas = new JTabbedPane();
    private final JButton btnSalir = Tema.botonSecundario("Cerrar sesión");

    public VentanaPrincipal(String nombreEmpresa, String nombreUsuario, String rol) {
        super(nombreEmpresa + " · " + nombreUsuario);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(barraSuperior(nombreEmpresa, nombreUsuario, rol), BorderLayout.NORTH);
        pestanas.setBorder(Tema.margen(8, 12, 12, 12));
        pestanas.setBackground(Tema.FONDO);
        add(pestanas, BorderLayout.CENTER);
        setSize(new Dimension(1180, 760));
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);
    }

    private JPanel barraSuperior(String nombreEmpresa, String nombreUsuario, String rol) {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Tema.SUPERFICIE);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE), Tema.margen(10, 18, 10, 18)));
        JLabel marca = Tema.subtitulo(nombreEmpresa);
        marca.setForeground(Tema.AZUL);
        marca.setFont(Tema.SUBTITULO.deriveFont(18f));
        barra.add(marca, BorderLayout.WEST);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        derecha.setOpaque(false);
        derecha.add(Tema.texto(nombreUsuario));
        derecha.add(Tema.chip(rol, Tema.AZUL_SUAVE, Tema.AZUL_OSCURO));
        btnSalir.setMnemonic('C');
        derecha.add(btnSalir);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    public void agregarPestana(String titulo, String ayuda, JComponent contenido) {
        pestanas.addTab(titulo, null, contenido, ayuda);
    }

    public JTabbedPane getPestanas() {
        return pestanas;
    }

    public JButton getBtnSalir() {
        return btnSalir;
    }
}
