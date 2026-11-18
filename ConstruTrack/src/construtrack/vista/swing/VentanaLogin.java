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

public final class VentanaLogin extends JFrame {
    private static final long serialVersionUID = 1L;

    private final JTextField txtIdentificador = new JTextField(18);
    private final JPasswordField txtClave = new JPasswordField(18);
    private final JButton btnEntrar = Tema.botonPrimario("Iniciar sesión");
    private final JButton btnWeb = Tema.botonSecundario("Abrir la versión web");
    private final JButton btnCrearCuenta = Tema.botonSecundario("Crear una cuenta");
    private final JLabel lblError = Tema.textoSuave(" ");

    public VentanaLogin(String nombreEmpresa) {
        this(nombreEmpresa, false);
    }

    public VentanaLogin(String nombreEmpresa, boolean modoDemo) {
        super(nombreEmpresa + " · Iniciar sesión");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 2));
        add(new Ilustracion());
        add(formulario(nombreEmpresa, modoDemo));
        setSize(new Dimension(920, 560));
        setMinimumSize(new Dimension(760, 500));
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(btnEntrar);
    }

    private JPanel formulario(String nombreEmpresa, boolean modoDemo) {
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
        form.add(Tema.textoSuave("<html><div style='width:300px'>Entra con tu correo o tu cédula. "
                + "¿Eres nuevo? Crea tu cuenta y únete con el código que te dé tu gerente.</div></html>"), c);
        c.insets = new Insets(18, 0, 4, 0);
        form.add(campo("Correo o cédula *", txtIdentificador, "Por ejemplo: tu@correo.com o 30000001"), c);
        c.insets = new Insets(4, 0, 4, 0);
        form.add(campo("Clave *", txtClave, "¿La olvidaste? Restablécela desde la página web"), c);
        lblError.setForeground(Tema.ROJO);
        form.add(lblError, c);
        c.insets = new Insets(10, 0, 4, 0);
        btnEntrar.setMnemonic('I');
        form.add(btnEntrar, c);
        c.insets = new Insets(14, 0, 4, 0);
        form.add(separador("¿Primera vez en ConstruTrack?"), c);
        c.insets = new Insets(4, 0, 4, 0);
        btnCrearCuenta.setToolTipText("Regístrate con tu correo; luego te unes a la empresa con un código");
        form.add(btnCrearCuenta, c);
        btnWeb.setToolTipText("Abre la página web de ConstruTrack en tu navegador");
        form.add(btnWeb, c);
        if (modoDemo) {
            c.insets = new Insets(18, 0, 0, 0);
            form.add(Tema.textoSuave("<html><div style='width:300px'><b>Usuarios de prueba</b><br>"
                    + "Gerente: 10000001 / gerente123<br>Supervisor: 20000001 / super123<br>"
                    + "Empleado: 30000001 / emp123</div></html>"), c);
        }

        contenedor.add(form);
        return contenedor;
    }

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

    public JTextField getTxtIdentificador() {
        return txtIdentificador;
    }

    public JButton getBtnCrearCuenta() {
        return btnCrearCuenta;
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
