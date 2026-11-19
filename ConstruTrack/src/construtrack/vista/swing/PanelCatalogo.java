package construtrack.vista.swing;

import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class PanelCatalogo extends JPanel {
    private static final long serialVersionUID = 1L;

    private final PanelTabla puestos = new PanelTabla("Puestos", "El rol define los permisos; el puesto describe el cargo de cada "
            + "persona. Las áreas, con su supervisor, se gestionan en la pestaña «Áreas».", "Puesto");
    private final JTextField txtPuesto = new JTextField(20);
    private final JButton btnAgregarPuesto = Tema.botonPrimario("Agregar");
    private final JButton btnQuitarPuesto = Tema.botonPeligro("Quitar");

    public PanelCatalogo() {
        super(new BorderLayout());
        setBorder(Tema.margen(12, 4, 4, 4));
        txtPuesto.getAccessibleContext().setAccessibleName("Nuevo puesto");
        puestos.agregarComponente(txtPuesto);
        puestos.agregarBoton(btnAgregarPuesto);
        puestos.agregarBoton(btnQuitarPuesto);
        add(puestos, BorderLayout.CENTER);
    }

    public PanelTabla getPuestos() {
        return puestos;
    }

    public JTextField getTxtPuesto() {
        return txtPuesto;
    }

    public JButton getBtnAgregarPuesto() {
        return btnAgregarPuesto;
    }

    public JButton getBtnQuitarPuesto() {
        return btnQuitarPuesto;
    }
}
