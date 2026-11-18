package construtrack.controlador.swing;

import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.modelo.novedades.Categoria;
import construtrack.modelo.novedades.EstadoNovedad;
import construtrack.modelo.novedades.Novedad;
import construtrack.servicio.ServicioNovedades;
import construtrack.vista.swing.Dialogos;
import construtrack.vista.swing.PanelTablero;
import construtrack.vista.swing.TarjetaNovedad;
import construtrack.vista.swing.Tema;
import java.util.Comparator;
import java.util.List;
import javax.swing.JButton;

public final class ControladoraTablero {
    private final PanelTablero vista;
    private final ServicioNovedades novedades;
    private final Usuario usuario;
    private final Runnable alCambiar;

    public ControladoraTablero(PanelTablero vista, ServicioNovedades novedades, Usuario usuario, Runnable alCambiar) {
        this.vista = vista;
        this.novedades = novedades;
        this.usuario = usuario;
        this.alCambiar = alCambiar;
    }

    public static String[] columnas() {
        EstadoNovedad[] estados = EstadoNovedad.values();
        String[] nombres = new String[estados.length];
        for (int i = 0; i < estados.length; i++) {
            nombres[i] = estados[i].getNombre();
        }
        return nombres;
    }

    public void refrescar() {
        vista.limpiar();
        List<Novedad> lista = novedades.listarTablero(usuario).stream()
                .sorted(Comparator.comparing(Novedad::getFechaRegistro).reversed()).toList();
        for (Novedad n : lista) {
            vista.agregarTarjeta(n.getEstado().getNombre(), tarjeta(n));
        }
        vista.actualizar();
    }

    private TarjetaNovedad tarjeta(Novedad n) {
        boolean licencia = n.getCategoria() == Categoria.LICENCIA;
        String fechas = n.getDesde().equals(n.getHasta()) ? n.getDesde().toString() : n.getDesde() + " → " + n.getHasta();
        String descripcion = n.getDescripcion().length() > 80 ? n.getDescripcion().substring(0, 77) + "…" : n.getDescripcion();
        if (n.getRevisadaPor() != null) {
            descripcion += "<br>" + (n.getEstado() == EstadoNovedad.APROBADA ? "Autorizó: " : "Rechazó: ") + n.getRevisadaPor().nombre();
        }
        TarjetaNovedad tarjeta = new TarjetaNovedad("#" + n.getId() + " · " + n.getNombreTipo(), n.getAfectado().getNombre(), fechas,
                descripcion, n.getCategoria().getNombre(), licencia ? Tema.MORADO : Tema.AMARILLO,
                licencia ? Tema.MORADO_SUAVE : Tema.AMARILLO_SUAVE);
        if (novedades.puedeRevisar(usuario, n)) {
            tarjeta.agregarAccion(accion(Tema.botonExito("Aprobar"), n, EstadoNovedad.APROBADA));
            tarjeta.agregarAccion(accion(Tema.botonPeligro("Rechazar"), n, EstadoNovedad.RECHAZADA));
        }
        return tarjeta;
    }

    private JButton accion(JButton boton, Novedad n, EstadoNovedad nuevo) {
        boolean aprobar = nuevo == EstadoNovedad.APROBADA;
        boton.setToolTipText((aprobar ? "Aprobar" : "Rechazar") + " #" + n.getId());
        boton.addActionListener(e -> {
            String nota = Dialogos.pedirTexto(vista, aprobar ? "Aprobar" : "Rechazar", (aprobar ? "Nota de aprobación" : "Motivo del rechazo")
                    + " para #" + n.getId() + " (" + n.getNombreTipo() + "):\nObligatoria; la verá quien lo registró.");
            if (nota == null) {
                return;
            }
            try {
                novedades.revisar(usuario, n.getId(), nuevo, nota);
                vista.mostrarMensaje("#" + n.getId() + (aprobar ? " aprobado." : " rechazado."), false);
                alCambiar.run();
            } catch (ConstruTrackException ex) {
                vista.mostrarMensaje(ex.getMessage(), true);
            }
        });
        return boton;
    }
}
