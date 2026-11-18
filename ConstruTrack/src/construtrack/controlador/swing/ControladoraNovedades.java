package construtrack.controlador.swing;

import construtrack.modelo.Obra;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.modelo.novedades.Categoria;
import construtrack.modelo.novedades.EstadoNovedad;
import construtrack.modelo.novedades.Novedad;
import construtrack.servicio.ServicioArchivos;
import construtrack.servicio.ServicioNovedades;
import construtrack.vista.swing.Dialogos;
import construtrack.vista.swing.Opcion;
import construtrack.vista.swing.PanelNovedades;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ControladoraNovedades {
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM");

    private final PanelNovedades vista;
    private final ServicioNovedades novedades;
    private final ServicioArchivos archivos;
    private final Usuario usuario;
    private final Categoria categoria;
    private final Runnable alCambiar;
    private Path documento;
    private List<Novedad> mostradas = List.of();

    public ControladoraNovedades(PanelNovedades vista, ServicioNovedades novedades, ServicioArchivos archivos, Usuario usuario,
            Categoria categoria, Runnable alCambiar) {
        this.vista = vista;
        this.novedades = novedades;
        this.archivos = archivos;
        this.usuario = usuario;
        this.categoria = categoria;
        this.alCambiar = alCambiar;
        llenarCombos();
        vista.getCboTipo().addActionListener(e -> mostrarCamposDelTipo());
        vista.getBtnRegistrar().addActionListener(e -> registrar());
        vista.getBtnArchivo().addActionListener(e -> elegirDocumento());
        vista.getCboFiltro().addActionListener(e -> refrescar());
        vista.getBtnAprobar().addActionListener(e -> revisar(EstadoNovedad.APROBADA));
        vista.getBtnRechazar().addActionListener(e -> revisar(EstadoNovedad.RECHAZADA));
        vista.getBtnDocumento().addActionListener(e -> verDocumento());
        mostrarCamposDelTipo();
    }

    public static boolean puedeRegistrar(ServicioNovedades novedades, Usuario usuario) {
        return !novedades.personasRegistrables(usuario).isEmpty();
    }

    private void llenarCombos() {
        novedades.tipos(categoria).forEach((codigo, nombre) -> vista.getCboTipo().addItem(new Opcion(codigo, nombre)));
        for (Usuario u : novedades.personasRegistrables(usuario)) {
            vista.getCboPersona().addItem(new Opcion(u.getCedula(), u == usuario ? u.getNombre() + " (yo)"
                    : u.getNombre() + (u.getPuesto() == null ? "" : " · " + u.getPuesto())));
        }
        vista.getCboObra().addItem(new Opcion("", "— La obra de la persona —"));
        if (usuario.getEmpresa() != null) {
            for (Obra o : usuario.getEmpresa().listarObras()) {
                vista.getCboObra().addItem(new Opcion(o.getNombre(), o.getNombre()));
            }
        }
    }

    private void mostrarCamposDelTipo() {
        Opcion tipo = (Opcion) vista.getCboTipo().getSelectedItem();
        vista.mostrarExtras(tipo == null ? "" : tipo.valor());
    }

    private void elegirDocumento() {
        Path elegido = Dialogos.elegirDocumento(vista, "Imagen o PDF del reposo");
        if (elegido != null) {
            documento = elegido;
            vista.getLblArchivo().setText(elegido.getFileName().toString());
        }
    }

    private void registrar() {
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
            Map<String, String> extras = extras(tipo.valor(), persona.valor());
            Novedad nueva = novedades.registrar(usuario, categoria, tipo.valor(), persona.valor(), desde, hasta,
                    vista.getTxtDescripcion().getText(), extras);
            vista.mostrarMensaje(nueva.getNombreTipo() + " #" + nueva.getId() + " registrado. Queda pendiente hasta que lo apruebe el gerente.",
                    false);
            vista.limpiar();
            documento = null;
            alCambiar.run();
        } catch (DateTimeParseException e) {
            vista.mostrarMensaje("la fecha debe tener el formato AAAA-MM-DD, por ejemplo " + LocalDate.now() + ".", true);
        } catch (IOException e) {
            vista.mostrarMensaje("no se pudo leer el documento: " + e.getMessage(), true);
        } catch (ConstruTrackException e) {
            vista.mostrarMensaje(e.getMessage(), true);
        }
    }

    private Map<String, String> extras(String codigoTipo, String cedula) throws IOException {
        Map<String, String> extras = new HashMap<>();
        Opcion obra = (Opcion) vista.getCboObra().getSelectedItem();
        if (categoria == Categoria.REPORTE_OBRA && obra != null && !obra.valor().isEmpty()) {
            extras.put("obra", obra.valor());
        }
        switch (codigoTipo) {
            case "REPOSO_MEDICO" -> {
                extras.put("medico", vista.getTxtMedico().getText());
                extras.put("matricula", vista.getTxtMatricula().getText());
                extras.put("centro", vista.getTxtCentro().getText());
                extras.put("tipoConsulta", ((Opcion) vista.getCboConsulta().getSelectedItem()).valor());
                if (documento != null) {
                    extras.put("archivo", archivos.subir(usuario, documento.getFileName().toString(), Files.readAllBytes(documento),
                            cedula, false).id());
                }
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

    private Novedad seleccionada() {
        Object id = vista.getTabla().valorSeleccionado(0);
        if (id == null) {
            vista.getTabla().mostrarMensaje("selecciona una fila de la tabla.", true);
            return null;
        }
        return mostradas.stream().filter(n -> n.getId() == (Integer) id).findFirst().orElse(null);
    }

    private void revisar(EstadoNovedad nuevo) {
        Novedad n = seleccionada();
        if (n == null) {
            return;
        }
        boolean aprobar = nuevo == EstadoNovedad.APROBADA;
        String nota = Dialogos.pedirTexto(vista, aprobar ? "Aprobar" : "Rechazar", (aprobar ? "Nota de aprobación" : "Motivo del rechazo")
                + " para " + n.getNombreTipo() + " #" + n.getId() + " de " + n.getAfectado().getNombre() + ":\n(Obligatoria; la verá el supervisor)");
        if (nota == null) {
            return;
        }
        try {
            novedades.revisar(usuario, n.getId(), nuevo, nota);
            vista.getTabla().mostrarMensaje(n.getNombreTipo() + " #" + n.getId() + (aprobar ? " aprobado." : " rechazado."), false);
            alCambiar.run();
        } catch (ConstruTrackException e) {
            vista.getTabla().mostrarMensaje(e.getMessage(), true);
        }
    }

    private void verDocumento() {
        Novedad n = seleccionada();
        if (n == null) {
            return;
        }
        if (n.getArchivo() == null) {
            vista.getTabla().mostrarMensaje("este registro no tiene documento adjunto.", true);
            return;
        }
        try {
            ServicioArchivos.Descarga d = archivos.descargar(usuario, n.getArchivo());
            Dialogos.abrir(vista, d.nombre(), d.datos());
        } catch (ConstruTrackException e) {
            vista.getTabla().mostrarMensaje(e.getMessage(), true);
        }
    }

    public void refrescar() {
        String filtro = ((Opcion) vista.getCboFiltro().getSelectedItem()).valor();
        mostradas = novedades.listar(usuario, categoria).stream()
                .filter(n -> filtro.isEmpty() || n.getEstado().name().equals(filtro)).toList();
        vista.getTabla().limpiar();
        for (Novedad n : mostradas) {
            String fechas = FECHA_CORTA.format(n.getDesde()) + (n.getHasta().equals(n.getDesde()) ? "" : " – " + FECHA_CORTA.format(n.getHasta()));
            vista.getTabla().fila(n.getId(), n.getNombreTipo(), n.getAfectado().getNombre(), fechas, n.getEstado().getNombre(),
                    n.getRevisadaPor() == null ? "—" : n.getRevisadaPor().nombre(),
                    n.getDescripcion() + (n.getDetalle().isEmpty() ? "" : " · " + n.getDetalle()), n.getNota());
        }
    }
}
