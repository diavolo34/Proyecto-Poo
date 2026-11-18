package construtrack.vista.swing;

import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

public final class PanelTalento extends JPanel {
    private static final long serialVersionUID = 1L;

    private final PanelTabla activos = new PanelTabla("Personal activo", "Despidos y renuncias con voucher de pago, carta de renuncia "
            + "y devolución de equipos. Quien egresa queda inactivo: no entra ni aparece en el personal, pero su historial se conserva.",
            "Cédula", "Nombre", "Rol", "Puesto", "Área", "Ingreso");
    private final PanelTabla egresos = new PanelTabla("Egresos", null, "Id", "Persona", "Cédula", "Tipo", "Fecha", "Motivo",
            "Equipos", "Ficha");
    private final JButton btnEgreso = Tema.botonPeligro("Registrar egreso…");
    private final JButton btnVoucher = Tema.botonSecundario("Ver voucher");
    private final JButton btnCarta = Tema.botonSecundario("Ver carta de renuncia");
    private final JButton btnExpediente = Tema.botonSecundario("Guardar expediente…");
    private final JButton btnEliminar = Tema.botonPeligro("Eliminar ficha");

    public PanelTalento() {
        super(new GridLayout(2, 1, 0, 12));
        setBorder(Tema.margen(12, 4, 4, 4));
        activos.anchos(90, 180, 90, 180, 130, 90);
        egresos.anchos(30, 170, 90, 80, 90, 220, 200, 90);
        activos.agregarBoton(btnEgreso);
        egresos.agregarBoton(btnVoucher);
        egresos.agregarBoton(btnCarta);
        egresos.agregarBoton(btnExpediente);
        egresos.agregarBoton(btnEliminar);
        add(activos);
        add(egresos);
    }

    public PanelTabla getActivos() {
        return activos;
    }

    public PanelTabla getEgresos() {
        return egresos;
    }

    public JButton getBtnEgreso() {
        return btnEgreso;
    }

    public JButton getBtnVoucher() {
        return btnVoucher;
    }

    public JButton getBtnCarta() {
        return btnCarta;
    }

    public JButton getBtnExpediente() {
        return btnExpediente;
    }

    public JButton getBtnEliminar() {
        return btnEliminar;
    }
}
