package construtrack.vista.swing;

import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class PanelCatalogo extends JPanel {
    private static final long serialVersionUID = 1L;

    private final PanelTabla puestos = new PanelTabla("Puestos", "El rol define los permisos; el puesto describe el cargo.", "Puesto");
    private final PanelTabla areas = new PanelTabla("Áreas", "El departamento al que pertenece cada persona.", "Área");
    private final JTextField txtPuesto = new JTextField(16);
    private final JTextField txtArea = new JTextField(16);
    private final JButton btnAgregarPuesto = Tema.botonPrimario("Agregar");
    private final JButton btnQuitarPuesto = Tema.botonPeligro("Quitar");
    private final JButton btnAgregarArea = Tema.botonPrimario("Agregar");
    private final JButton btnQuitarArea = Tema.botonPeligro("Quitar");

    public PanelCatalogo() {
        super(new GridLayout(1, 2, 12, 0));
        setBorder(Tema.margen(12, 4, 4, 4));
        txtPuesto.getAccessibleContext().setAccessibleName("Nuevo puesto");
        txtArea.getAccessibleContext().setAccessibleName("Nueva área");
        puestos.agregarComponente(txtPuesto);
        puestos.agregarBoton(btnAgregarPuesto);
        puestos.agregarBoton(btnQuitarPuesto);
        areas.agregarComponente(txtArea);
        areas.agregarBoton(btnAgregarArea);
        areas.agregarBoton(btnQuitarArea);
        add(puestos);
        add(areas);
    }

    public PanelTabla getPuestos() {
        return puestos;
    }

    public PanelTabla getAreas() {
        return areas;
    }

    public JTextField getTxtPuesto() {
        return txtPuesto;
    }

    public JTextField getTxtArea() {
        return txtArea;
    }

    public JButton getBtnAgregarPuesto() {
        return btnAgregarPuesto;
    }

    public JButton getBtnQuitarPuesto() {
        return btnQuitarPuesto;
    }

    public JButton getBtnAgregarArea() {
        return btnAgregarArea;
    }

    public JButton getBtnQuitarArea() {
        return btnQuitarArea;
    }
}
