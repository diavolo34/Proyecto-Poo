package construtrack.vista.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * Vista: inicio de sesión. Solo construye y expone los componentes; la lógica está en ControladoraLogin.
 * Diseño: ilustración a la izquierda y un formulario corto a la derecha, con una sola acción principal.
 */
public final class VentanaLogin extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JTextField txtCedula = new JTextField(18);
    private final JPasswordField txtClave = new JPasswordField(18);
    private final JButton btnEntrar = Tema.botonPrimario("Iniciar sesión");
    private final JButton btnWeb = Tema.botonSecundario("Abrir la versión web");
    private final JLabel lblError = Tema.textoSuave(" ");

    public VentanaLogin(String nombreEmpresa) {
        super(nombreEmpresa + " · Iniciar sesión");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 2));
        add(new Ilustracion());
        add(formulario(nombreEmpresa));
        setSize(new Dimension(920, 560));
        setMinimumSize(new Dimension(760, 500));
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(btnEntrar); // Enter inicia sesión (eficiencia de uso)
    }

    private JPanel formulario(String nombreEmpresa) {
        JPanel contenedor = new JPanel(new GridBagLayout());
        contenedor.setBackground(Tema.SUPERFICIE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 4, 0);

        JLabel marca = Tema.titulo(nombreEmpresa);
        marca.setForeground(Tema.AZUL);
        form.add(marca, c);
        form.add(Tema.subtitulo("Inicia sesión para continuar"), c);
        form.add(Tema.textoSuave("<html><div style='width:300px'>Usa tu cédula (solo números) y la clave "
                + "que te dio tu jefe.</div></html>"), c);
        c.insets = new Insets(18, 0, 4, 0);
        form.add(campo("Cédula *", txtCedula, "Por ejemplo: 30000001"), c);
        c.insets = new Insets(4, 0, 4, 0);
        form.add(campo("Clave *", txtClave, "Mínimo 4 caracteres"), c);
        lblError.setForeground(Tema.ROJO);
        form.add(lblError, c);
        c.insets = new Insets(10, 0, 4, 0);
        btnEntrar.setMnemonic('I');
        form.add(btnEntrar, c);
        c.insets = new Insets(14, 0, 4, 0);
        form.add(separador("o también puedes"), c);
        c.insets = new Insets(4, 0, 4, 0);
        btnWeb.setToolTipText("Abre la página web de ConstruTrack en tu navegador");
        form.add(btnWeb, c);
        c.insets = new Insets(18, 0, 0, 0);
        form.add(Tema.textoSuave("<html><div style='width:300px'><b>Usuarios de prueba</b><br>"
                + "Jefe: 10000001 / jefe123<br>Supervisor: 20000001 / super123<br>"
                + "Empleado: 30000001 / emp123</div></html>"), c);

        contenedor.add(form);
        return contenedor;
    }

    /** Campo con su etiqueta arriba y una ayuda debajo; la etiqueta queda asociada para lectores de pantalla. */
    private static JPanel campo(String etiqueta, JTextField campo, String ayuda) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        JLabel lbl = Tema.texto(etiqueta);
        lbl.setFont(Tema.NEGRITA);
        lbl.setLabelFor(campo);
        campo.setFont(Tema.FUENTE.deriveFont(14f));
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x8590A2)), Tema.margen(8, 10, 8, 10)));
        campo.setToolTipText(ayuda);
        campo.getAccessibleContext().setAccessibleName(etiqueta);
        panel.add(lbl, BorderLayout.NORTH);
        panel.add(campo, BorderLayout.CENTER);
        panel.add(Tema.textoSuave(ayuda), BorderLayout.SOUTH);
        return panel;
    }

    private static Component separador(String texto) {
        JLabel lbl = Tema.textoSuave(texto);
        lbl.setHorizontalAlignment(JLabel.CENTER);
        return lbl;
    }

    public JTextField getTxtCedula() {
        return txtCedula;
    }

    public JPasswordField getTxtClave() {
        return txtClave;
    }

    public JButton getBtnEntrar() {
        return btnEntrar;
    }

    public JButton getBtnWeb() {
        return btnWeb;
    }

    public JLabel getLblError() {
        return lblError;
    }
}
