package construtrack.controlador.swing;

import construtrack.modelo.Historial;
import construtrack.modelo.Horario;
import construtrack.modelo.Periodo;
import construtrack.modelo.RegistroAsistencia;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.servicio.ServicioAsistencia;
import construtrack.vista.swing.PanelAsistencia;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ControladoraAsistencia {
    private static final int MAX_FILAS = 30;

    private final PanelAsistencia vista;
    private final ServicioAsistencia asistencia;
    private final Usuario usuario;
    private final Runnable alCambiar;

    public ControladoraAsistencia(PanelAsistencia vista, ServicioAsistencia asistencia, Usuario usuario,
            Runnable alCambiar) {
        this.vista = vista;
        this.asistencia = asistencia;
        this.usuario = usuario;
        this.alCambiar = alCambiar;
        vista.getBtnEntrada().addActionListener(e -> marcar(true));
        vista.getBtnSalida().addActionListener(e -> marcar(false));
    }

    private void marcar(boolean entrada) {
        try {
            RegistroAsistencia r = entrada ? asistencia.marcarEntrada(usuario) : asistencia.marcarSalida(usuario);
            String control = r.getVerificacion() == construtrack.modelo.EstadoVerificacion.POR_CONFIRMAR
                    ? " Queda por confirmar (" + r.getMotivoVerificacion().toLowerCase() + "): el escritorio no envía ubicación."
                    : "";
            vista.mostrarMensaje((entrada
                    ? "entrada registrada a las " + r.getHoraEntrada() + (r.isTarde() ? " (llegada tarde)." : ".")
                    : "salida registrada a las " + r.getHoraSalida() + ".") + control, false);
            alCambiar.run();
        } catch (ConstruTrackException e) {
            vista.mostrarMensaje(e.getMessage(), true);
        }
    }

    public void refrescar() {
        Horario h = usuario.getHorario();
        vista.getLblSaludo().setText("Hola, " + usuario.getNombre().split(" ")[0]);
        vista.getLblHorario().setText("Horario: " + h.getDias() + " de " + h.getHoraEntrada() + " a "
                + h.getHoraSalida() + (usuario.getObra() == null ? "" : " · Obra: " + usuario.getObra().getNombre()));
        vista.mostrarEstado(usuario.estaPresente());
        construtrack.modelo.novedades.Licencia incapacidad = usuario.getHistorial().incapacidadEl(LocalDate.now());
        if (incapacidad != null) {
            vista.getLblHorario().setText(vista.getLblHorario().getText() + " · Incapacidad: reposo médico aprobado hasta el "
                    + incapacidad.getHasta());
            vista.getBtnEntrada().setEnabled(false);
        }

        Historial historial = usuario.getHistorial();
        Periodo semana = Periodo.semanaDe(LocalDate.now());
        vista.getLblJornadas().setText(String.valueOf(historial.asistenciasEn(semana).size()));
        vista.getLblRetardos().setText(String.valueOf(historial.contarRetardos(semana)));
        vista.getLblPuntualidad().setText(String.format("%.0f%%", historial.porcentajePuntualidad(semana)));

        vista.getModelo().setRowCount(0);
        List<RegistroAsistencia> recientes = new ArrayList<>(historial.getAsistencias());
        Collections.reverse(recientes);
        for (RegistroAsistencia r : recientes.subList(0, Math.min(MAX_FILAS, recientes.size()))) {
            vista.getModelo().addRow(new Object[] {r.getFecha(), r.getHoraEntrada(),
                r.getHoraSalida() == null ? "En curso" : r.getHoraSalida(),
                r.isTarde() ? "Tarde (" + r.minutosRetraso(h) + " min)" : "Puntual",
                r.estaAbierto() ? "—" : String.format("%.1f h", r.minutosTrabajados() / 60.0),
                r.getVerificacion() == construtrack.modelo.EstadoVerificacion.NO_APLICA ? "—"
                        : r.getVerificacion().getNombre() + (r.getMotivoVerificacion().isEmpty() ? "" : " · " + r.getMotivoVerificacion())});
        }
    }
}
