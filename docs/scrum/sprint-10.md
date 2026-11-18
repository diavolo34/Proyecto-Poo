# Sprint 10 · Refactorización pedida por el cliente

**Objetivo:** adaptar el producto al lenguaje y a los procesos del cliente: permisos y reposos con aprobación, reportes en
obra, ciclo de obra, talento humano e inventario, en la web y en Swing.
**Historias:** HU-36 a HU-45.

## Sprint Backlog
- **Gerente** (HU-36): el rol «Jefe» pasa a «Gerente» en clases, textos y documentos; los datos guardados con «Jefe» se
  siguen leyendo.
- **Permisos y reposos** (HU-37, HU-38):
  - jerarquía `Novedad` → `Licencia` (permiso laboral, reposo médico) y `ReporteObra` (retardo, inasistencia, accidente);
  - el supervisor registra, el gerente aprueba o rechaza con nota obligatoria y queda como autorizador (`Firma`);
  - el reposo pide médico, matrícula, centro asistencial, tipo de consulta y el documento, guardado en Google Drive;
  - un reposo aprobado marca los días como «Incapacidad» y bloquea la marca; alerta `FaltasConsecutivas`.
- **Reportes en obra** (HU-39): sin gravedad, con la obra; el empleado no tiene acceso.
- **Obras** (HU-40): Planificada → En curso → Concluida; `RetrasoObra` con motivo y nueva fecha, alerta para el gerente.
- **Catálogo de puestos y áreas** (HU-41) y «Personal» o «Área» en vez de «equipo».
- **Talento humano** (HU-42): `Egreso` con voucher, carta y devolución de equipos; desactivación y expediente.
- **Inventario** (HU-43): `Inventario`, `Articulo` y `Asignacion`, inspirado en herramientas como Hilti ON!Track,
  ShareMyToolbox y GoCodes (entrega por persona, fecha de devolución, estado al recibir e historial).
- **Ubicación en cada marca y mapa** (HU-44) y **ojo de la clave y portada** (HU-45).
- Las invitaciones vencidas o canceladas se borran solas.

## Incremento
- 105 pruebas JUnit (14 nuevas): permisos y reposos, incapacidad, faltas seguidas, archivos y Google Drive, egresos, inventario,
  ciclo de obra, retrasos, catálogo, invitaciones caducadas y persistencia de todo lo nuevo.
- CI con pruebas de humo de los recursos nuevos de la API.

## Review
Recorrido en el navegador:
1. La supervisora registra un reposo con foto; sin documento, el sistema lo rechaza.
2. El gerente lo ve, abre la foto, intenta aprobar sin nota (rechazado) y lo aprueba con nota.
3. El trabajador ve su incapacidad y el botón de entrada bloqueado; no ve los reportes en obra.
4. La supervisora reporta un retraso y el gerente lo ve como alerta y lo marca revisado.
5. El gerente carga una herramienta, la entrega y registra la devolución con daños.
6. Registra una renuncia con voucher y carta, sin devolución de equipos; la persona ya no puede entrar.
También se revisaron las 11 pestañas del gerente en Swing.

## Retrospectiva
- **Bien:** usar una `Firma` (cédula y nombre) en lugar de referencias a `Usuario` evitó datos rotos al eliminar fichas.
- **Mejorar:** una vista que se recargaba mientras se cambiaba de pantalla mostraba un error; ahora cada vista se detiene
  si ya no está en pantalla.
- **Para el futuro:** etiquetas QR para el inventario y aviso por correo al aprobar o rechazar.
