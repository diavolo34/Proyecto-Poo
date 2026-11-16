package construtrack.vista.swing;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

public final class PanelIncidencias extends JPanel {
    private static final long serialVersionUID = 1L;
    public static final String SIN_EXTRAS = "SIN_EXTRAS";

    private final JComboBox<Opcion> cboTipo = new JComboBox<>();
    private final JComboBox<Opcion> cboPersona = new JComboBox<>();
    private final JTextField txtDesde = new JTextField(LocalDate.now().toString(), 10);
    private final JTextField txtHasta = new JTextField(10);
    private final JTextArea txtDescripcion = new JTextArea(3, 24);
    private final CardLayout tarjetasExtras = new CardLayout();
    private final JPanel panelExtras = new JPanel(tarjetasExtras);
    private final JTextField txtCentroMedico = new JTextField(18);
    private final JTextField txtCertificado = new JTextField(12);
    private final JSpinner spnMinutos = new JSpinner(new SpinnerNumberModel(15, 1, 600, 5));
    private final JTextField txtLugar = new JTextField(18);
    private final JCheckBox chkAtencion = new JCheckBox("Requirió atención médica");
    private final JButton btnReportar = Tema.botonPrimario("Reportar incidencia");
    private final JLabel lblMensaje = Tema.texto(" ");
    private final DefaultTableModel modelo =
            Tema.modeloSoloLectura("#", "Tipo", "Persona", "Desde", "Hasta", "Gravedad", "Estado", "Detalle");

    public PanelIncidencias() {
        super(new BorderLayout());
        setBorder(Tema.margen(12, 4, 4, 4));
        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formulario(), tablaIncidencias());
        division.setBorder(null);
        division.setDividerLocation(400);
        division.setResizeWeight(0);
        add(division, BorderLayout.CENTER);
    }

    private JPanel formulario() {
        JPanel tarjeta = Tema.tarjeta(new BorderLayout(0, 12));
        tarjeta.setMinimumSize(new Dimension(360, 0));
        tarjeta.add(Tema.subtitulo("Reportar una incidencia"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int fila = 0;
        fila = campo(form, fila, "Tipo", cboTipo);
        fila = campo(form, fila, "Persona afectada", cboPersona);
        txtDesde.setToolTipText("Formato AAAA-MM-DD");
        txtHasta.setToolTipText("Opcional: déjalo vacío si es un solo día");
        fila = campo(form, fila, "Desde (AAAA-MM-DD)", txtDesde);
        fila = campo(form, fila, "Hasta (opcional)", txtHasta);
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        fila = campo(form, fila, "Descripción", Tema.desplazable(txtDescripcion));

        panelExtras.setOpaque(false);
        panelExtras.add(vacio(), SIN_EXTRAS);
        panelExtras.add(extras(new String[] {"Centro médico", "N.º de certificado"},
                new JComponent[] {txtCentroMedico, txtCertificado}), "REPOSO_MEDICO");
        panelExtras.add(extras(new String[] {"Minutos de retraso"}, new JComponent[] {spnMinutos}), "RETARDO");
        chkAtencion.setOpaque(false);
        panelExtras.add(extras(new String[] {"Lugar de la obra", ""},
                new JComponent[] {txtLugar, chkAtencion}), "ACCIDENTE_LABORAL");
        GridBagConstraints c = restricciones(fila);
        c.gridwidth = 2;
        form.add(panelExtras, c);
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.setOpaque(false);
        arriba.add(form, BorderLayout.NORTH);
        tarjeta.add(arriba, BorderLayout.CENTER);

        JPanel abajo = new JPanel(new BorderLayout(0, 8));
        abajo.setOpaque(false);
        abajo.add(btnReportar, BorderLayout.NORTH);
        abajo.add(lblMensaje, BorderLayout.SOUTH);
        tarjeta.add(abajo, BorderLayout.SOUTH);
        return tarjeta;
    }

    private JPanel tablaIncidencias() {
        JPanel tarjeta = Tema.tarjeta(new BorderLayout(0, 10));
        tarjeta.add(Tema.subtitulo("Incidencias"), BorderLayout.NORTH);
        JTable tabla = Tema.tabla(modelo);
        tabla.getColumnModel().getColumn(0).setMaxWidth(40);
        Tema.anchos(tabla, 30, 130, 120, 55, 55, 65, 85, 170);
        tarjeta.add(Tema.desplazable(tabla), BorderLayout.CENTER);
        return tarjeta;
    }

    private static int campo(JPanel form, int fila, String etiqueta, JComponent campo) {
        JLabel lbl = Tema.texto(etiqueta);
        lbl.setFont(Tema.NEGRITA);
        lbl.setLabelFor(campo);
        GridBagConstraints c = restricciones(fila);
        form.add(lbl, c);
        c = restricciones(fila + 1);
        form.add(campo, c);
        return fila + 2;
    }

    private static GridBagConstraints restricciones(int fila) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = fila;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(fila % 2 == 0 ? 8 : 2, 0, 0, 0);
        return c;
    }

    private static JPanel extras(String[] etiquetas, JComponent[] campos) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        int fila = 0;
        for (int i = 0; i < campos.length; i++) {
            if (etiquetas[i].isEmpty()) {
                panel.add(campos[i], restricciones(fila));
                fila += 2;
            } else {
                fila = campo(panel, fila, etiquetas[i], campos[i]);
            }
        }
        return panel;
    }

    private static JPanel vacio() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    public void mostrarExtras(String codigoTipo) {
        boolean tieneExtras = "REPOSO_MEDICO".equals(codigoTipo) || "RETARDO".equals(codigoTipo)
                || "ACCIDENTE_LABORAL".equals(codigoTipo);
        tarjetasExtras.show(panelExtras, tieneExtras ? codigoTipo : SIN_EXTRAS);
    }

    public void mostrarMensaje(String texto, boolean esError) {
        lblMensaje.setText("<html>" + (esError ? "Error: " : "Listo: ") + texto + "</html>");
        lblMensaje.setForeground(esError ? Tema.ROJO : Tema.VERDE);
    }

    public void limpiar() {
        txtDescripcion.setText("");
        txtHasta.setText("");
        txtCentroMedico.setText("");
        txtCertificado.setText("");
        txtLugar.setText("");
        chkAtencion.setSelected(false);
    }

    public JComboBox<Opcion> getCboTipo() {
        return cboTipo;
    }

    public JComboBox<Opcion> getCboPersona() {
        return cboPersona;
    }

    public JTextField getTxtDesde() {
        return txtDesde;
    }

    public JTextField getTxtHasta() {
        return txtHasta;
    }

    public JTextArea getTxtDescripcion() {
        return txtDescripcion;
    }

    public JTextField getTxtCentroMedico() {
        return txtCentroMedico;
    }

    public JTextField getTxtCertificado() {
        return txtCertificado;
    }

    public JSpinner getSpnMinutos() {
        return spnMinutos;
    }

    public JTextField getTxtLugar() {
        return txtLugar;
    }

    public JCheckBox getChkAtencion() {
        return chkAtencion;
    }

    public JButton getBtnReportar() {
        return btnReportar;
    }

    public DefaultTableModel getModelo() {
        return modelo;
    }
}
