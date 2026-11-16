package construtrack.modelo;

import static construtrack.Datos.AHORA;
import static construtrack.Datos.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.Datos;
import construtrack.modelo.alertas.Alerta;
import construtrack.modelo.alertas.AusenciaSinJustificar;
import construtrack.modelo.alertas.MonitorSupervision;
import construtrack.modelo.alertas.Nivel;
import construtrack.modelo.alertas.ReglaAlerta;
import construtrack.modelo.alertas.RetardosReiterados;
import construtrack.modelo.reportes.Documento;
import construtrack.modelo.reportes.FormateadorCSV;
import construtrack.modelo.reportes.ReporteAsistencia;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlertasYReportesTest {
    private final Datos d = new Datos();
    private final Periodo semana = Periodo.semanaDe(HOY);
    private final LocalDate lunes = LocalDate.of(2026, 11, 16);
    private final LocalDate martes = LocalDate.of(2026, 11, 17);

    private void tardeLunesYMartes(Usuario u) {
        u.agregarAsistencia(new RegistroAsistencia(lunes, LocalTime.of(7, 30), true));
        u.agregarAsistencia(new RegistroAsistencia(martes, LocalTime.of(7, 40), true));
    }

    @Test
    @DisplayName("La regla de retardos se dispara al llegar al umbral")
    void reglaRetardos() {
        tardeLunesYMartes(d.jose);
        assertTrue(new RetardosReiterados(3).evaluar(d.jose, semana, HOY, AHORA).isEmpty());
        List<Alerta> alertas = new RetardosReiterados(2).evaluar(d.jose, semana, HOY, AHORA);
        assertEquals(1, alertas.size());
        assertEquals(Nivel.MEDIA, alertas.get(0).getNivel());
    }

    @Test
    @DisplayName("El monitor aplica todas las reglas solo a empleados; a igual antigüedad, primero la más grave")
    void monitorConReglasInyectadas() {
        tardeLunesYMartes(d.jose);

        MonitorSupervision monitor = new MonitorSupervision(List.<ReglaAlerta>of(new RetardosReiterados(2), new AusenciaSinJustificar()));
        List<Alerta> alertas = monitor.evaluar(d.empresa.listarUsuarios(), semana, HOY, AHORA);
        assertEquals(Nivel.ALTA, alertas.get(0).getNivel());
        assertEquals("Laura Torres", alertas.get(0).getNombre());
        assertTrue(alertas.stream().noneMatch(a -> a.getNombre().equals("Ana Rodríguez")), "Los supervisores no se evalúan");
    }

    @Test
    @DisplayName("Las alertas salen de la más antigua a la más reciente, aunque la nueva sea más grave")
    void alertasPorAntiguedad() {
        ReglaAlerta regla = (empleado, periodo, hoy, ahora) -> switch (empleado.getNombre()) {
            case "Laura Torres" -> List.of(Alerta.de(Nivel.MEDIA, empleado, "Desde el lunes", lunes.atStartOfDay(), ahora));
            case "José García" -> List.of(Alerta.de(Nivel.ALTA, empleado, "Desde el martes", martes.atStartOfDay(), ahora));
            default -> List.of();
        };
        List<Alerta> alertas = new MonitorSupervision(List.of(regla)).evaluar(d.empresa.listarUsuarios(), semana, HOY, AHORA);
        assertEquals(List.of("Laura Torres", "José García"), alertas.stream().map(Alerta::getNombre).toList());
    }

    @Test
    @DisplayName("El reporte de asistencia (Template Method) produce una fila por persona con sus cifras")
    void reporteAsistencia() {
        tardeLunesYMartes(d.jose);
        Documento doc = new ReporteAsistencia().generar(List.of(d.jose, d.laura), semana, HOY);
        assertEquals(13, doc.getColumnas().size(), "8 de asistencia + 5 de horas trabajadas y extra");

        assertEquals(List.of("30000001", "José García", "Empleado", "2", "2", "100.0", "0", "0", "0.0", "18.0", "0.0", "0.0", "0.0"),
                doc.getFilas().get(0).stream().map(c -> c.replace(',', '.')).toList());
        assertEquals("2", doc.getFilas().get(1).get(6), "Laura tiene 2 ausencias sin justificar");
    }

    @Test
    @DisplayName("El CSV usa ; como separador, BOM UTF-8 y escapa comillas")
    void formatoCsv() {
        Documento doc = new Documento("Prueba");
        doc.definirColumnas(List.of("Nombre", "Detalle"));
        doc.agregarFila("José", "dijo \"hola\"; adiós");
        String csv = new FormateadorCSV().formatear(doc);
        assertTrue(csv.startsWith("﻿Nombre;Detalle\r\n"));
        assertTrue(csv.contains("José;\"dijo \"\"hola\"\"; adiós\""));
    }

    @Test
    @DisplayName("Las estadísticas suman asistencias y retardos del grupo")
    void estadisticas() {
        tardeLunesYMartes(d.jose);
        Estadisticas e = new Estadisticas(List.of(d.jose, d.laura), semana, HOY);
        assertEquals(50.0, e.tasaAsistencia(), 0.001, "2 jornadas de 4 días laborables");
        assertEquals(0.0, e.porcentajePuntualidad(), 0.001);
        assertEquals("José García", e.ranking().get(0).nombre());
    }
}
