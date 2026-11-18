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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

public final class PanelNovedades extends JPanel {
    private static final long serialVersionUID = 1L;
    public static final String SIN_EXTRAS = "SIN_EXTRAS";

    private final JComboBox<Opcion> cboTipo = new JComboBox<>();
    private final JComboBox<Opcion> cboPersona = new JComboBox<>();
    private final JComboBox<Opcion> cboObra = new JComboBox<>();
    private final JTextField txtDesde = new JTextField(LocalDate.now().toString(), 10);
    private final JTextField txtHasta = new JTextField(10);
    private final JTextArea txtDescripcion = new JTextArea(3, 24);
    private final CardLayout tarjetasExtras = new CardLayout();
    private final JPanel panelExtras = new JPanel(tarjetasExtras);
    private final JTextField txtMedico = new JTextField(18);
    private final JTextField txtMatricula = new JTextField(12);
    private final JTextField txtCentro = new JTextField(18);
    private final JComboBox<Opcion> cboConsulta = new JComboBox<>(new Opcion[] {
        new Opcion("INTERNA", "Interna"), new Opcion("EMERGENCIA", "Por emergencia")});
    private final JButton btnArchivo = Tema.botonSecundario("Adjuntar imagen o PDF del reposo…");
    private final JLabel lblArchivo = Tema.textoSuave("Ningún archivo elegido");
    private final JSpinner spnMinutos = new JSpinner(new SpinnerNumberModel(15, 1, 600, 5));
    private final JTextField txtLugar = new JTextField(18);
    private final JCheckBox chkAtencion = new JCheckBox("Requirió atención médica");
    private final JButton btnRegistrar = Tema.botonPrimario("Registrar");
    private final JLabel lblMensaje = Tema.texto(" ");
    private final JComboBox<Opcion> cboFiltro = new JComboBox<>(new Opcion[] {
        new Opcion("PENDIENTE", "Pendientes"), new Opcion("APROBADA", "Aprobados"), new Opcion("RECHAZADA", "Rechazados"),
        new Opcion("", "Todos")});
    private final JButton btnAprobar = Tema.botonExito("Aprobar con nota");
    private final JButton btnRechazar = Tema.botonPeligro("Rechazar con nota");
    private final JButton btnDocumento = Tema.botonSecundario("Ver documento");
    private final PanelTabla tabla;

    public PanelNovedades(String titulo, String ayuda, boolean puedeRegistrar, boolean conObra, boolean puedeRevisar) {
        super(new BorderLayout());
        setBorder(Tema.margen(12, 4, 4, 4));
        tabla = new PanelTabla(titulo, ayuda, "#", "Tipo", "Persona", "Fechas", "Estado", "Autorizó / rechazó", "Detalle", "Nota");
        tabla.anchos(30, 120, 120, 90, 75, 120, 220, 160);
        tabla.getTabla().getColumnModel().getColumn(0).setMaxWidth(44);
        tabla.agregarComponente(Tema.textoSuave("Mostrar:"));
        tabla.agregarComponente(cboFiltro);
        if (puedeRevisar) {
            tabla.agregarBoton(btnAprobar);
            tabla.agregarBoton(btnRechazar);
        }
        tabla.agregarBoton(btnDocumento);
        if (!puedeRegistrar) {
            add(tabla, BorderLayout.CENTER);
            return;
        }
        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formulario(conObra), tabla);
        division.setBorder(null);
        division.setDividerLocation(400);
        division.setResizeWeight(0);
        add(division, BorderLayout.CENTER);
    }

    private JComponent formulario(boolean conObra) {
        JPanel tarjeta = Tema.tarjeta(new BorderLayout(0, 12));
        tarjeta.setMinimumSize(new Dimension(360, 0));
        tarjeta.add(Tema.subtitulo("Registrar"), BorderLayout.NORTH);
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int fila = 0;
        fila = campo(form, fila, "Tipo", cboTipo);
        fila = campo(form, fila, "Persona", cboPersona);
        if (conObra) {
            fila = campo(form, fila, "Obra", cboObra);
        }
        txtDesde.setToolTipText("Formato AAAA-MM-DD");
        txtHasta.setToolTipText("Opcional: déjalo vacío si es un solo día");
        fila = campo(form, fila, "Desde (AAAA-MM-DD)", txtDesde);
        fila = campo(form, fila, "Hasta (opcional)", txtHasta);
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        fila = campo(form, fila, conObra ? "Qué pasó" : "Motivo", Tema.desplazable(txtDescripcion));
        panelExtras.setOpaque(false);
        panelExtras.add(vacio(), SIN_EXTRAS);
        lblArchivo.setBorder(Tema.margen(4, 0, 0, 0));
        JPanel archivo = new JPanel(new BorderLayout());
        archivo.setOpaque(false);
        archivo.add(btnArchivo, BorderLayout.NORTH);
        archivo.add(lblArchivo, BorderLayout.SOUTH);
        panelExtras.add(extras(new String[] {"Nombre del médico", "Matrícula médica", "Centro asistencial", "Tipo de consulta",
            "Documento del reposo"}, new JComponent[] {txtMedico, txtMatricula, txtCentro, cboConsulta, archivo}), "REPOSO_MEDICO");
        panelExtras.add(extras(new String[] {"Minutos de retraso"}, new JComponent[] {spnMinutos}), "RETARDO");
        chkAtencion.setOpaque(false);
        panelExtras.add(extras(new String[] {"Lugar de la obra", ""}, new JComponent[] {txtLugar, chkAtencion}), "ACCIDENTE_LABORAL");
        GridBagConstraints c = restricciones(fila);
        c.gridwidth = 2;
        form.add(panelExtras, c);
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.setOpaque(false);
        arriba.add(form, BorderLayout.NORTH);
        tarjeta.add(Tema.desplazable(arriba), BorderLayout.CENTER);
        JPanel abajo = new JPanel(new BorderLayout(0, 8));
        abajo.setOpaque(false);
        abajo.add(btnRegistrar, BorderLayout.NORTH);
        abajo.add(lblMensaje, BorderLayout.SOUTH);
        tarjeta.add(abajo, BorderLayout.SOUTH);
        return tarjeta;
    }

    private static int campo(JPanel form, int fila, String etiqueta, JComponent campo) {
        JLabel lbl = Tema.texto(etiqueta);
        lbl.setFont(Tema.NEGRITA);
        lbl.setLabelFor(campo);
        form.add(lbl, restricciones(fila));
        form.add(campo, restricciones(fila + 1));
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
        txtMedico.setText("");
        txtMatricula.setText("");
        txtCentro.setText("");
        txtLugar.setText("");
        chkAtencion.setSelected(false);
        lblArchivo.setText("Ningún archivo elegido");
    }

    public JComboBox<Opcion> getCboTipo() {
        return cboTipo;
    }

    public JComboBox<Opcion> getCboPersona() {
        return cboPersona;
    }

    public JComboBox<Opcion> getCboObra() {
        return cboObra;
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

    public JTextField getTxtMedico() {
        return txtMedico;
    }

    public JTextField getTxtMatricula() {
        return txtMatricula;
    }

    public JTextField getTxtCentro() {
        return txtCentro;
    }

    public JComboBox<Opcion> getCboConsulta() {
        return cboConsulta;
    }

    public JButton getBtnArchivo() {
        return btnArchivo;
    }

    public JLabel getLblArchivo() {
        return lblArchivo;
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

    public JButton getBtnRegistrar() {
        return btnRegistrar;
    }

    public JComboBox<Opcion> getCboFiltro() {
        return cboFiltro;
    }

    public JButton getBtnAprobar() {
        return btnAprobar;
    }

    public JButton getBtnRechazar() {
        return btnRechazar;
    }

    public JButton getBtnDocumento() {
        return btnDocumento;
    }

    public PanelTabla getTabla() {
        return tabla;
    }
}
