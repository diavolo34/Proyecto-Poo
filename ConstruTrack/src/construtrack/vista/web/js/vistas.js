// Vistas del sistema. Cada una tiene un título, para quién está disponible y cómo se dibuja.

const HORAS = Array.from({ length: 34 }, (_, i) => `${String(5 + Math.floor(i / 2)).padStart(2, '0')}:${i % 2 ? '30' : '00'}`);
const esRevisor = () => estado.usuario && estado.usuario.rol !== 'Empleado';
const esJefe = () => estado.usuario && estado.usuario.rol === 'Jefe';
const contenido = () => $('#principal');

function encabezado(migas, titulo, acciones = '') {
  return `<div class="migas">${esc(migas)}</div>
    <div class="titulo-vista"><h1>${esc(titulo)}</h1><div class="acciones" style="margin:0">${acciones}</div></div>`;
}

const VISTAS = [
  { id: 'asistencia', titulo: 'Mi asistencia', grupo: 'Mi trabajo', icono: '⏱', disponible: () => true, dibujar: vistaAsistencia },
  { id: 'incidencias', titulo: 'Incidencias', grupo: 'Mi trabajo', icono: '✚', disponible: () => true, dibujar: vistaIncidencias },
  { id: 'tablero', titulo: 'Tablero', grupo: 'Equipo', icono: '▦', disponible: esRevisor, dibujar: vistaTablero },
  { id: 'supervision', titulo: 'Supervisión', grupo: 'Equipo', icono: '▤', disponible: esRevisor, dibujar: vistaSupervision },
  { id: 'obras', titulo: 'Obras', grupo: 'Equipo', icono: '⌂', disponible: esRevisor, dibujar: vistaObras },
  { id: 'personal', titulo: 'Personal', grupo: 'Equipo', icono: '☰', disponible: esRevisor, dibujar: vistaPersonal },
  { id: 'invitaciones', titulo: 'Invitaciones', grupo: 'Administración', icono: '✉', disponible: esJefe, dibujar: vistaInvitaciones },
];

// ------------------------------------------------------------------ Mi asistencia
async function vistaAsistencia() {
  const u = estado.usuario = await Api.yo();
  const lunes = new Date();
  lunes.setDate(lunes.getDate() - ((lunes.getDay() + 6) % 7));
  const desde = lunes.toISOString().slice(0, 10);
  const semana = u.asistencias.filter((a) => a.fecha >= desde);
  const tardes = semana.filter((a) => a.tarde).length;
  const puntualidad = semana.length ? Math.round(100 * (semana.length - tardes) / semana.length) : 100;
  const hoy = u.asistenciaHoy;
  const horas = (a) => (a.horaSalida ? formatoHoras(a.minutos) : '—');
  const extra = (a) => (a.minutosExtra
    ? chip('+' + formatoHoras(a.minutosExtra), a.estadoExtra === 'Aprobadas' ? 'verde' : a.estadoExtra === 'Rechazadas' ? 'gris' : 'amarillo')
      + `<br><span class="pequeno suave">${esc(a.estadoExtra)}</span>`
    : '—');
  contenido().innerHTML = encabezado('Mi trabajo /', 'Mi asistencia') + `
    <div class="rejilla-2">
      <section class="tarjeta" aria-labelledby="t-hoy">
        <h2 id="t-hoy">Hola, ${esc(u.nombre.split(' ')[0])}</h2>
        <div class="estado-jornada">${u.presente ? chip('● En la obra', 'verde') : chip('○ Fuera de la obra', 'gris')}
          ${hoy ? `<span class="suave pequeno">Entraste hoy a las ${esc(hoy.horaEntrada)}${hoy.tarde ? ' (tarde)' : ''}</span>` : ''}</div>
        <p class="suave pequeno">Horario: ${esc(textoDias(u.horario.dias))}, de ${esc(u.horario.horaEntrada)} a ${esc(u.horario.horaSalida)}
          ${u.obra ? ' · Obra: ' + esc(u.obra) : ''}${u.supervisor ? ' · Supervisor: ' + esc(u.supervisor.nombre) : ''}</p>
        <div class="acciones" style="justify-content:flex-start">
          <button class="btn btn-primario" id="marcar-entrada" ${u.presente ? 'disabled' : ''}>Marcar entrada</button>
          <button class="btn btn-secundario" id="marcar-salida" ${u.presente ? '' : 'disabled'}>Marcar salida</button>
        </div>
      </section>
      <section class="tarjeta" aria-labelledby="t-semana">
        <h2 id="t-semana">Esta semana</h2>
        <div class="cifras cifras-3">
          <div class="cifra"><div class="valor">${semana.length}</div><div class="descripcion">Jornadas</div></div>
          <div class="cifra"><div class="valor">${tardes}</div><div class="descripcion">Llegadas tarde</div></div>
          <div class="cifra"><div class="valor">${puntualidad}%</div><div class="descripcion">Puntualidad</div></div>
        </div>
      </section>
    </div>
    <section class="tarjeta" style="margin-top:16px"><h2>Mi historial reciente</h2>
      <div class="tabla-contenedor"><table>
        <thead><tr><th>Fecha</th><th>Entrada</th><th>Salida</th><th>Llegada</th><th>Horas</th><th>Extra</th></tr></thead>
        <tbody>${u.asistencias.map((a) => `<tr><td>${fecha(a.fecha)}</td><td>${esc(a.horaEntrada)}</td>
          <td>${a.horaSalida ? esc(a.horaSalida) : chip('En curso', 'azul')}</td>
          <td>${a.tarde ? chip('Tarde', 'amarillo') : chip('Puntual', 'verde')}</td><td>${horas(a)}</td><td>${extra(a)}</td></tr>`).join('')
          || '<tr><td colspan="6" class="vacio">Aún no tienes jornadas registradas. Marca tu primera entrada.</td></tr>'}</tbody>
      </table></div></section>`;
  const marcar = async (accion) => {
    try {
      const respuesta = await accion();
      aviso(respuesta.mensaje);
      await vistaAsistencia();
    } catch (e) {
      aviso(e.message, 'error');
    }
  };
  $('#marcar-entrada').addEventListener('click', () => marcar(Api.marcarEntrada));
  $('#marcar-salida').addEventListener('click', () => marcar(Api.marcarSalida));
}

// ------------------------------------------------------------------ Incidencias
const EXTRAS = {
  REPOSO_MEDICO: `<div class="campo"><label for="centroMedico">Centro médico *</label><input id="centroMedico" name="centroMedico"></div>
    <div class="campo"><label for="certificado">N.º de certificado</label><input id="certificado" name="certificado"></div>`,
  RETARDO: `<div class="campo"><label for="minutos">Minutos de retraso *</label><input id="minutos" name="minutos" type="number" min="1" max="600" value="15"></div>`,
  ACCIDENTE_LABORAL: `<div class="campo"><label for="lugar">Lugar de la obra *</label><input id="lugar" name="lugar" placeholder="Ej.: Andamio 3, piso 4"></div>
    <label class="casillas"><label><input type="checkbox" name="requirioAtencion" value="true"> Requirió atención médica</label></label>`,
};

async function vistaIncidencias() {
  const [opciones, lista] = await Promise.all([Api.opcionesIncidencias(), Api.incidencias()]);
  const hoy = new Date().toISOString().slice(0, 10);
  contenido().innerHTML = encabezado('Mi trabajo /', 'Incidencias') + `
    <div class="rejilla-incidencias">
      <form class="tarjeta" id="form-incidencia" novalidate aria-labelledby="t-reportar">
        <h2 id="t-reportar">Reportar una incidencia</h2>
        <div class="campo"><label for="codigoTipo">Tipo *</label><select id="codigoTipo" name="codigoTipo">
          ${Object.entries(opciones.tipos).map(([codigo, nombre]) => `<option value="${esc(codigo)}">${esc(nombre)}</option>`).join('')}</select></div>
        <div class="campo ${opciones.personas.length > 1 ? '' : 'oculto'}"><label for="cedula">Persona afectada *</label><select id="cedula" name="cedula">
          ${opciones.personas.map((p) => `<option value="${esc(p.cedula)}">${esc(p.nombre)}</option>`).join('')}</select></div>
        <div class="fila">
          <div class="campo"><label for="desde">Desde *</label><input id="desde" name="desde" type="date" value="${hoy}"></div>
          <div class="campo"><label for="hasta">Hasta</label><input id="hasta" name="hasta" type="date"><span class="ayuda">Vacío = un día</span></div>
        </div>
        <div class="campo"><label for="descripcion">Descripción *</label>
          <textarea id="descripcion" name="descripcion" maxlength="300" placeholder="Cuenta brevemente qué pasó"></textarea></div>
        <div id="extras"></div>
        <button class="btn btn-primario btn-bloque" type="submit">Reportar incidencia</button>
      </form>
      <section class="tarjeta" aria-labelledby="t-lista"><h2 id="t-lista">${esRevisor() ? 'Incidencias de tu equipo' : 'Mis incidencias'}</h2>
        <div class="tabla-contenedor"><table>
          <thead><tr><th>#</th><th>Tipo</th><th>Persona</th><th>Fechas</th><th>Gravedad</th><th>Estado</th><th>Detalle</th></tr></thead>
          <tbody>${lista.map((i) => `<tr><td>${i.id}</td><td>${esc(i.nombreTipo)}</td><td>${esc(i.nombreAfectado)}</td>
            <td>${fechaCorta(i.desde)}${i.hasta !== i.desde ? ' – ' + fechaCorta(i.hasta) : ''}</td>
            <td>${chipGravedad(i.gravedad)}</td><td>${chipEstado(i.estadoNombre)}</td>
            <td class="pequeno">${esc(i.detalle || '')}${i.comentarioRevision ? `<br><span class="suave">“${esc(i.comentarioRevision)}”</span>` : ''}</td></tr>`).join('')
            || '<tr><td colspan="7" class="vacio">No hay incidencias.</td></tr>'}</tbody>
        </table></div></section>
    </div>`;
  const tipo = $('#codigoTipo');
  const mostrarExtras = () => { $('#extras').innerHTML = EXTRAS[tipo.value] || ''; };
  tipo.addEventListener('change', mostrarExtras);
  mostrarExtras();
  $('#form-incidencia').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    try {
      const nueva = await Api.reportarIncidencia(datosFormulario(evento.target));
      aviso(`Incidencia #${nueva.id} reportada. Quedó pendiente de revisión.`);
      await vistaIncidencias();
    } catch (e) {
      aviso(e.message, 'error');
    }
  });
}

// ------------------------------------------------------------------ Tablero
async function vistaTablero() {
  const lista = await Api.incidencias();
  const columnas = ['Pendiente', 'En revisión', 'Aprobada', 'Rechazada'];
  const filtro = estado.filtroTablero || '';
  const visibles = lista.filter((i) => !filtro || (i.nombreAfectado + ' ' + i.nombreTipo).toLowerCase().includes(filtro.toLowerCase()));
  const orden = { Alta: 0, Media: 1, Baja: 2 };
  const tarjeta = (i) => `<article class="tarjeta-kanban gravedad-${i.gravedad.toLowerCase()}" aria-label="Incidencia ${i.id}">
      <h4>#${i.id} · ${esc(i.nombreTipo)}</h4><p>${esc(i.nombreAfectado)}</p>
      <p class="suave pequeno">${fecha(i.desde)}${i.hasta !== i.desde ? ' → ' + fecha(i.hasta) : ''}</p>
      <p class="pequeno">${esc(i.descripcion)}</p>${chipGravedad(i.gravedad)}
      ${i.puedoRevisar ? `<div class="acciones">
        ${i.estado === 'PENDIENTE' ? `<button class="btn btn-secundario btn-chico" data-revisar="${i.id}" data-estado="EN_REVISION">Revisar</button>` : ''}
        <button class="btn btn-exito btn-chico" data-revisar="${i.id}" data-estado="APROBADA">Aprobar</button>
        <button class="btn btn-peligro btn-chico" data-revisar="${i.id}" data-estado="RECHAZADA">Rechazar</button></div>` : ''}
    </article>`;
  contenido().innerHTML = encabezado('Equipo /', 'Tablero de incidencias',
    `<label class="solo-lector" for="filtro-tablero">Buscar en el tablero</label>
     <input id="filtro-tablero" placeholder="Buscar persona o tipo…" value="${esc(filtro)}" style="width:240px">`) + `
    <div class="kanban">${columnas.map((col) => {
      const tarjetas = visibles.filter((i) => i.estadoNombre === col).sort((a, b) => orden[a.gravedad] - orden[b.gravedad]);
      return `<section class="kanban-columna" aria-label="${col}"><h3>${col} <span>${tarjetas.length}</span></h3>
        ${tarjetas.map(tarjeta).join('') || '<p class="suave pequeno" style="margin:4px">Sin incidencias</p>'}</section>`;
    }).join('')}</div>`;
  const buscador = $('#filtro-tablero');
  buscador.addEventListener('input', () => {
    estado.filtroTablero = buscador.value;
    vistaTablero().then(() => { const b = $('#filtro-tablero'); b.focus(); b.setSelectionRange(b.value.length, b.value.length); });
  });
  contenido().querySelectorAll('[data-revisar]').forEach((boton) => boton.addEventListener('click', () =>
    revisarIncidencia(Number(boton.dataset.revisar), boton.dataset.estado)));
}

async function revisarIncidencia(id, nuevoEstado) {
  const enviar = async (comentario) => {
    try {
      await Api.revisarIncidencia(id, nuevoEstado, comentario);
      aviso(`Incidencia #${id}: ${{ EN_REVISION: 'en revisión', APROBADA: 'aprobada', RECHAZADA: 'rechazada' }[nuevoEstado]}.`);
      Modal.cerrar();
      await vistaTablero();
    } catch (e) {
      aviso(e.message, 'error');
    }
  };
  if (nuevoEstado !== 'RECHAZADA') {
    await enviar('');
    return;
  }
  // Rechazar pide el motivo: el trabajador sabrá por qué (ayuda a reconocer y corregir errores)
  const modal = Modal.abrir(`Rechazar la incidencia #${id}`, `<form id="form-rechazo">
    <div class="campo"><label for="motivo">Motivo del rechazo *</label>
      <textarea id="motivo" name="comentario" required placeholder="Ej.: falta el certificado médico"></textarea>
      <span class="ayuda">El trabajador verá este motivo.</span></div>
    <div class="acciones"><button type="button" class="btn btn-secundario" data-cerrar>Cancelar</button>
      <button class="btn btn-peligro" type="submit">Rechazar</button></div></form>`);
  modal.querySelector('form').addEventListener('submit', (e) => { e.preventDefault(); enviar($('#motivo').value); });
}

// ------------------------------------------------------------------ Supervisión
async function vistaSupervision() {
  const periodo = estado.periodo || 'ultimos7';
  const [est, alertas, extras] = await Promise.all([Api.estadisticas(periodo), Api.alertas(periodo), Api.horasExtra()]);
  const color = (v) => (v >= 90 ? '#22a06b' : v >= 75 ? '#e2b203' : '#c9372c');
  contenido().innerHTML = encabezado('Equipo /', 'Supervisión', `
    <label for="periodo" class="pequeno">Período</label>
    <select id="periodo" style="width:auto">${[['ultimos7', 'Últimos 7 días'], ['semana', 'Esta semana'], ['mes', 'Este mes']]
      .map(([v, t]) => `<option value="${v}" ${v === periodo ? 'selected' : ''}>${t}</option>`).join('')}</select>`) + `
    <p class="suave pequeno" style="margin-top:-8px">${esc(est.periodo)}</p>
    <div class="cifras">
      <div class="cifra"><div class="descripcion">Asistencia</div><div class="valor">${Math.round(est.tasaAsistencia)}%</div></div>
      <div class="cifra"><div class="descripcion">Puntualidad</div><div class="valor">${Math.round(est.porcentajePuntualidad)}%</div></div>
      <div class="cifra"><div class="descripcion">Horas trabajadas</div><div class="valor">${Math.round(est.minutosTrabajados / 60)} h</div></div>
      <div class="cifra"><div class="descripcion">Alertas activas</div><div class="valor">${alertas.length}</div></div>
    </div>
    <div class="rejilla-2" style="margin-top:16px">
      <section class="tarjeta" aria-labelledby="t-barras"><h2 id="t-barras">Asistencia por persona</h2>
        <div class="barras">${est.personas.map((p) => `<div class="barra-fila">
          <span title="${esc(p.nombre)}">${esc(p.nombre.split(' ')[0])} ${esc((p.nombre.split(' ')[1] || '')[0] || '')}.</span>
          <div class="barra-pista" role="img" aria-label="${esc(p.nombre)}: ${Math.round(p.tasaAsistencia)}% de asistencia">
            <span style="width:${p.tasaAsistencia}%;background:${color(p.tasaAsistencia)}"></span></div>
          <strong>${Math.round(p.tasaAsistencia)}%</strong></div>`).join('') || '<p class="suave">Sin datos.</p>'}</div></section>
      <section class="tarjeta" aria-labelledby="t-alertas"><h2 id="t-alertas">Alertas automáticas</h2><p class="suave pequeno" style="margin-top:-6px">De la más antigua a la más reciente.</p>
        ${alertas.map((a) => `<div class="alerta">${chip(a.nivel, a.nivel === 'Alta' ? 'rojo' : 'amarillo')}
          <div><strong>${esc(a.nombre)}</strong> <span class="suave pequeno">· desde ${fecha(a.desde)}</span><br><span class="suave pequeno">${esc(a.mensaje)}</span></div></div>`).join('')
          || '<p class="suave">Sin alertas en este período. Todo en orden.</p>'}</section>
    </div>
    <section class="tarjeta" style="margin-top:16px" aria-labelledby="t-ranking"><h2 id="t-ranking">Tabla de puntualidad</h2>
      <p class="suave pequeno" style="margin-top:-6px">Ordenada de quien más requiere atención a quien menos. Solo cuenta los días desde que cada persona entró a la empresa.
        Las horas extra cuentan cuando el supervisor o el jefe las aprueba.</p>
      <div class="tabla-contenedor"><table><thead><tr><th>Persona</th><th>Horas trabajadas / esperadas</th>
        <th>Extra diaria</th><th>Extra día libre</th><th>Extra por aprobar</th><th>Retardos</th><th>Faltas sin justificar</th>
        <th>Justificadas</th><th>Puntualidad</th></tr></thead><tbody>
        ${est.personas.map((p) => `<tr><td style="white-space:nowrap">${esc(p.nombre)}</td>
          <td style="white-space:nowrap"><strong>${formatoHoras(p.minutos)}</strong> / ${formatoHoras(p.minutosEsperados)}</td>
          <td>${p.extraDiaria ? chip('+' + formatoHoras(p.extraDiaria), 'verde') : '—'}</td>
          <td>${p.extraDiaLibre ? chip('+' + formatoHoras(p.extraDiaLibre), 'verde') : '—'}</td>
          <td>${p.extraPendiente ? chip(formatoHoras(p.extraPendiente), 'amarillo') : '—'}</td>
          <td>${p.retardos ? chip(p.retardos, 'amarillo') : 0}</td><td>${p.ausencias ? chip(p.ausencias, 'rojo') : 0}</td>
          <td>${p.justificadas}</td><td>${Math.round(p.puntualidad)}%</td></tr>`).join('')}</tbody></table></div></section>
    <section class="tarjeta" style="margin-top:16px" aria-labelledby="t-extra"><h2 id="t-extra">Horas extra por aprobar</h2>
      <p class="suave pequeno" style="margin-top:-6px">Se calculan solas con la entrada y la salida: lo que pasa de la jornada, o todo lo trabajado en un día libre (desde 15 min).</p>
      ${extras.length ? `<div class="tabla-contenedor"><table><thead><tr><th>Persona</th><th>Fecha</th><th>Entrada – salida</th>
        <th>Tipo</th><th>Extra</th><th><span class="solo-lector">Acciones</span></th></tr></thead><tbody>
        ${extras.map((x) => `<tr><td>${esc(x.nombre)}</td><td>${fecha(x.jornada.fecha)}</td>
          <td>${esc(x.jornada.horaEntrada)} – ${esc(x.jornada.horaSalida || '…')}</td><td>${esc(x.jornada.tipoExtra)}</td>
          <td><strong>${formatoHoras(x.jornada.minutosExtra)}</strong></td>
          <td style="white-space:nowrap"><button class="btn btn-exito btn-chico" data-extra="APROBADAS" data-cedula="${esc(x.cedula)}" data-fecha="${esc(x.jornada.fecha)}">Aprobar</button>
            <button class="btn btn-peligro btn-chico" data-extra="RECHAZADAS" data-cedula="${esc(x.cedula)}" data-fecha="${esc(x.jornada.fecha)}">Rechazar</button></td></tr>`).join('')}
        </tbody></table></div>` : '<p class="suave">No hay horas extra por aprobar.</p>'}</section>
    <section class="tarjeta" style="margin-top:16px" aria-labelledby="t-reportes"><h2 id="t-reportes">Reportes</h2>
      <div class="acciones" style="justify-content:flex-start;align-items:flex-end">
        <div class="campo" style="margin:0"><label for="tipo-reporte">Tipo</label><select id="tipo-reporte">
          <option value="asistencia">Asistencia</option><option value="puntualidad">Puntualidad</option><option value="incidencias">Incidencias</option></select></div>
        <div class="campo" style="margin:0"><label for="formato-reporte">Formato</label><select id="formato-reporte">
          <option value="csv">Excel (CSV)</option><option value="txt">Texto</option></select></div>
        <button class="btn btn-primario" id="descargar-reporte">Descargar reporte</button>
      </div></section>`;
  $('#periodo').addEventListener('change', (e) => { estado.periodo = e.target.value; vistaSupervision(); });
  contenido().querySelectorAll('[data-extra]').forEach((b) => b.addEventListener('click', async () => {
    try {
      await Api.revisarHorasExtra(b.dataset.cedula, b.dataset.fecha, b.dataset.extra);
      aviso(b.dataset.extra === 'APROBADAS' ? 'Horas extra aprobadas.' : 'Horas extra rechazadas.');
      vistaSupervision();
    } catch (e) {
      aviso(e.message, 'error');
    }
  }));
  $('#descargar-reporte').addEventListener('click', async () => {
    try {
      const nombre = await Api.descargarReporte($('#tipo-reporte').value, $('#formato-reporte').value, periodo);
      aviso('Reporte descargado: ' + nombre);
    } catch (e) {
      aviso(e.message, 'error');
    }
  });
}

// ------------------------------------------------------------------ Personal
async function vistaPersonal() {
  const lista = await Api.listarUsuarios();
  const filtro = (estado.filtroPersonal || '').toLowerCase();
  const visibles = lista.filter((u) => !filtro || [u.nombre, u.cedula, u.rol, u.obra, u.correo].join(' ').toLowerCase().includes(filtro));
  contenido().innerHTML = encabezado('Equipo /', esJefe() ? 'Personal' : 'Mi equipo', `
    <label class="solo-lector" for="buscar-personal">Buscar</label>
    <input id="buscar-personal" placeholder="Buscar nombre, cédula, obra…" value="${esc(estado.filtroPersonal || '')}" style="width:240px">
    ${esJefe() ? '<button class="btn btn-primario" id="registrar-persona">+ Registrar persona</button>' : ''}`) + `
    <div class="tabla-contenedor"><table>
      <thead><tr><th>Persona</th><th>Cédula</th><th>Rol</th><th>Estado</th><th>Horario</th><th>Supervisor</th><th>Obra</th><th><span class="solo-lector">Acciones</span></th></tr></thead>
      <tbody>${visibles.map((u) => `<tr>
        <td><strong>${esc(u.nombre)}</strong>${u.correo ? `<br><span class="suave pequeno">${esc(u.correo)}</span>` : ''}</td>
        <td>${esc(u.cedula)}</td><td>${chipRol(u.rol)}</td>
        <td>${u.presente ? chip('En la obra', 'verde') : chip('Fuera', 'gris')}</td>
        <td class="pequeno">${esc(u.horario.dias)}<br>${esc(u.horario.horaEntrada)}–${esc(u.horario.horaSalida)}</td>
        <td>${u.supervisor ? esc(u.supervisor.nombre) : '—'}</td><td>${esc(u.obra || '—')}</td>
        <td style="white-space:nowrap"><button class="btn btn-enlace btn-chico" data-historial="${esc(u.cedula)}">Historial</button>
          ${esJefe() && u.rol !== 'Jefe' ? `<button class="btn btn-enlace btn-chico" data-editar="${esc(u.cedula)}">Editar</button>
          <button class="btn btn-enlace btn-chico" style="color:var(--rojo)" data-eliminar="${esc(u.cedula)}" data-nombre="${esc(u.nombre)}">Eliminar</button>` : ''}</td>
      </tr>`).join('') || '<tr><td colspan="8" class="vacio">No hay coincidencias.</td></tr>'}</tbody></table></div>`;
  const buscador = $('#buscar-personal');
  buscador.addEventListener('input', () => {
    estado.filtroPersonal = buscador.value;
    vistaPersonal().then(() => { const b = $('#buscar-personal'); b.focus(); b.setSelectionRange(b.value.length, b.value.length); });
  });
  if (esJefe()) $('#registrar-persona').addEventListener('click', () => formularioPersona(null));
  contenido().querySelectorAll('[data-editar]').forEach((b) => b.addEventListener('click', () => formularioPersona(b.dataset.editar)));
  contenido().querySelectorAll('[data-historial]').forEach((b) => b.addEventListener('click', () => historialPersona(b.dataset.historial)));
  contenido().querySelectorAll('[data-eliminar]').forEach((b) => b.addEventListener('click', () => {
    const modal = Modal.abrir('Eliminar persona', `<p>¿Seguro que quieres eliminar a <strong>${esc(b.dataset.nombre)}</strong>?
      Esta acción no se puede deshacer. Su cuenta seguirá existiendo, pero sin empresa.</p>
      <div class="acciones"><button class="btn btn-secundario" data-cerrar>Cancelar</button>
      <button class="btn btn-peligro" id="confirmar-eliminar">Eliminar</button></div>`);
    modal.querySelector('#confirmar-eliminar').addEventListener('click', async () => {
      try {
        await Api.eliminarUsuario(b.dataset.eliminar);
        Modal.cerrar();
        aviso(b.dataset.nombre + ' fue eliminado.');
        await vistaPersonal();
      } catch (e) {
        aviso(e.message, 'error');
      }
    });
  }));
}

async function formularioPersona(cedula) {
  const [opciones, u] = await Promise.all([Api.opcionesPersonal(), cedula ? Api.verUsuario(cedula) : Promise.resolve(null)]);
  const dias = (u ? u.horario.dias : 'LU,MA,MI,JU,VI').split(',');
  const horas = (nombre, valor) => `<select id="${nombre}" name="${nombre}">${HORAS.map((h) => `<option ${h === valor ? 'selected' : ''}>${h}</option>`).join('')}</select>`;
  const modal = Modal.abrir(cedula ? 'Editar persona' : 'Registrar persona', `<form id="form-persona" novalidate>
    <div class="fila">
      <div class="campo"><label for="rol">Rol</label><select id="rol" name="rol">
        ${['Empleado', 'Supervisor'].map((r) => `<option ${u && u.rol === r ? 'selected' : ''}>${r}</option>`).join('')}</select></div>
      <div class="campo"><label for="p-cedula">Cédula o DNI</label><input id="p-cedula" name="cedula" value="${esc(u ? u.cedula : '')}" ${u ? 'disabled' : 'inputmode="numeric" data-solo-numeros'}></div>
    </div>
    <div class="campo"><label for="p-nombre">Nombre completo</label><input id="p-nombre" name="nombre" value="${esc(u ? u.nombre : '')}"></div>
    <div class="campo"><label for="p-clave">Clave</label><input id="p-clave" name="clave" type="password" autocomplete="new-password">
      <span class="ayuda">${u ? 'Déjala vacía para no cambiarla.' : 'Mínimo 4 caracteres.'}</span></div>
    <div class="campo"><span class="etiqueta">Días de trabajo</span><div class="casillas">
      ${Object.keys(DIAS).map((d) => `<label><input type="checkbox" name="dias" value="${d}" ${dias.includes(d) ? 'checked' : ''}>${d}</label>`).join('')}</div></div>
    <div class="fila">
      <div class="campo"><label for="horaEntrada">Entrada</label>${horas('horaEntrada', u ? u.horario.horaEntrada : '07:00')}</div>
      <div class="campo"><label for="horaSalida">Salida</label>${horas('horaSalida', u ? u.horario.horaSalida : '16:00')}</div>
    </div>
    <div class="campo" id="campo-supervisor"><label for="supervisor">Supervisor</label><select id="supervisor" name="supervisor">
      <option value="">— Sin supervisor —</option>${opciones.supervisores.map((s) =>
        `<option value="${esc(s.cedula)}" ${u && u.supervisor && u.supervisor.cedula === s.cedula ? 'selected' : ''}>${esc(s.nombre)}</option>`).join('')}</select></div>
    <div class="campo"><label for="p-obra">Obra</label><select id="p-obra" name="obra">
      <option value="">— Sin obra —</option>${opciones.obras.map((o) =>
        `<option value="${esc(o.codigo)}" ${u && u.obra === o.nombre ? 'selected' : ''}>${esc(o.nombre)}</option>`).join('')}</select></div>
    <div class="mensaje mensaje-error oculto" role="alert"></div>
    <div class="acciones"><button type="button" class="btn btn-secundario" data-cerrar>Cancelar</button>
      <button class="btn btn-primario" type="submit">Guardar</button></div></form>`);
  const formulario = modal.querySelector('form');
  const rol = $('#rol');
  const alternarSupervisor = () => $('#campo-supervisor').classList.toggle('oculto', rol.value !== 'Empleado');
  rol.addEventListener('change', alternarSupervisor);
  alternarSupervisor();
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const datos = datosFormulario(formulario);
    datos.dias = datos.dias || '';
    if (!datos.clave) delete datos.clave;
    try {
      const obra = datos.obra || '';
      delete datos.obra;
      const guardado = cedula ? await Api.modificarUsuario(cedula, datos) : await Api.registrarUsuario(datos);
      const obraAnterior = (opciones.obras.find((o) => u && o.nombre === u.obra) || {}).codigo || '';
      if (obra !== obraAnterior) await Api.asignarObra(guardado.cedula, obra);
      Modal.cerrar();
      aviso((cedula ? 'Se actualizó a ' : 'Se registró a ') + guardado.nombre + '.');
      await vistaPersonal();
    } catch (e) {
      mostrarErrorFormulario(formulario, e.message);
    }
  });
}

async function historialPersona(cedula) {
  try {
    const u = await Api.verUsuario(cedula);
    const incidencias = u.incidencias || [];
    const modal = Modal.abrir('Historial de ' + u.nombre, `<p class="suave">${esc(textoDias(u.horario.dias))}, de ${esc(u.horario.horaEntrada)} a ${esc(u.horario.horaSalida)}
      ${u.fechaIngreso ? ' · En la empresa desde el ' + fecha(u.fechaIngreso) : ''}</p>
      <div class="pestanas" role="tablist">
        <button type="button" role="tab" class="pestana activa" aria-selected="true" data-pestana="h-asistencia">Asistencia (${u.asistencias.length})</button>
        <button type="button" role="tab" class="pestana" aria-selected="false" data-pestana="h-incidencias">Incidencias (${incidencias.length})</button>
      </div>
      <div id="h-asistencia" role="tabpanel"><div class="tabla-contenedor"><table><thead><tr><th>Fecha</th><th>Entrada</th><th>Salida</th><th>Llegada</th><th>Horas</th><th>Extra</th></tr></thead><tbody>
      ${u.asistencias.map((a) => `<tr><td>${fecha(a.fecha)}</td><td>${esc(a.horaEntrada)}</td><td>${esc(a.horaSalida || 'En curso')}</td>
        <td>${a.tarde ? chip('Tarde', 'amarillo') : chip('Puntual', 'verde')}</td><td>${a.horaSalida ? formatoHoras(a.minutos) : '—'}</td>
        <td>${a.minutosExtra ? '+' + formatoHoras(a.minutosExtra) + ' · ' + esc(a.estadoExtra) : '—'}</td></tr>`).join('')
        || '<tr><td colspan="6" class="vacio">Sin jornadas registradas.</td></tr>'}</tbody></table></div></div>
      <div id="h-incidencias" role="tabpanel" class="oculto"><div class="tabla-contenedor"><table><thead><tr><th>#</th><th>Tipo</th><th>Fechas</th><th>Estado</th><th>Detalle</th></tr></thead><tbody>
      ${incidencias.map((i) => `<tr><td>${i.id}</td><td>${esc(i.nombreTipo)}<br>${chipGravedad(i.gravedad)}</td>
        <td style="white-space:nowrap">${fecha(i.desde)}${i.hasta && i.hasta !== i.desde ? ' – ' + fecha(i.hasta) : ''}</td>
        <td>${chipEstado(i.estadoNombre)}${i.revisor ? `<br><span class="pequeno suave">por ${esc(i.revisor)}</span>` : ''}</td>
        <td>${esc(i.descripcion)}${i.detalle ? `<br><span class="pequeno suave">${esc(i.detalle)}</span>` : ''}
          ${i.comentarioRevision ? `<br><span class="pequeno">«${esc(i.comentarioRevision)}»</span>` : ''}</td></tr>`).join('')
        || '<tr><td colspan="5" class="vacio">Sin incidencias: no ha reportado faltas, reposos ni permisos.</td></tr>'}</tbody></table></div></div>`);
    modal.querySelectorAll('[data-pestana]').forEach((b) => b.addEventListener('click', () => {
      modal.querySelectorAll('[data-pestana]').forEach((x) => {
        const activa = x === b;
        x.classList.toggle('activa', activa);
        x.setAttribute('aria-selected', String(activa));
        modal.querySelector('#' + x.dataset.pestana).classList.toggle('oculto', !activa);
      });
    }));
  } catch (e) {
    aviso(e.message, 'error');
  }
}

// ------------------------------------------------------------------ Invitaciones (Jefe)
// ------------------------------------------------------------------ Obras (Supervisor y Jefe)
async function vistaObras() {
  const [obras, visibles] = await Promise.all([Api.obras(), Api.listarUsuarios()]);
  // A quién puede mover de obra: el Jefe a todos; el Supervisor a su equipo y a sí mismo
  const asignables = esJefe() ? visibles : [estado.usuario, ...visibles];
  contenido().innerHTML = encabezado('Equipo /', 'Obras', '<button class="btn btn-primario" id="nueva-obra">+ Nueva obra</button>') + `
    <p class="suave" style="margin-top:-8px">Los lugares donde trabaja el personal. Las gestionan el supervisor y el jefe.</p>
    <div class="rejilla-obras">${obras.map((o) => `<section class="tarjeta" aria-labelledby="obra-${esc(o.codigo)}">
      <div class="titulo-vista" style="margin:0 0 4px"><h2 id="obra-${esc(o.codigo)}" style="margin:0">${esc(o.nombre)}</h2>${chip(o.codigo, 'gris')}</div>
      <p class="suave pequeno" style="margin:0 0 10px">${esc(o.ubicacion || 'Sin ubicación')} · ${fecha(o.inicio)} → ${fecha(o.fin)}</p>
      <div class="barra-pista" role="img" aria-label="${o.avance}% del plazo transcurrido"><span style="width:${o.avance}%;background:#0c66e4"></span></div>
      <p class="pequeno suave" style="margin:4px 0 12px">${o.avance}% del plazo</p>
      <h3 class="pequeno" style="margin:0 0 6px">Personas (${o.personas.length})</h3>
      <ul class="lista-personas">${o.personas.map((p) => `<li>${esc(p.nombre)} ${chipRol(p.rol)}
        ${asignables.some((a) => a.cedula === p.cedula) ? `<button class="btn btn-enlace btn-chico" data-quitar="${esc(p.cedula)}" aria-label="Quitar a ${esc(p.nombre)} de la obra">Quitar</button>` : ''}</li>`).join('')
        || '<li class="suave">Nadie asignado todavía.</li>'}</ul>
      <div class="acciones" style="justify-content:flex-start;flex-wrap:wrap">
        <label class="solo-lector" for="sumar-${esc(o.codigo)}">Sumar persona a ${esc(o.nombre)}</label>
        <select id="sumar-${esc(o.codigo)}" data-sumar="${esc(o.codigo)}" style="width:auto;max-width:100%">
          <option value="">+ Sumar persona…</option>${asignables.filter((a) => !o.personas.some((p) => p.cedula === a.cedula))
            .map((a) => `<option value="${esc(a.cedula)}">${esc(a.nombre)}${a.obra ? ' (en ' + esc(a.obra) + ')' : ''}</option>`).join('')}</select>
        <button class="btn btn-secundario btn-chico" data-editar-obra="${esc(o.codigo)}">Editar</button>
        <button class="btn btn-peligro btn-chico" data-eliminar-obra="${esc(o.codigo)}">Eliminar</button>
      </div></section>`).join('') || '<p class="suave">Aún no hay obras. Crea la primera.</p>'}</div>`;
  const recargar = async (mensaje) => { aviso(mensaje); await vistaObras(); };
  const intentar = (accion) => async (evento) => {
    try {
      await accion(evento);
    } catch (e) {
      aviso(e.message, 'error');
    }
  };
  $('#nueva-obra').addEventListener('click', () => formularioObra(null));
  contenido().querySelectorAll('[data-editar-obra]').forEach((b) => b.addEventListener('click',
    () => formularioObra(obras.find((o) => o.codigo === b.dataset.editarObra))));
  contenido().querySelectorAll('[data-eliminar-obra]').forEach((b) => b.addEventListener('click', intentar(async () => {
    const obra = obras.find((o) => o.codigo === b.dataset.eliminarObra);
    if (!confirm(`¿Eliminar la obra «${obra.nombre}»? Esta acción no se puede deshacer.`)) return;
    await Api.eliminarObra(obra.codigo);
    await recargar('Obra eliminada.');
  })));
  contenido().querySelectorAll('[data-sumar]').forEach((s) => s.addEventListener('change', intentar(async () => {
    if (!s.value) return;
    const persona = await Api.asignarObra(s.value, s.dataset.sumar);
    await recargar(persona.nombre + ' ahora trabaja en esta obra.');
  })));
  contenido().querySelectorAll('[data-quitar]').forEach((b) => b.addEventListener('click', intentar(async () => {
    const persona = await Api.asignarObra(b.dataset.quitar, '');
    await recargar(persona.nombre + ' quedó sin obra asignada.');
  })));
}

function formularioObra(obra) {
  const hoy = new Date().toISOString().slice(0, 10);
  const modal = Modal.abrir(obra ? 'Editar obra' : 'Nueva obra', `<form novalidate>
    <div class="campo"><label for="o-nombre">Nombre *</label><input id="o-nombre" name="nombre" required value="${esc(obra ? obra.nombre : '')}" placeholder="Ej.: Edificio Las Acacias"></div>
    <div class="campo"><label for="o-ubicacion">Ubicación</label><input id="o-ubicacion" name="ubicacion" value="${esc(obra ? obra.ubicacion : '')}" placeholder="Ej.: Av. Libertador, Caracas"></div>
    <div class="fila">
      <div class="campo"><label for="o-inicio">Inicio *</label><input id="o-inicio" name="inicio" type="date" required value="${esc(obra ? obra.inicio : hoy)}"></div>
      <div class="campo"><label for="o-fin">Fin estimado *</label><input id="o-fin" name="fin" type="date" required value="${esc(obra ? obra.fin : '')}"></div>
    </div>
    <div class="mensaje mensaje-error oculto" role="alert"></div>
    <div class="acciones"><button type="button" class="btn btn-secundario" data-cerrar>Cancelar</button>
      <button class="btn btn-primario" type="submit">Guardar</button></div></form>`);
  const formulario = modal.querySelector('form');
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    try {
      const datos = datosFormulario(formulario);
      const guardada = obra ? await Api.editarObra(obra.codigo, datos) : await Api.crearObra(datos);
      Modal.cerrar();
      aviso((obra ? 'Se actualizó la obra ' : 'Se creó la obra ') + guardada.nombre + '.');
      await vistaObras();
    } catch (e) {
      mostrarErrorFormulario(formulario, e.message);
    }
  });
}

async function vistaInvitaciones() {
  const [opciones, lista, config] = await Promise.all([Api.opcionesPersonal(), Api.invitaciones(), Api.configuracionInvitaciones()]);
  contenido().innerHTML = encabezado('Administración /', 'Invitaciones') + `
    <p class="suave" style="margin-top:-8px">Cada código sirve una sola vez. Compártelo por WhatsApp o correo con la persona que quieres sumar.</p>
    <div class="rejilla-incidencias">
      <form class="tarjeta" id="form-invitacion" novalidate aria-labelledby="t-nueva">
        <h2 id="t-nueva">Nueva invitación</h2>
        <div class="campo"><label for="i-rol">Rol</label><select id="i-rol" name="rol"><option>Empleado</option><option>Supervisor</option></select></div>
        <div class="campo" id="i-campo-supervisor"><label for="i-supervisor">Supervisor</label><select id="i-supervisor" name="supervisor">
          <option value="">— Sin supervisor —</option>${opciones.supervisores.map((s) => `<option value="${esc(s.cedula)}">${esc(s.nombre)}</option>`).join('')}</select></div>
        <div class="campo"><label for="i-obra">Obra</label><select id="i-obra" name="obra"><option value="">— Sin obra —</option>
          ${opciones.obras.map((o) => `<option value="${esc(o.codigo)}">${esc(o.nombre)}</option>`).join('')}</select></div>
        <div class="campo"><label for="i-dias">Vigencia (días)</label><input id="i-dias" name="dias" type="number" min="1" max="30" value="7"></div>
        <div class="campo"><label for="i-correo">Correo de la persona (opcional)</label><input id="i-correo" name="correo" type="email" placeholder="nombre@correo.com">
          <span class="ayuda">${config.correoReal ? '✉ Le llegará un correo con el código y un botón para unirse.'
            : 'El envío de correos no está configurado: se registra, pero no llega. Comparte el código por WhatsApp.'}</span></div>
        <button class="btn btn-primario btn-bloque" type="submit">Crear invitación</button>
        <div id="codigo-creado" class="oculto" style="margin-top:14px"></div>
      </form>
      <section class="tarjeta" aria-labelledby="t-creadas"><h2 id="t-creadas">Invitaciones creadas</h2>
        <div class="tabla-contenedor"><table><thead><tr><th>Código</th><th>Rol</th><th>Supervisor</th><th>Obra</th><th>Estado</th><th>Vence</th><th>Usada por</th><th></th></tr></thead>
        <tbody>${lista.map((v) => `<tr><td><strong class="codigo">${esc(v.codigo)}</strong></td><td>${esc(v.rol)}</td><td>${esc(v.supervisor || '—')}</td>
          <td>${esc(v.obra || '—')}</td><td>${chipEstado(v.estado)}</td><td>${fecha(v.vence)}</td><td>${esc(v.usadaPor || '—')}</td>
          <td style="white-space:nowrap">${v.estado === 'Vigente' ? `<button class="btn btn-enlace btn-chico" data-enviar="${esc(v.codigo)}">Enviar por correo</button>
            <button class="btn btn-enlace btn-chico" style="color:var(--rojo)" data-cancelar="${esc(v.codigo)}">Cancelar</button>` : ''}</td></tr>`).join('')
          || '<tr><td colspan="8" class="vacio">Aún no has creado invitaciones.</td></tr>'}</tbody></table></div></section>
    </div>`;
  const rol = $('#i-rol');
  rol.addEventListener('change', () => $('#i-campo-supervisor').classList.toggle('oculto', rol.value !== 'Empleado'));
  $('#form-invitacion').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    try {
      const creada = await Api.crearInvitacion(datosFormulario(evento.target));
      const v = creada.invitacion;
      await vistaInvitaciones();
      const enlace = location.href.split('#')[0] + '#registro';
      const mensaje = `Te invito a ConstruTrack. Crea tu cuenta en ${enlace} y únete con el código ${v.codigo}`;
      const caja = $('#codigo-creado');
      caja.classList.remove('oculto');
      caja.innerHTML = `<div class="mensaje mensaje-info"><div class="codigo-grande">${esc(v.codigo)}</div>
        <p class="pequeno" style="margin:6px 0">Vence el ${fecha(v.vence)} · ${esc(v.rol)}${v.obra ? ' · ' + esc(v.obra) : ''}</p>
        <div class="acciones" style="justify-content:center"><button type="button" class="btn btn-contorno btn-chico" id="copiar-codigo">Copiar código</button>
        <button type="button" class="btn btn-contorno btn-chico" id="copiar-mensaje">Copiar mensaje para enviar</button></div></div>`;
      const copiar = async (texto, que) => {
        try { await navigator.clipboard.writeText(texto); aviso(que + ' copiado.'); } catch (e) { aviso('Cópialo a mano: ' + texto, 'error'); }
      };
      $('#copiar-codigo').addEventListener('click', () => copiar(v.codigo, 'Código'));
      $('#copiar-mensaje').addEventListener('click', () => copiar(mensaje, 'Mensaje'));
      aviso('Invitación ' + v.codigo + ' creada.');
      if (creada.avisoCorreo) aviso(creada.avisoCorreo, creada.correoEnviado ? 'ok' : 'error');
    } catch (e) {
      aviso(e.message, 'error');
    }
  });
  contenido().querySelectorAll('[data-enviar]').forEach((b) => b.addEventListener('click', () => {
    const modal = Modal.abrir('Enviar ' + b.dataset.enviar + ' por correo', `<form novalidate>
      <div class="campo"><label for="e-correo">Correo de la persona</label><input id="e-correo" name="correo" type="email" required placeholder="nombre@correo.com"></div>
      ${config.correoReal ? '' : '<p class="mensaje mensaje-info pequeno">El envío de correos no está configurado en este servidor: quedará registrado, pero no llegará.</p>'}
      <div class="mensaje mensaje-error oculto" role="alert"></div>
      <div class="acciones"><button type="button" class="btn btn-secundario" data-cerrar>Cancelar</button>
        <button class="btn btn-primario" type="submit">Enviar</button></div></form>`);
    const formulario = modal.querySelector('form');
    formulario.addEventListener('submit', async (evento) => {
      evento.preventDefault();
      const boton = formulario.querySelector('button[type=submit]');
      boton.disabled = true;
      boton.textContent = 'Enviando…';
      try {
        const r = await Api.enviarInvitacion(b.dataset.enviar, $('#e-correo').value);
        Modal.cerrar();
        aviso(r.mensaje);
      } catch (e) {
        mostrarErrorFormulario(formulario, e.message);
        boton.disabled = false;
        boton.textContent = 'Enviar';
      }
    });
  }));
  contenido().querySelectorAll('[data-cancelar]').forEach((b) => b.addEventListener('click', async () => {
    try {
      await Api.cancelarInvitacion(b.dataset.cancelar);
      aviso('Invitación ' + b.dataset.cancelar + ' cancelada.');
      await vistaInvitaciones();
    } catch (e) {
      aviso(e.message, 'error');
    }
  }));
}
