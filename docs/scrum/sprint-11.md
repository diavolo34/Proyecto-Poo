# Sprint 11 · Áreas con supervisor y lenguaje del cliente

**Objetivo:** organizar al personal en áreas, cada una con su supervisor, y usar en toda la aplicación el lenguaje que
pidió el cliente: «proyecto», «Panel de control» y «Registro de personal».
**Historias:** HU-46 a HU-49.

## Sprint Backlog
- **Áreas** (HU-46, HU-47):
  - clase `Area` con su `Supervisor` responsable; `Empresa` crea, elimina, incluye, mueve y saca personas, y mantiene las
    reglas: una sola área por persona, el gerente fuera de las áreas y el responsable dentro de un área que dirige;
  - **el área manda**: el supervisor del empleado es el de su área, así que la ficha ya no tiene un campo «Supervisor» suelto;
  - permisos polimórficos con `puedeIncluirEnArea`: el gerente incluye a cualquiera; el supervisor, solo a personas sin
    área y en un área que dirige;
  - `ServicioAreas`, `ControladorAreas` (`/api/areas`), pestaña «Áreas» en la web y en Swing, y alerta alta para el gerente
    si un área queda sin supervisor;
  - persistencia con la línea `AR`; las áreas antiguas del catálogo, que no tenían supervisor, se descartan al cargar y el
    catálogo queda solo con los puestos;
  - el código de registro ya no trae área: la persona entra sin área y luego la incluyen.
- **Proyecto** (HU-48): menú «Proyectos», «Reportes de proyecto», «Inicio del proyecto», «Conclusión del proyecto» y estados
  Planificado, En curso y Concluido. Los proyectos nuevos se numeran PR-01, PR-02…; los existentes conservan su código OB-
  para no romper los registros guardados. En el código, la clase sigue llamándose `Obra`.
- **Nombres técnicos** (HU-49): «Tablero» pasa a «Panel de control» e «Invitaciones» a «Registro de personal»; el código
  CT-XXXX-XXXX se llama «código de registro» en pantallas y correos. Las direcciones antiguas (`#obras`, `#tablero`,
  `#invitaciones`) siguen funcionando y llevan a la pantalla nueva.
- Portada: «Cada marca, respaldada con evidencia» en lugar de «Marcas con evidencia, no con buena fe».

## Incremento
- 112 pruebas JUnit (7 nuevas en `AreasTest`): crear áreas, el área define el supervisor, permisos del supervisor,
  eliminar, responsable dentro de su área, supervisor que egresa y persistencia.
- CI con pruebas de humo de `/api/areas`, de los proyectos PR- y del acceso denegado al empleado.

## Review
Recorrido en el navegador con los datos de ejemplo (Plomería y Electricidad de Ana; Mantenimiento y Transporte de Luis):
1. El gerente crea «Soldadura» con Luis como supervisor y mueve a Sofía: su supervisor pasa a ser Luis.
2. Saca a Miguel de Transporte, elimina el área vacía y Miguel aparece en «Personas sin área».
3. Ana solo ve sus dos áreas, no puede crear áreas y suma a Miguel a Plomería: su supervisora pasa a ser Ana.
4. El responsable de cada área aparece como «Responsable del área», sin botones para moverlo o sacarlo.
5. Los menús, títulos, estados, correos y mensajes dicen «proyecto», «Panel de control» y «Registro de personal».
También se revisaron las pestañas «Áreas», «Proyectos», «Panel de control» y «Registro de personal» en Swing.

## Retrospectiva
- **Bien:** que el área defina al supervisor eliminó una fuente de datos contradictorios (empleado de un área con el
  supervisor de otra).
- **Mejorar:** el servidor de prueba servía una copia vieja de la web; ahora se recompila antes de cada revisión.
- **Para el futuro:** varios supervisores por área si una cuadrilla crece, y filtrar el panel de control por área.
