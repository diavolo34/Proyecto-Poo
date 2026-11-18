package construtrack.vista.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class DialogoCodigo extends JDialog {
    private static final long serialVersionUID = 1L;

    private final JTextField txtCodigo = new JTextField(12);
    private final JLabel lblMensaje = Tema.texto(" ");
    private final JButton btnConfirmar;
    private final JButton btnSecundario;
    private final JButton btnCancelar = Tema.botonSecundario("Cancelar");

    public DialogoCodigo(Window dueno, String titulo, String explicacion, String etiqueta, String textoConfirmar,
            String textoSecundario) {
        super(dueno, titulo, ModalityType.APPLICATION_MODAL);
        btnConfirmar = Tema.botonPrimario(textoConfirmar);
        btnSecundario = textoSecundario == null ? null : Tema.botonSecundario(textoSecundario);
        JPanel contenido = new JPanel(new BorderLayout(0, 14));
        contenido.setBackground(Tema.SUPERFICIE);
        contenido.setBorder(Tema.margen(22, 26, 18, 26));

        JPanel encabezado = new JPanel(new BorderLayout(0, 6));
        encabezado.setOpaque(false);
        encabezado.add(Tema.subtitulo(titulo), BorderLayout.NORTH);
        encabezado.add(Tema.textoSuave("<html><div style='width:360px'>" + explicacion + "</div></html>"), BorderLayout.CENTER);
        contenido.add(encabezado, BorderLayout.NORTH);

        txtCodigo.setFont(Tema.SUBTITULO.deriveFont(20f));
        contenido.add(new Formulario().campo(etiqueta, txtCodigo, "Escríbelo sin espacios").panel(), BorderLayout.CENTER);

        JPanel abajo = new JPanel(new BorderLayout());
        abajo.setOpaque(false);
        abajo.add(lblMensaje, BorderLayout.NORTH);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        botones.setOpaque(false);
        botones.add(btnCancelar);
        if (btnSecundario != null) {
            botones.add(btnSecundario);
        }
        botones.add(btnConfirmar);
        abajo.add(botones, BorderLayout.SOUTH);
        contenido.add(abajo, BorderLayout.SOUTH);

        setContentPane(contenido);
        getRootPane().setDefaultButton(btnConfirmar);
        pack();
        setLocationRelativeTo(dueno);
    }

    public void mostrarError(String mensaje) {
        lblMensaje.setForeground(Tema.ROJO);
        lblMensaje.setText("<html><div style='width:360px'>Error: " + mensaje + "</div></html>");
        txtCodigo.selectAll();
        txtCodigo.requestFocusInWindow();
        pack();
    }

    public void mostrarAviso(String mensaje) {
        lblMensaje.setForeground(Tema.VERDE);
        lblMensaje.setText("<html><div style='width:360px'>" + mensaje + "</div></html>");
        pack();
    }

    public String codigo() {
        return txtCodigo.getText().trim();
    }

    public JButton getBtnConfirmar() {
        return btnConfirmar;
    }

    public JButton getBtnSecundario() {
        return btnSecundario;
    }

    public JButton getBtnCancelar() {
        return btnCancelar;
    }
}
