package construtrack.vista.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

public final class PanelInvitaciones extends JPanel {
    private static final long serialVersionUID = 1L;

    private final JComboBox<String> cboRol = new JComboBox<>(new String[] {"Empleado", "Supervisor"});
    private final JComboBox<Opcion> cboSupervisor = new JComboBox<>();
    private final JComboBox<Opcion> cboObra = new JComboBox<>();
    private final JComboBox<String> cboPuesto = new JComboBox<>();
    private final JComboBox<String> cboArea = new JComboBox<>();
    private final JSpinner spnDias = new JSpinner(new SpinnerNumberModel(7, 1, 30, 1));
    private final JTextField txtCorreo = new JTextField(18);
    private final JButton btnCrear = Tema.botonPrimario("Crear invitación");
    private final JLabel lblCodigo = Tema.texto("—");
    private final JButton btnCopiar = Tema.botonSecundario("Copiar código");
    private final JButton btnCancelar = Tema.botonPeligro("Cancelar invitación");
    private final JLabel lblMensaje = Tema.texto(" ");
    private final DefaultTableModel modelo =
            Tema.modeloSoloLectura("Código", "Rol", "Supervisor", "Obra", "Estado", "Vence", "Usada por");
    private final JTable tabla = Tema.tabla(modelo);

    public PanelInvitaciones() {
        super(new BorderLayout(0, 10));
        setBorder(Tema.margen(12, 4, 4, 4));
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        barra.add(Tema.titulo("Invitaciones"), BorderLayout.WEST);
        barra.add(Tema.textoSuave("Cada código sirve una sola vez. Compártelo por WhatsApp o correo con la persona que "
                + "quieres sumar."), BorderLayout.SOUTH);
        add(barra, BorderLayout.NORTH);

        JPanel formulario = Tema.tarjeta(new BorderLayout(0, 12));
        formulario.add(Tema.subtitulo("Nueva invitación"), BorderLayout.NORTH);
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.setOpaque(false);
        formulario.add(arriba, BorderLayout.CENTER);
        arriba.add(new Formulario()
                .campo("Rol", cboRol, null)
                .campo("Supervisor", cboSupervisor, "Solo para empleados")
                .campo("Puesto", cboPuesto, null, 0, 1)
                .campo("Área", cboArea, null, 1, 1)
                .campo("Obra", cboObra, null)
                .campo("Vigencia (días)", spnDias, "Entre 1 y 30 días")
                .campo("Correo de la persona (opcional)", txtCorreo, "Se registra el envío; no se manda un correo real")
                .panel(), BorderLayout.NORTH);
        JPanel resultado = new JPanel(new BorderLayout(0, 8));
        resultado.setOpaque(false);
        resultado.add(btnCrear, BorderLayout.NORTH);
        JPanel codigo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        codigo.setBackground(Tema.AZUL_SUAVE);
        codigo.setBorder(Tema.margen(10, 6, 10, 6));
        lblCodigo.setFont(Tema.SUBTITULO.deriveFont(20f));
        lblCodigo.setForeground(Tema.AZUL_OSCURO);
        codigo.add(lblCodigo);
        codigo.add(btnCopiar);
        btnCopiar.setEnabled(false);
        resultado.add(codigo, BorderLayout.CENTER);
        resultado.add(lblMensaje, BorderLayout.SOUTH);
        formulario.add(resultado, BorderLayout.SOUTH);

        JPanel lista = Tema.tarjeta(new BorderLayout(0, 10));
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        encabezado.add(Tema.subtitulo("Invitaciones creadas"), BorderLayout.WEST);
        encabezado.add(btnCancelar, BorderLayout.EAST);
        lista.add(encabezado, BorderLayout.NORTH);
        Tema.anchos(tabla, 120, 80, 120, 150, 80, 85, 130);
        lista.add(Tema.desplazable(tabla), BorderLayout.CENTER);

        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formulario, lista);
        division.setBorder(null);
        division.setDividerLocation(360);
        add(division, BorderLayout.CENTER);
        cboRol.addActionListener(e -> cboSupervisor.setEnabled("Empleado".equals(cboRol.getSelectedItem())));
    }

    public void mostrarCodigo(String codigo) {
        lblCodigo.setText(codigo);
        btnCopiar.setEnabled(true);
    }

    public void mostrarMensaje(String texto, boolean esError) {
        lblMensaje.setText("<html>" + (esError ? "Error: " : "Listo: ") + texto + "</html>");
        lblMensaje.setForeground(esError ? Tema.ROJO : Tema.VERDE);
    }

    public JComboBox<String> getCboRol() {
        return cboRol;
    }

    public JComboBox<String> getCboPuesto() {
        return cboPuesto;
    }

    public JComboBox<String> getCboArea() {
        return cboArea;
    }

    public JComboBox<Opcion> getCboSupervisor() {
        return cboSupervisor;
    }

    public JComboBox<Opcion> getCboObra() {
        return cboObra;
    }

    public JSpinner getSpnDias() {
        return spnDias;
    }

    public JTextField getTxtCorreo() {
        return txtCorreo;
    }

    public JButton getBtnCrear() {
        return btnCrear;
    }

    public JLabel getLblCodigo() {
        return lblCodigo;
    }

    public JButton getBtnCopiar() {
        return btnCopiar;
    }

    public JButton getBtnCancelar() {
        return btnCancelar;
    }

    public DefaultTableModel getModelo() {
        return modelo;
    }

    public JTable getTabla() {
        return tabla;
    }
}
