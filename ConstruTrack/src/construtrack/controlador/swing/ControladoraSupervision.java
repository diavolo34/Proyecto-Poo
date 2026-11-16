package construtrack.controlador.swing;

import construtrack.modelo.Estadisticas;
import construtrack.modelo.Usuario;
import construtrack.modelo.alertas.Alerta;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.servicio.Exportacion;
import construtrack.servicio.ServicioSupervision;
import construtrack.vista.swing.Opcion;
import construtrack.vista.swing.PanelSupervision;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;

public final class ControladoraSupervision {
    private final PanelSupervision vista;
    private final ServicioSupervision supervision;
    private final Usuario usuario;

    public ControladoraSupervision(PanelSupervision vista, ServicioSupervision supervision, Usuario usuario) {
        this.vista = vista;
        this.supervision = supervision;
        this.usuario = usuario;
        vista.getCboPeriodo().addActionListener(e -> refrescar());
        vista.getBtnExportar().addActionListener(e -> exportar());
    }

    private String periodo() {
        return ((Opcion) vista.getCboPeriodo().getSelectedItem()).valor();
    }

    public void refrescar() {
        Estadisticas e = supervision.estadisticas(usuario, periodo());
        vista.getLblAsistencia().setText(String.format("%.0f%%", e.tasaAsistencia()));
        vista.getLblPuntualidad().setText(String.format("%.0f%%", e.porcentajePuntualidad()));
        vista.getLblHoras().setText(String.format("%.0f h", e.minutosTrabajados() / 60.0));

        List<String> nombres = new ArrayList<>();
        List<Double> tasas = new ArrayList<>();
        for (Estadisticas.Resumen r : e.resumenes()) {
            String[] partes = r.nombre().split(" ");
            nombres.add(partes.length > 1 ? partes[0] + " " + partes[1].charAt(0) + "." : partes[0]);
            tasas.add(r.tasaAsistencia());
        }
        vista.getGrafico().setDatos(nombres, tasas, 100, "%");

        List<Alerta> alertas = supervision.alertas(usuario, periodo());
        vista.getModeloAlertas().clear();
        alertas.forEach(vista.getModeloAlertas()::addElement);
        vista.getLblAlertas().setText(String.valueOf(alertas.size()));
    }

    private void exportar() {
        String tipo = ((Opcion) vista.getCboReporte().getSelectedItem()).valor();
        String formato = ((Opcion) vista.getCboFormato().getSelectedItem()).valor();
        try {
            Exportacion exportacion = supervision.exportar(usuario, tipo, formato, periodo());
            JFileChooser selector = new JFileChooser();
            selector.setDialogTitle("Guardar reporte");
            selector.setSelectedFile(new File(exportacion.nombreArchivo()));
            if (selector.showSaveDialog(vista) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            Files.writeString(selector.getSelectedFile().toPath(), exportacion.contenido(), StandardCharsets.UTF_8);
            vista.mostrarMensaje("reporte guardado en " + selector.getSelectedFile().getName() + ".", false);
        } catch (ConstruTrackException e) {
            vista.mostrarMensaje(e.getMessage(), true);
        } catch (IOException e) {
            vista.mostrarMensaje("no se pudo guardar el archivo: " + e.getMessage(), true);
        }
    }
}
