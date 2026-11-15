package construtrack.vista.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Vista: formulario modal para registrar o editar a una persona.
 * Usa listas desplegables y casillas para que el usuario elija en lugar de escribir (prevención de errores).
 */
public final class DialogoUsuario extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final String[] DIAS = {"LU", "MA", "MI", "JU", "VI", "SA", "DO"};

    private final JComboBox<String> cboRol = new JComboBox<>(new String[] {"Empleado", "Supervisor"});
    private final JTextField txtCedula = new JTextField(16);
    private final JTextField txtNombre = new JTextField(22);
    private final JPasswordField txtClave = new JPasswordField(16);
    private final transient List<JCheckBox> chkDias = new ArrayList<>();
    private final JComboBox<String> cboEntrada = new JComboBox<>(horas());
    private final JComboBox<String> cboSalida = new JComboBox<>(horas());
    private final JComboBox<Opcion> cboSupervisor = new JComboBox<>();
    private final JCheckBox chkPresente = new JCheckBox("Está en la obra ahora");
    private final JLabel lblError = Tema.texto(" ");
    private final JButton btnGuardar = Tema.botonPrimario("Guardar");
    private final JButton btnCancelar = Tema.botonSecundario("Cancelar");
    private final boolean edicion;

    public DialogoUsuario(Frame dueno, boolean edicion) {
        super(dueno, edicion ? "Editar persona" : "Registrar persona", true);
        this.edicion = edicion;
        JPanel contenido = new JPanel(new BorderLayout(0, 12));
        contenido.setBackground(Tema.SUPERFICIE);
        contenido.setBorder(Tema.margen(20, 24, 20, 24));
        contenido.add(Tema.subtitulo(edicion ? "Editar persona" : "Registrar persona"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int fila = 0;
        fila = agregar(form, fila, "Rol", cboRol, null);
        fila = agregar(form, fila, "Cédula", txtCedula, "Entre 6 y 9 dígitos, sin puntos");
        fila = agregar(form, fila, "Nombre completo", txtNombre, null);
        fila = agregar(form, fila, "Clave", txtClave,
                edicion ? "Déjala vacía para no cambiarla" : "Mínimo 4 caracteres, sin espacios");
        JPanel dias = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        dias.setOpaque(false);
        for (String dia : DIAS) {
            JCheckBox chk = new JCheckBox(dia, !"SA".equals(dia) && !"DO".equals(dia));
            chk.setOpaque(false);
            chkDias.add(chk);
            dias.add(chk);
        }
        fila = agregar(form, fila, "Días de trabajo", dias, null);
        cboEntrada.setSelectedItem("07:00");
        cboSalida.setSelectedItem("16:00");
        JPanel horas = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        horas.setOpaque(false);
        horas.add(cboEntrada);
        horas.add(Tema.texto("a"));
        horas.add(cboSalida);
        fila = agregar(form, fila, "Horario", horas, "Se toleran 10 minutos de retraso");
        fila = agregar(form, fila, "Supervisor", cboSupervisor, "Solo aplica a empleados");
        chkPresente.setOpaque(false);
        agregar(form, fila, "", chkPresente, null);
        contenido.add(form, BorderLayout.CENTER);

        JPanel abajo = new JPanel(new BorderLayout());
        abajo.setOpaque(false);
        lblError.setForeground(Tema.ROJO);
        abajo.add(lblError, BorderLayout.NORTH);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        botones.setOpaque(false);
        botones.add(btnCancelar);
        botones.add(btnGuardar);
        abajo.add(botones, BorderLayout.SOUTH);
        contenido.add(abajo, BorderLayout.SOUTH);

        setContentPane(contenido);
        getRootPane().setDefaultButton(btnGuardar);
        txtCedula.setEnabled(!edicion);
        cboRol.addActionListener(e -> cboSupervisor.setEnabled("Empleado".equals(cboRol.getSelectedItem())));
        pack();
        setLocationRelativeTo(dueno);
    }

    private static int agregar(JPanel form, int fila, String etiqueta, JComponent campo, String ayuda) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = fila;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 0, 0, 14);
        JLabel lbl = Tema.texto(etiqueta);
        lbl.setFont(Tema.NEGRITA);
        lbl.setLabelFor(campo);
        form.add(lbl, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(6, 0, 0, 0);
        form.add(campo, c);
        if (ayuda != null) {
            c.gridy = fila + 1;
            c.insets = new Insets(0, 0, 0, 0);
            form.add(Tema.textoSuave(ayuda), c);
            return fila + 2;
        }
        return fila + 1;
    }

    private static String[] horas() {
        List<String> horas = new ArrayList<>();
        for (int h = 5; h <= 21; h++) {
            horas.add(String.format("%02d:00", h));
            horas.add(String.format("%02d:30", h));
        }
        return horas.toArray(new String[0]);
    }

    public void cargarSupervisores(List<Opcion> supervisores) {
        cboSupervisor.removeAllItems();
        cboSupervisor.addItem(new Opcion("", "— Sin supervisor —"));
        supervisores.forEach(cboSupervisor::addItem);
    }

    /** Llena el formulario con los datos de una persona existente (modo edición). */
    public void cargar(String rol, String cedula, String nombre, String dias, String entrada, String salida,
            String cedulaSupervisor, boolean presente) {
        cboRol.setSelectedItem(rol);
        txtCedula.setText(cedula);
        txtNombre.setText(nombre);
        for (JCheckBox chk : chkDias) {
            chk.setSelected(dias.contains(chk.getText()));
        }
        cboEntrada.setSelectedItem(entrada);
        cboSalida.setSelectedItem(salida);
        for (int i = 0; i < cboSupervisor.getItemCount(); i++) {
            if (cboSupervisor.getItemAt(i).valor().equals(cedulaSupervisor)) {
                cboSupervisor.setSelectedIndex(i);
            }
        }
        chkPresente.setSelected(presente);
    }

    /** Datos del formulario con las claves que espera el modelo (Jefe.registrarUsuario / modificarUsuario). */
    public Map<String, String> datos() {
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("rol", String.valueOf(cboRol.getSelectedItem()));
        datos.put("cedula", txtCedula.getText().trim());
        datos.put("nombre", txtNombre.getText().trim());
        String clave = new String(txtClave.getPassword());
        if (!edicion || !clave.isEmpty()) {
            datos.put("clave", clave);
        }
        List<String> dias = new ArrayList<>();
        for (JCheckBox chk : chkDias) {
            if (chk.isSelected()) {
                dias.add(chk.getText());
            }
        }
        datos.put("dias", String.join(",", dias));
        datos.put("horaEntrada", String.valueOf(cboEntrada.getSelectedItem()));
        datos.put("horaSalida", String.valueOf(cboSalida.getSelectedItem()));
        Opcion supervisor = (Opcion) cboSupervisor.getSelectedItem();
        datos.put("supervisor", supervisor == null ? "" : supervisor.valor());
        datos.put("presente", String.valueOf(chkPresente.isSelected()));
        return datos;
    }

    public void mostrarError(String mensaje) {
        lblError.setText("Error: " + mensaje);
        pack();
    }

    public JButton getBtnGuardar() {
        return btnGuardar;
    }

    public JButton getBtnCancelar() {
        return btnCancelar;
    }
}
