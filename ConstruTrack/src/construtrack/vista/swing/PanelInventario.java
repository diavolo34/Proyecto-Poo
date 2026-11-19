package construtrack.vista.swing;

import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

public final class PanelInventario extends JPanel {
    private static final long serialVersionUID = 1L;

    private final PanelTabla articulos = new PanelTabla("Inventario", "Herramientas, equipos y materiales de la empresa. Entrega cada "
            + "artículo a un supervisor o empleado y registra la devolución con su estado; todo queda en el historial.",
            "Código", "Artículo", "Categoría", "Marca / serie", "Disponibles", "Total", "Estado");
    private final PanelTabla entregas = new PanelTabla("Entregas activas", null, "Id", "Artículo", "Tiene", "Cantidad", "Proyecto",
            "Entregado", "Devolver el");
    private final JButton btnAgregar = Tema.botonPrimario("Agregar…");
    private final JButton btnEditar = Tema.botonSecundario("Editar…");
    private final JButton btnEntregar = Tema.botonSecundario("Entregar…");
    private final JButton btnHistorial = Tema.botonSecundario("Historial");
    private final JButton btnDevolucion = Tema.botonPrimario("Registrar devolución…");

    public PanelInventario() {
        super(new GridLayout(2, 1, 0, 12));
        setBorder(Tema.margen(12, 4, 4, 4));
        articulos.anchos(70, 220, 100, 200, 80, 60, 120);
        entregas.anchos(30, 200, 170, 70, 170, 90, 120);
        articulos.agregarBoton(btnAgregar);
        articulos.agregarBoton(btnEditar);
        articulos.agregarBoton(btnEntregar);
        articulos.agregarBoton(btnHistorial);
        entregas.agregarBoton(btnDevolucion);
        add(articulos);
        add(entregas);
    }

    public PanelTabla getArticulos() {
        return articulos;
    }

    public PanelTabla getEntregas() {
        return entregas;
    }

    public JButton getBtnAgregar() {
        return btnAgregar;
    }

    public JButton getBtnEditar() {
        return btnEditar;
    }

    public JButton getBtnEntregar() {
        return btnEntregar;
    }

    public JButton getBtnHistorial() {
        return btnHistorial;
    }

    public JButton getBtnDevolucion() {
        return btnDevolucion;
    }
}
