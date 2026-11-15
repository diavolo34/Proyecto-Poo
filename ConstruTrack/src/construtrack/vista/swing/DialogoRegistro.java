package construtrack.vista.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Vista: registro de una cuenta nueva con correo y datos personales.
 * Pide solo lo necesario y explica el siguiente paso (unirse con un código), para que nadie se pierda.
 */
public final class DialogoRegistro extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField txtCorreo = new JTextField(24);
    private final JTextField txtNombre = new JTextField(12);
    private final JTextField txtApellido = new JTextField(12);
    private final JTextField txtDocumento = new JTextField(12);
    private final JTextField txtTelefono = new JTextField(12);
    private final JPasswordField txtClave = new JPasswordField(12);
    private final JPasswordField txtConfirmacion = new JPasswordField(12);
    private final JLabel lblError = Tema.texto(" ");
    private final JButton btnCrear = Tema.botonPrimario("Crear cuenta");
    private final JButton btnCancelar = Tema.botonSecundario("Cancelar");

    public DialogoRegistro(Window dueno) {
        super(dueno, "Crear una cuenta", ModalityType.APPLICATION_MODAL);
        JPanel contenido = new JPanel(new BorderLayout(0, 12));
        contenido.setBackground(Tema.SUPERFICIE);
        contenido.setBorder(Tema.margen(22, 26, 18, 26));

        JPanel encabezado = new JPanel(new BorderLayout(0, 4));
        encabezado.setOpaque(false);
        encabezado.add(Tema.subtitulo("Crea tu cuenta en ConstruTrack"), BorderLayout.NORTH);
        encabezado.add(Tema.textoSuave("<html><div style='width:380px'>Después de registrarte, escribe el código de "
                + "invitación que te dio tu jefe para unirte a la empresa.</div></html>"), BorderLayout.CENTER);
        contenido.add(encabezado, BorderLayout.NORTH);

        Formulario f = new Formulario()
                .campo("Correo electrónico *", txtCorreo, "Lo usarás para iniciar sesión")
                .campo("Nombre *", txtNombre, null, 0, 1)
                .campo("Apellido *", txtApellido, null, 1, 1)
                .campo("Cédula o DNI *", txtDocumento, "Sin puntos ni guiones", 0, 1)
                .campo("Teléfono *", txtTelefono, "Ej.: +58 412 1234567", 1, 1)
                .campo("Clave *", txtClave, "Mínimo 4 caracteres", 0, 1)
                .campo("Repite la clave *", txtConfirmacion, null, 1, 1);
        contenido.add(f.panel(), BorderLayout.CENTER);

        JPanel abajo = new JPanel(new BorderLayout());
        abajo.setOpaque(false);
        lblError.setForeground(Tema.ROJO);
        abajo.add(lblError, BorderLayout.NORTH);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        botones.setOpaque(false);
        botones.add(btnCancelar);
        botones.add(btnCrear);
        abajo.add(botones, BorderLayout.SOUTH);
        contenido.add(abajo, BorderLayout.SOUTH);

        setContentPane(contenido);
        getRootPane().setDefaultButton(btnCrear);
        pack();
        setLocationRelativeTo(dueno);
    }

    /** Datos con las claves que espera el servicio de registro. */
    public Map<String, String> datos() {
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("correo", txtCorreo.getText());
        datos.put("nombre", txtNombre.getText());
        datos.put("apellido", txtApellido.getText());
        datos.put("documento", txtDocumento.getText());
        datos.put("telefono", txtTelefono.getText());
        datos.put("clave", new String(txtClave.getPassword()));
        datos.put("confirmacion", new String(txtConfirmacion.getPassword()));
        return datos;
    }

    public void mostrarError(String mensaje) {
        lblError.setText("<html><div style='width:380px'>Error: " + mensaje + "</div></html>");
        pack();
    }

    public JButton getBtnCrear() {
        return btnCrear;
    }

    public JButton getBtnCancelar() {
        return btnCancelar;
    }

    public JTextField getTxtCorreo() {
        return txtCorreo;
    }
}
