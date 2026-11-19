package construtrack.vista.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public final class PanelPersonal extends JPanel {
    private static final long serialVersionUID = 1L;

    private final DefaultTableModel modelo =
            Tema.modeloSoloLectura("Cédula", "Nombre", "Rol", "Puesto", "Área", "Estado", "Horario", "Supervisor", "Proyecto");
    private final JTable tabla = Tema.tabla(modelo);
    private final JTextField txtBuscar = new JTextField(22);
    private final JButton btnRegistrar = Tema.botonPrimario("+ Registrar persona");
    private final JButton btnEditar = Tema.botonSecundario("Editar");
    private final JLabel lblMensaje = Tema.texto(" ");

    public PanelPersonal(String titulo, boolean puedeEditar) {
        super(new BorderLayout(0, 10));
        setBorder(Tema.margen(12, 4, 4, 4));

        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        barra.add(Tema.titulo(titulo), BorderLayout.WEST);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        JLabel lblBuscar = Tema.texto("Buscar:");
        lblBuscar.setLabelFor(txtBuscar);
        txtBuscar.setToolTipText("Filtra por nombre, cédula, rol, puesto, área o proyecto mientras escribes");
        acciones.add(lblBuscar);
        acciones.add(txtBuscar);
        if (puedeEditar) {
            btnEditar.setToolTipText("Edita a la persona seleccionada en la tabla");
            acciones.add(btnEditar);
            acciones.add(btnRegistrar);
        }
        barra.add(acciones, BorderLayout.EAST);
        add(barra, BorderLayout.NORTH);

        JPanel tarjeta = Tema.tarjeta(new BorderLayout());
        tabla.getAccessibleContext().setAccessibleName("Personal");
        Tema.anchos(tabla, 80, 150, 80, 140, 110, 80, 200, 130, 150);
        tarjeta.add(Tema.desplazable(tabla), BorderLayout.CENTER);
        add(tarjeta, BorderLayout.CENTER);
        add(lblMensaje, BorderLayout.SOUTH);
    }

    public void mostrarMensaje(String texto, boolean esError) {
        lblMensaje.setText((esError ? "Error: " : "Listo: ") + texto);
        lblMensaje.setForeground(esError ? Tema.ROJO : Tema.VERDE);
    }

    public DefaultTableModel getModelo() {
        return modelo;
    }

    public JTable getTabla() {
        return tabla;
    }

    public JTextField getTxtBuscar() {
        return txtBuscar;
    }

    public JButton getBtnRegistrar() {
        return btnRegistrar;
    }

    public JButton getBtnEditar() {
        return btnEditar;
    }

}
