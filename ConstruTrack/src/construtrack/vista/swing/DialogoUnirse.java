package construtrack.vista.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Vista: la persona con cuenta escribe el código de invitación para unirse a la empresa. */
public final class DialogoUnirse extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField txtCodigo = new JTextField(16);
    private final JLabel lblError = Tema.texto(" ");
    private final JButton btnUnirse = Tema.botonPrimario("Unirme a la empresa");
    private final JButton btnMasTarde = Tema.botonSecundario("Más tarde");

    public DialogoUnirse(Window dueno, String nombre) {
        super(dueno, "Únete a tu empresa", ModalityType.APPLICATION_MODAL);
        JPanel contenido = new JPanel(new BorderLayout(0, 14));
        contenido.setBackground(Tema.SUPERFICIE);
        contenido.setBorder(Tema.margen(22, 26, 18, 26));

        JPanel encabezado = new JPanel(new BorderLayout(0, 6));
        encabezado.setOpaque(false);
        encabezado.add(Tema.subtitulo("¡Hola, " + nombre + "! Únete a tu empresa"), BorderLayout.NORTH);
        encabezado.add(Tema.textoSuave("<html><div style='width:360px'>Tu cuenta ya está creada. Escribe el código de "
                + "invitación que te dio tu jefe (formato CT-XXXX-XXXX). Al unirte tendrás el rol, el supervisor y la "
                + "obra que él definió.</div></html>"), BorderLayout.CENTER);
        contenido.add(encabezado, BorderLayout.NORTH);

        txtCodigo.setFont(Tema.SUBTITULO.deriveFont(18f));
        txtCodigo.setToolTipText("Puedes escribirlo en minúsculas; los espacios se ignoran");
        contenido.add(new Formulario().campo("Código de invitación *", txtCodigo, "Ej.: CT-RQ7K-4M2P").panel(),
                BorderLayout.CENTER);

        JPanel abajo = new JPanel(new BorderLayout());
        abajo.setOpaque(false);
        lblError.setForeground(Tema.ROJO);
        abajo.add(lblError, BorderLayout.NORTH);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        botones.setOpaque(false);
        botones.add(btnMasTarde);
        botones.add(btnUnirse);
        abajo.add(botones, BorderLayout.SOUTH);
        contenido.add(abajo, BorderLayout.SOUTH);

        setContentPane(contenido);
        getRootPane().setDefaultButton(btnUnirse);
        pack();
        setLocationRelativeTo(dueno);
    }

    public void mostrarError(String mensaje) {
        lblError.setText("<html><div style='width:360px'>Error: " + mensaje + "</div></html>");
        pack();
    }

    public JTextField getTxtCodigo() {
        return txtCodigo;
    }

    public JButton getBtnUnirse() {
        return btnUnirse;
    }

    public JButton getBtnMasTarde() {
        return btnMasTarde;
    }
}
