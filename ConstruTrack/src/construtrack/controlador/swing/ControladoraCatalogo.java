package construtrack.controlador.swing;

import construtrack.modelo.Catalogo;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.servicio.ServicioCatalogo;
import construtrack.vista.swing.PanelCatalogo;
import construtrack.vista.swing.PanelTabla;
import javax.swing.JTextField;

public final class ControladoraCatalogo {
    private final PanelCatalogo vista;
    private final ServicioCatalogo catalogo;
    private final Usuario usuario;
    private final Runnable alCambiar;

    public ControladoraCatalogo(PanelCatalogo vista, ServicioCatalogo catalogo, Usuario usuario, Runnable alCambiar) {
        this.vista = vista;
        this.catalogo = catalogo;
        this.usuario = usuario;
        this.alCambiar = alCambiar;
        vista.getBtnAgregarPuesto().addActionListener(e -> agregar(ServicioCatalogo.Tipo.PUESTOS, vista.getTxtPuesto(), vista.getPuestos()));
        vista.getBtnQuitarPuesto().addActionListener(e -> quitar(ServicioCatalogo.Tipo.PUESTOS, vista.getPuestos()));
    }

    private void agregar(ServicioCatalogo.Tipo tipo, JTextField campo, PanelTabla tabla) {
        try {
            String nombre = catalogo.agregar(usuario, tipo, campo.getText());
            campo.setText("");
            tabla.mostrarMensaje("«" + nombre + "» agregado.", false);
            alCambiar.run();
        } catch (ConstruTrackException e) {
            tabla.mostrarMensaje(e.getMessage(), true);
        }
    }

    private void quitar(ServicioCatalogo.Tipo tipo, PanelTabla tabla) {
        Object nombre = tabla.valorSeleccionado(0);
        if (nombre == null) {
            tabla.mostrarMensaje("selecciona un elemento de la lista.", true);
            return;
        }
        try {
            catalogo.quitar(usuario, tipo, nombre.toString());
            tabla.mostrarMensaje("«" + nombre + "» quitado del catálogo.", false);
            alCambiar.run();
        } catch (ConstruTrackException e) {
            tabla.mostrarMensaje(e.getMessage(), true);
        }
    }

    public void refrescar() {
        Catalogo c = catalogo.ver(usuario);
        vista.getPuestos().limpiar();
        c.getPuestos().forEach(p -> vista.getPuestos().fila(p));
    }
}
