package construtrack.controlador.swing;

import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.modelo.incidencias.Incidencia;
import construtrack.servicio.ServicioIncidencias;
import construtrack.vista.swing.Opcion;
import construtrack.vista.swing.PanelIncidencias;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ControladoraIncidencias {
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM");

    private final PanelIncidencias vista;
    private final ServicioIncidencias incidencias;
    private final Usuario usuario;
    private final Runnable alCambiar;

    public ControladoraIncidencias(PanelIncidencias vista, ServicioIncidencias incidencias, Usuario usuario,
            Runnable alCambiar) {
        this.vista = vista;
        this.incidencias = incidencias;
        this.usuario = usuario;
        this.alCambiar = alCambiar;
        llenarCombos();
        vista.getCboTipo().addActionListener(e -> mostrarCamposDelTipo());
        vista.getBtnReportar().addActionListener(e -> reportar());
        mostrarCamposDelTipo();
    }

    private void llenarCombos() {
        incidencias.tipos().forEach((codigo, nombre) -> vista.getCboTipo().addItem(new Opcion(codigo, nombre)));
        for (Usuario u : incidencias.personasReportables(usuario)) {
            vista.getCboPersona().addItem(new Opcion(u.getCedula(),
                    u == usuario ? u.getNombre() + " (yo)" : u.getNombre() + " · " + u.getRol()));
        }
        vista.getCboPersona().setEnabled(vista.getCboPersona().getItemCount() > 1);
    }

    private void mostrarCamposDelTipo() {
        Opcion tipo = (Opcion) vista.getCboTipo().getSelectedItem();
        vista.mostrarExtras(tipo == null ? "" : tipo.valor());
    }

    private void reportar() {
        Opcion tipo = (Opcion) vista.getCboTipo().getSelectedItem();
        Opcion persona = (Opcion) vista.getCboPersona().getSelectedItem();
        if (tipo == null || persona == null) {
            vista.mostrarMensaje("elige el tipo y la persona.", true);
            return;
        }
        try {
            LocalDate desde = LocalDate.parse(vista.getTxtDesde().getText().trim());
            String textoHasta = vista.getTxtHasta().getText().trim();
            LocalDate hasta = textoHasta.isEmpty() ? null : LocalDate.parse(textoHasta);
            Incidencia nueva = incidencias.reportar(usuario, tipo.valor(), persona.valor(), desde, hasta,
                    vista.getTxtDescripcion().getText(), extras(tipo.valor()));
            vista.mostrarMensaje("se reportó la incidencia #" + nueva.getId() + ". Quedó pendiente de revisión.", false);
            vista.limpiar();
            alCambiar.run();
        } catch (DateTimeParseException e) {
            vista.mostrarMensaje("la fecha debe tener el formato AAAA-MM-DD, por ejemplo " + LocalDate.now() + ".", true);
        } catch (ConstruTrackException e) {
            vista.mostrarMensaje(e.getMessage(), true);
        }
    }

    private Map<String, String> extras(String codigoTipo) {
        Map<String, String> extras = new HashMap<>();
        switch (codigoTipo) {
            case "REPOSO_MEDICO" -> {
                extras.put("centroMedico", vista.getTxtCentroMedico().getText());
                extras.put("certificado", vista.getTxtCertificado().getText());
            }
            case "RETARDO" -> extras.put("minutos", String.valueOf(vista.getSpnMinutos().getValue()));
            case "ACCIDENTE_LABORAL" -> {
                extras.put("lugar", vista.getTxtLugar().getText());
                extras.put("requirioAtencion", String.valueOf(vista.getChkAtencion().isSelected()));
            }
            default -> {
            }
        }
        return extras;
    }

    public void refrescar() {
        vista.getModelo().setRowCount(0);
        List<Incidencia> lista = incidencias.listarVisibles(usuario);
        lista.sort(Comparator.comparingInt(Incidencia::getId).reversed());
        for (Incidencia i : lista) {
            vista.getModelo().addRow(new Object[] {i.getId(), i.getNombreTipo(), i.getAfectado().getNombre(),
                FECHA_CORTA.format(i.getDesde()), FECHA_CORTA.format(i.getHasta()), i.getGravedad().getNombre(),
                i.getEstado().getNombre(), i.getDetalle()});
        }
    }
}
