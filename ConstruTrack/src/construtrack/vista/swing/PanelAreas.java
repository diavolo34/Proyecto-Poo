package construtrack.vista.swing;

import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

public final class PanelAreas extends JPanel {
    private static final long serialVersionUID = 1L;

    private final PanelTabla areas;
    private final PanelTabla personas;
    private final JButton btnNueva = Tema.botonPrimario("Nueva área…");
    private final JButton btnSupervisor = Tema.botonSecundario("Cambiar supervisor…");
    private final JButton btnEliminar = Tema.botonPeligro("Eliminar área");
    private final JButton btnIncluir = Tema.botonPrimario("Incluir en un área…");
    private final JButton btnSacar = Tema.botonSecundario("Sacar del área");

    public PanelAreas(boolean esGerente) {
        super(new GridLayout(2, 1, 0, 12));
        setBorder(Tema.margen(12, 4, 4, 4));
        areas = new PanelTabla(esGerente ? "Áreas" : "Mis áreas", "Cada área tiene un supervisor responsable: los empleados que "
                + "entran a un área quedan a cargo de su supervisor.", "Área", "Supervisor", "Integrantes");
        personas = new PanelTabla("Personas", esGerente ? "Incluye, mueve o saca a las personas de cada área."
                : "Puedes sumar a tu área a quien todavía no tiene una.", "Cédula", "Persona", "Rol", "Puesto", "Área", "Supervisor");
        areas.anchos(220, 220, 90);
        personas.anchos(90, 200, 90, 180, 150, 180);
        if (esGerente) {
            areas.agregarBoton(btnNueva);
            areas.agregarBoton(btnSupervisor);
            areas.agregarBoton(btnEliminar);
        }
        personas.agregarBoton(btnIncluir);
        if (esGerente) {
            personas.agregarBoton(btnSacar);
        }
        add(areas);
        add(personas);
    }

    public PanelTabla getAreas() {
        return areas;
    }

    public PanelTabla getPersonas() {
        return personas;
    }

    public JButton getBtnNueva() {
        return btnNueva;
    }

    public JButton getBtnSupervisor() {
        return btnSupervisor;
    }

    public JButton getBtnEliminar() {
        return btnEliminar;
    }

    public JButton getBtnIncluir() {
        return btnIncluir;
    }

    public JButton getBtnSacar() {
        return btnSacar;
    }
}
