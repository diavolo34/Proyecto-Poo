package construtrack.controlador.swing;

import construtrack.modelo.RevisorIncidencias;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.modelo.incidencias.EstadoIncidencia;
import construtrack.modelo.incidencias.Gravedad;
import construtrack.modelo.incidencias.Incidencia;
import construtrack.servicio.ServicioIncidencias;
import construtrack.vista.swing.PanelTablero;
import construtrack.vista.swing.TarjetaIncidencia;
import construtrack.vista.swing.Tema;
import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;

public final class ControladoraTablero {
    private final PanelTablero vista;
    private final ServicioIncidencias incidencias;
    private final Usuario usuario;
    private final Runnable alCambiar;

    public ControladoraTablero(PanelTablero vista, ServicioIncidencias incidencias, Usuario usuario,
            Runnable alCambiar) {
        this.vista = vista;
        this.incidencias = incidencias;
        this.usuario = usuario;
        this.alCambiar = alCambiar;
    }

    public static String[] columnas() {
        EstadoIncidencia[] estados = EstadoIncidencia.values();
        String[] nombres = new String[estados.length];
        for (int i = 0; i < estados.length; i++) {
            nombres[i] = estados[i].getNombre();
        }
        return nombres;
    }

    public void refrescar() {
        vista.limpiar();
        List<Incidencia> lista = incidencias.listarVisibles(usuario);
        lista.sort(Comparator.comparing(Incidencia::getGravedad).reversed()
                .thenComparing(Comparator.comparingInt(Incidencia::getId).reversed()));
        for (Incidencia i : lista) {
            vista.agregarTarjeta(i.getEstado().getNombre(), tarjeta(i));
        }
        vista.actualizar();
    }

    private TarjetaIncidencia tarjeta(Incidencia i) {
        Color[] colores = colores(i.getGravedad());
        String fechas = i.getDesde().equals(i.getHasta()) ? i.getDesde().toString() : i.getDesde() + " → " + i.getHasta();
        String descripcion = i.getDescripcion().length() > 80 ? i.getDescripcion().substring(0, 77) + "…" : i.getDescripcion();
        TarjetaIncidencia tarjeta = new TarjetaIncidencia("#" + i.getId() + " · " + i.getNombreTipo(),
                i.getAfectado().getNombre(), fechas, descripcion, i.getGravedad().getNombre(), colores[0], colores[1]);
        if (usuario instanceof RevisorIncidencias revisor && revisor.puedeRevisar(i) && !i.getEstado().esFinal()) {
            if (i.getEstado() == EstadoIncidencia.PENDIENTE) {
                tarjeta.agregarAccion(accion(Tema.botonSecundario("Revisar"), i, EstadoIncidencia.EN_REVISION));
            }
            tarjeta.agregarAccion(accion(Tema.botonExito("Aprobar"), i, EstadoIncidencia.APROBADA));
            tarjeta.agregarAccion(accion(Tema.botonPeligro("Rechazar"), i, EstadoIncidencia.RECHAZADA));
        }
        return tarjeta;
    }

    private JButton accion(JButton boton, Incidencia incidencia, EstadoIncidencia nuevo) {
        boton.setToolTipText("Pasar la incidencia #" + incidencia.getId() + " a " + nuevo.getNombre().toLowerCase());
        boton.addActionListener(e -> {
            String comentario = "";
            if (nuevo == EstadoIncidencia.RECHAZADA) {
                comentario = JOptionPane.showInputDialog(vista,
                        "¿Por qué se rechaza la incidencia #" + incidencia.getId() + "?\nEl trabajador verá este motivo.",
                        "Motivo del rechazo", JOptionPane.QUESTION_MESSAGE);
                if (comentario == null) {
                    return;
                }
            }
            try {
                incidencias.revisar(usuario, incidencia.getId(), nuevo, comentario);
                vista.mostrarMensaje("la incidencia #" + incidencia.getId() + " pasó a "
                        + nuevo.getNombre().toLowerCase() + ".", false);
                alCambiar.run();
            } catch (ConstruTrackException ex) {
                vista.mostrarMensaje(ex.getMessage(), true);
            }
        });
        return boton;
    }

    private static Color[] colores(Gravedad gravedad) {
        return switch (gravedad) {
            case ALTA -> new Color[] {Tema.ROJO, Tema.ROJO_SUAVE};
            case MEDIA -> new Color[] {Tema.AMARILLO, Tema.AMARILLO_SUAVE};
            case BAJA -> new Color[] {Tema.AZUL, Tema.AZUL_SUAVE};
        };
    }
}
