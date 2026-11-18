const HORAS = Array.from({ length: 34 }, (_, i) => `${String(5 + Math.floor(i / 2)).padStart(2, '0')}:${i % 2 ? '30' : '00'}`);
const esRevisor = () => estado.usuario && estado.usuario.rol !== 'Empleado';
const esGerente = () => estado.usuario && estado.usuario.rol === 'Gerente';
const esSupervisor = () => estado.usuario && estado.usuario.rol === 'Supervisor';
const contenido = () => $('#principal');

function encabezado(migas, titulo, acciones = '') {
  return `${migas ? `<div class="migas">${esc(migas)}</div>` : ''}
    <div class="titulo-vista"><h1>${esc(titulo)}</h1><div class="acciones" style="margin:0">${acciones}</div></div>`;
}

const GRUPO_OBRAS = 'Personal y obras';
const GRUPO_ADMIN = 'Administración';

const VISTAS = [
  { id: 'asistencia', titulo: 'Asistencia', grupo: '', icono: '⏱', disponible: () => true, dibujar: vistaAsistencia },
  { id: 'permisos', titulo: 'Permisos y reposos', grupo: '', icono: '✚', disponible: () => true, dibujar: vistaPermisos },
  { id: 'reportes-obra', titulo: 'Reportes en obra', grupo: '', icono: '⚑', disponible: esRevisor, dibujar: vistaReportesObra },
  { id: 'tablero', titulo: 'Tablero', grupo: GRUPO_OBRAS, icono: '▦', disponible: esRevisor, dibujar: vistaTablero },
  { id: 'supervision', titulo: 'Supervisión', grupo: GRUPO_OBRAS, icono: '▤', disponible: esRevisor, dibujar: vistaSupervision },
  { id: 'obras', titulo: 'Obras', grupo: GRUPO_OBRAS, icono: '⌂', disponible: esRevisor, dibujar: vistaObras },
  { id: 'personal', titulo: 'Personal', grupo: GRUPO_OBRAS, icono: '☰', disponible: esRevisor, dibujar: vistaPersonal },
  { id: 'talento', titulo: 'Talento humano', grupo: GRUPO_ADMIN, icono: '⚇', disponible: esGerente, dibujar: vistaTalento },
  { id: 'inventario', titulo: 'Inventario', grupo: GRUPO_ADMIN, icono: '⚒', disponible: esGerente, dibujar: vistaInventario },
  { id: 'catalogo', titulo: 'Puestos y áreas', grupo: GRUPO_ADMIN, icono: '☷', disponible: esGerente, dibujar: vistaCatalogo },
  { id: 'invitaciones', titulo: 'Invitaciones', grupo: GRUPO_ADMIN, icono: '✉', disponible: esGerente, dibujar: vistaInvitaciones },
  { id: 'codigo-obra', titulo: 'Código de asistencia', grupo: GRUPO_OBRAS, icono: '▣', oculta: true, disponible: esRevisor, dibujar: vistaCodigoObra },
  { id: 'seguridad', titulo: 'Seguridad', grupo: GRUPO_ADMIN, icono: '🔒', disponible: () => Boolean(estado.usuario && estado.usuario.admiteDosPasos), dibujar: vistaSeguridad },
];

async function vistaSeguridad() {
  const zona = contenido();
  const info = await Api.dosPasos();
  zona.innerHTML = encabezado('', 'Seguridad') + `
    <section class="tarjeta tarjeta-angosta" aria-labelledby="t-dos-pasos">
      <h2 id="t-dos-pasos">Verificación en dos pasos ${info.activa ? chip('Activa', 'verde') : chip('Desactivada', 'gris')}</h2>
      <p class="suave">Además de tu clave, al entrar se te pedirá un código de 6 dígitos que genera una aplicación en tu
        teléfono. Así, aunque alguien descubra tu clave, no podrá entrar a tu empresa.</p>
      <div id="zona-dos-pasos"></div>
    </section>`;
  const panel = zona.querySelector('#zona-dos-pasos');
  if (info.activa) {
    panel.innerHTML = `<p>Te quedan <strong>${info.respaldosRestantes}</strong> códigos de respaldo.</p>
      <form id="form-desactivar" novalidate>
        <h3>Desactivar</h3>
        <div class="campo"><label for="d-clave">Tu clave *</label><input id="d-clave" name="clave" type="password" autocomplete="current-password"></div>
        <div class="campo"><label for="d-codigo">Código de la aplicación *</label><input id="d-codigo" name="codigo" inputmode="numeric" autocomplete="one-time-code"></div>
        <div class="mensaje mensaje-error oculto" role="alert"></div>
        <button class="btn btn-secundario" type="submit">Desactivar verificación en dos pasos</button>
      </form>`;
    const formulario = $('#form-desactivar');
    formulario.addEventListener('submit', async (evento) => {
      evento.preventDefault();
      try {
        const respuesta = await Api.desactivarDosPasos($('#d-clave').value, $('#d-codigo').value.trim());
        estado.usuario.dosPasos = false;
        aviso(respuesta.mensaje);
        vistaSeguridad();
      } catch (e) {
        mostrarErrorFormulario(formulario, e.message);
      }
    });
    return;
  }
  panel.innerHTML = '<button class="btn btn-primario" id="iniciar-dos-pasos" type="button">Activar verificación en dos pasos</button>';
  $('#iniciar-dos-pasos').addEventListener('click', async () => {
    const inscripcion = await Api.iniciarDosPasos();
    panel.innerHTML = `<ol class="pasos-dos-pasos">
        <li>Instala <strong>Google Authenticator</strong> o <strong>Microsoft Authenticator</strong> en tu teléfono.</li>
        <li>En la aplicación, elige «Escanear un código QR» y apunta a este código:
          <div class="marco-qr">${QR.svg(inscripcion.enlace, 'Código QR para la aplicación de autenticación')}</div>
          <span class="pequeno suave">¿No puedes escanearlo? Elige «Ingresar una clave» y escribe:</span>
          <code class="clave-manual">${esc(inscripcion.secreto.replace(/(.{4})/g, '$1 ').trim())}</code></li>
        <li>Escribe el código de 6 dígitos que aparece en la aplicación:</li>
      </ol>
      <form id="form-activar" novalidate class="fila-activar">
        <div class="campo"><label for="a-codigo" class="solo-lector">Código de 6 dígitos</label>
          <input id="a-codigo" class="codigo-grande" inputmode="numeric" autocomplete="one-time-code" maxlength="6" placeholder="000000"></div>
        <div class="mensaje mensaje-error oculto" role="alert"></div>
        <button class="btn btn-primario" type="submit">Activar</button>
      </form>`;
    const formulario = $('#form-activar');
    formulario.addEventListener('submit', async (evento) => {
      evento.preventDefault();
      try {
        const respuesta = await Api.activarDosPasos($('#a-codigo').value.trim());
        estado.usuario.dosPasos = true;
        aviso(respuesta.mensaje);
        zona.querySelector('#t-dos-pasos').innerHTML = 'Verificación en dos pasos ' + chip('Activa', 'verde');
        const texto = 'Códigos de respaldo de ConstruTrack (cada uno sirve una sola vez):\n\n' + respuesta.respaldos.join('\n') + '\n';
        panel.innerHTML = `<div class="mensaje mensaje-info" role="status">Guarda estos códigos en un lugar seguro.
            Si pierdes el teléfono, cada uno te deja entrar una sola vez.</div>
          <div class="respaldos">${respuesta.respaldos.map((r) => `<code>${esc(r)}</code>`).join('')}</div>
          <div class="acciones" style="justify-content:flex-start">
            <a class="btn btn-secundario" download="construtrack-codigos-respaldo.txt"
              href="${URL.createObjectURL(new Blob([texto], { type: 'text/plain' }))}">Descargar códigos</a>
            <button class="btn btn-primario" id="respaldos-listo" type="button">Ya los guardé</button></div>`;
        $('#respaldos-listo').addEventListener('click', vistaSeguridad);
      } catch (e) {
        mostrarErrorFormulario(formulario, e.message);
      }
    });
    $('#a-codigo').focus();
  });
}

async function vistaAsistencia() {
  const zona = contenido();
  const u = estado.usuario = await Api.yo();
  const lunes = new Date();
  lunes.setDate(lunes.getDate() - ((lunes.getDay() + 6) % 7));
  const desde = lunes.toISOString().slice(0, 10);
  const semana = u.asistencias.filter((a) => a.fecha >= desde);
  const tardes = semana.filter((a) => a.tarde).length;
  const puntualidad = semana.length ? Math.round(100 * (semana.length - tardes) / semana.length) : 100;
  const hoy = u.asistenciaHoy;
  const incapacidad = u.incapacidadHoy;
  const horas = (a) => (a.horaSalida ? formatoHoras(a.minutos) : '—');
  const extra = (a) => (a.minutosExtra
    ? chip('+' + formatoHoras(a.minutosExtra), a.estadoExtra === 'Aprobadas' ? 'verde' : a.estadoExtra === 'Rechazadas' ? 'gris' : 'amarillo')
      + `<br><span class="pequeno suave">${esc(a.estadoExtra)}</span>`
    : '—');
  const herramientas = u.herramientas || [];
  const ausencias = u.ausencias || [];
  zona.innerHTML = encabezado('', 'Asistencia') + `
    ${u.admiteDosPasos && !u.dosPasos ? `<div class="mensaje mensaje-info">Protege tu empresa:
      <a href="#seguridad">activa la verificación en dos pasos</a>.</div>` : ''}
    ${incapacidad ? `<div class="mensaje mensaje-info" role="status"><strong>Incapacidad médica:</strong> tienes un reposo aprobado del
      ${fecha(incapacidad.desde)} al ${fecha(incapacidad.hasta)}. Esos días quedan justificados y no necesitas marcar asistencia.</div>` : ''}
    <div class="rejilla-2">
      <section class="tarjeta" aria-labelledby="t-hoy">
        <h2 id="t-hoy">Hola, ${esc(u.nombre.split(' ')[0])}</h2>
        <div class="estado-jornada">${u.presente ? chip('● En la obra', 'verde') : chip('○ Fuera de la obra', 'gris')}
          ${hoy ? `<span class="suave pequeno">Entraste hoy a las ${esc(hoy.horaEntrada)}${hoy.tarde ? ' (tarde)' : ''}</span>` : ''}</div>
        <p class="suave pequeno">Horario: ${esc(textoDias(u.horario.dias))}, de ${esc(u.horario.horaEntrada)} a ${esc(u.horario.horaSalida)}
          ${u.puesto ? ' · ' + esc(u.puesto) : ''}${u.obra ? ' · Obra: ' + esc(u.obra) : ''}${u.supervisor ? ' · Supervisor: ' + esc(u.supervisor.nombre) : ''}</p>
        <p class="pequeno control-marca">Al marcar se registra tu ubicación${u.controlObra && u.controlObra.exigeCodigo ? ' y se pide el código QR de la obra' : ''}.
          ${u.rol === 'Gerente' ? '' : 'Si no permites la ubicación, la marca queda por confirmar hasta que la revise tu supervisor.'}</p>
        <div class="acciones" style="justify-content:flex-start">
          <button class="btn btn-primario" id="marcar-entrada" ${u.presente || incapacidad ? 'disabled' : ''}>Marcar entrada</button>
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
    <section class="tarjeta" style="margin-top:16px"><h2>Historial reciente</h2>
      <div class="tabla-contenedor"><table>
        <thead><tr><th>Fecha</th><th>Entrada</th><th>Salida</th><th>Llegada</th><th>Horas</th><th>Extra</th><th>Control</th><th>Ubicación</th></tr></thead>
        <tbody>${u.asistencias.map((a) => `<tr><td>${fecha(a.fecha)}</td><td>${esc(a.horaEntrada)}</td>
          <td>${a.horaSalida ? esc(a.horaSalida) : chip('En curso', 'azul')}</td>
          <td>${a.tarde ? chip('Tarde', 'amarillo') : chip('Puntual', 'verde')}</td><td>${horas(a)}</td><td>${extra(a)}</td>
          <td>${chipControl(a)}</td><td>${Mapa.boton(a.ubicacionEntrada, 'Entrada del ' + fecha(a.fecha))}</td></tr>`).join('')
          || '<tr><td colspan="8" class="vacio">Aún no tienes jornadas registradas. Marca tu primera entrada.</td></tr>'}</tbody>
      </table></div></section>
    <div class="rejilla-2" style="margin-top:16px">
      <section class="tarjeta" aria-labelledby="t-ausencias"><h2 id="t-ausencias">Días sin marcar</h2>
        <p class="suave pequeno" style="margin-top:-6px">Días laborables de los últimos 30 días sin una jornada válida.</p>
        ${ausencias.length ? `<ul class="lista-simple">${ausencias.map((a) => `<li><span>${fecha(a.fecha)}</span>
          ${chip(a.tipo, a.tipo === 'Incapacidad' ? 'morado' : a.justificada ? 'azul' : 'rojo')}</li>`).join('')}</ul>`
          : '<p class="suave">No tienes días sin marcar. ¡Bien!</p>'}</section>
      <section class="tarjeta" aria-labelledby="t-herramientas"><h2 id="t-herramientas">Herramientas a mi cargo</h2>
        ${herramientas.length ? `<ul class="lista-simple">${herramientas.map((h) => `<li><span><strong>${esc(h.nombreArticulo)}</strong>
          ${h.cantidad > 1 ? '× ' + h.cantidad : ''} <span class="pequeno suave">${esc(h.articulo)} · desde ${fecha(h.fecha)}</span></span>
          ${h.devolucionPrevista ? chip((h.vencida ? 'Vencida: ' : 'Devolver el ') + fecha(h.devolucionPrevista), h.vencida ? 'rojo' : 'gris') : ''}</li>`).join('')}</ul>`
          : '<p class="suave">No tienes herramientas ni equipos asignados.</p>'}</section>
    </div>`;
  const marcar = async (tipo, codigo) => {
    try {
      const respuesta = await Marcacion.marcar(tipo, u.controlObra, codigo);
      if (!respuesta) return;
      aviso(respuesta.mensaje, respuesta.mensaje.includes('por confirmar') ? 'aviso' : 'ok');
      await vistaAsistencia();
    } catch (e) {
      aviso(e.message, 'error');
    }
  };
  $('#marcar-entrada').addEventListener('click', () => marcar('entrada'));
  $('#marcar-salida').addEventListener('click', () => marcar('salida'));
  const pendiente = Marcacion.tomarPendiente();
  if (pendiente) marcar(u.presente ? 'salida' : 'entrada', pendiente);
}

function chipControl(a) {
  const colores = { VERIFICADA: 'verde', CONFIRMADA: 'verde', POR_CONFIRMAR: 'amarillo', RECHAZADA: 'rojo' };
  if (!colores[a.verificacion]) return '—';
  return `<span title="${esc(a.motivo || '')}">${chip(a.verificacionTexto, colores[a.verificacion])}</span>`
    + (a.verificacion !== 'VERIFICADA' && a.motivo ? `<br><span class="pequeno suave">${esc(a.motivo)}</span>` : '');
}

async function vistaCodigoObra() {
  const zona = contenido();
  const codigoObra = ruta().parametros.get('obra');
  const primero = await Api.codigoObra(codigoObra);
  zona.innerHTML = encabezado('Obras /', 'Código de asistencia',
    '<button class="btn btn-secundario" id="pantalla-completa" type="button">Pantalla completa</button>') + `
    <section class="tarjeta pantalla-codigo" id="pantalla-codigo" aria-labelledby="codigo-obra-nombre">
      <h2 id="codigo-obra-nombre">${esc(primero.nombre)}</h2>
      <p class="suave">Escanéalo con el celular para marcar la entrada o la salida. Cambia cada 30 segundos.</p>
      <div class="marco-codigo-qr"></div>
      <p class="codigo-digitos" aria-live="polite"></p>
      <div class="barra-pista barra-codigo"><span></span></div>
      <p class="pequeno suave">¿No pueden escanear? Que escriban estos 6 dígitos en «Asistencia».</p>
    </section>`;
  const dibujar = (dato) => {
    const enlace = `${location.origin}${location.pathname}#marcar?obra=${encodeURIComponent(dato.obra)}&codigo=${dato.codigo}`;
    zona.querySelector('.marco-codigo-qr').innerHTML = QR.svg(enlace, 'Código QR de asistencia de ' + dato.nombre);
    zona.querySelector('.codigo-digitos').textContent = dato.codigo.replace(/(\d{3})(\d{3})/, '$1 $2');
    const barra = zona.querySelector('.barra-codigo span');
    barra.style.transition = 'none';
    barra.style.width = Math.round(100 * dato.segundosRestantes / 30) + '%';
    requestAnimationFrame(() => requestAnimationFrame(() => {
      barra.style.transition = `width ${dato.segundosRestantes}s linear`;
      barra.style.width = '0%';
    }));
  };
  const refrescar = (espera) => setTimeout(async () => {
    if (!zona.isConnected) return;
    try {
      const dato = await Api.codigoObra(codigoObra);
      dibujar(dato);
      refrescar(dato.segundosRestantes * 1000 + 400);
    } catch (e) {
      refrescar(5000);
    }
  }, espera);
  dibujar(primero);
  refrescar(primero.segundosRestantes * 1000 + 400);
  $('#pantalla-completa').addEventListener('click', () => {
    const pantalla = zona.querySelector('#pantalla-codigo');
    if (pantalla.requestFullscreen) pantalla.requestFullscreen();
  });
}

function distanciaObra(punto, obra) {
  if (!punto || !obra || obra.latitud === null || obra.latitud === undefined) return '';
  const metros = Mapa.distancia(punto, obra);
  const fuera = obra.radio && metros > obra.radio + Math.min(punto.precision || 0, 100);
  return `<br><span class="pequeno ${fuera ? 'texto-rojo' : 'suave'}">a ${Mapa.texto(metros)} de la obra</span>`;
}

async function vistaSupervision() {
  const zona = contenido();
  const periodo = estado.periodo || 'ultimos7';
  const diaMapa = estado.diaMapa || new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10);
  const [est, alertas, extras, marcas, ubicaciones] = await Promise.all([Api.estadisticas(periodo), Api.alertas(periodo),
    Api.horasExtra(), Api.porConfirmar(), Api.ubicaciones(diaMapa)]);
  const color = (v) => (v >= 90 ? '#22a06b' : v >= 75 ? '#e2b203' : '#c9372c');
  const nivel = { Gerente: 0, Supervisor: 1 };
  est.personas = est.personas.map((p, i) => ({ ...p, orden: i }))
    .sort((a, b) => (nivel[a.rol] ?? 2) - (nivel[b.rol] ?? 2) || a.orden - b.orden);
  const lider = (p) => p.rol !== 'Empleado';
  zona.innerHTML = encabezado('', 'Supervisión', `
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
        <div class="barras">${est.personas.map((p) => `<div class="barra-fila${lider(p) ? ' barra-lider' : ''}">
          <span title="${esc(p.nombre)} · ${esc(p.rol)}">${esc(p.nombre.split(' ')[0])} ${esc((p.nombre.split(' ')[1] || '')[0] || '')}.${lider(p) ? ` <span class="marca-rol marca-${p.rol.toLowerCase()}">${p.rol === 'Gerente' ? 'G' : 'S'}</span>` : ''}</span>
          <div class="barra-pista" role="img" aria-label="${esc(p.nombre)}: ${Math.round(p.tasaAsistencia)}% de asistencia">
            <span style="width:${p.tasaAsistencia}%;background:${color(p.tasaAsistencia)}"></span></div>
          <strong>${Math.round(p.tasaAsistencia)}%</strong></div>`).join('') || '<p class="suave">Sin datos.</p>'}</div></section>
      <section class="tarjeta" aria-labelledby="t-alertas"><h2 id="t-alertas">Alertas automáticas</h2><p class="suave pequeno" style="margin-top:-6px">De la más antigua a la más reciente.</p>
        ${alertas.map((a) => `<div class="alerta">${chip(a.nivel, a.nivel === 'Alta' ? 'rojo' : 'amarillo')}
          <div><strong>${esc(a.nombre)}</strong> <span class="suave pequeno">· desde ${fecha(a.desde)}</span><br><span class="suave pequeno">${esc(a.mensaje)}</span></div></div>`).join('')
          || '<p class="suave">Sin alertas en este período. Todo en orden.</p>'}</section>
    </div>
    <section class="tarjeta" style="margin-top:16px" aria-labelledby="t-ranking"><h2 id="t-ranking">Tabla de puntualidad</h2>
      <p class="suave pequeno" style="margin-top:-6px">Primero el gerente y los supervisores (destacados); luego el personal, de quien más requiere atención a quien menos. Solo cuenta los días desde que cada persona entró a la empresa.
        Las horas extra cuentan cuando el supervisor o el gerente las aprueba.</p>
      <div class="tabla-contenedor"><table><thead><tr><th>Persona</th><th>Horas trabajadas / esperadas</th>
        <th>Extra diaria</th><th>Extra día libre</th><th>Extra por aprobar</th><th>Retardos</th><th>Faltas sin justificar</th>
        <th>Justificadas</th><th>Incapacidad</th><th>Puntualidad</th></tr></thead><tbody>
        ${est.personas.map((p) => `<tr${lider(p) ? ` class="fila-lider fila-${p.rol.toLowerCase()}"` : ''}><td style="white-space:nowrap">${lider(p) ? '<strong>' + esc(p.nombre) + '</strong> ' + chipRol(p.rol) : esc(p.nombre)}</td>
          <td style="white-space:nowrap"><strong>${formatoHoras(p.minutos)}</strong> / ${formatoHoras(p.minutosEsperados)}</td>
          <td>${p.extraDiaria ? chip('+' + formatoHoras(p.extraDiaria), 'verde') : '—'}</td>
          <td>${p.extraDiaLibre ? chip('+' + formatoHoras(p.extraDiaLibre), 'verde') : '—'}</td>
          <td>${p.extraPendiente ? chip(formatoHoras(p.extraPendiente), 'amarillo') : '—'}</td>
          <td>${p.retardos ? chip(p.retardos, 'amarillo') : 0}</td><td>${p.ausencias ? chip(p.ausencias, 'rojo') : 0}</td>
          <td>${p.justificadas}</td><td>${p.incapacidad ? chip(p.incapacidad + ' d', 'morado') : 0}</td><td>${Math.round(p.puntualidad)}%</td></tr>`).join('')}</tbody></table></div></section>
    <section class="tarjeta" style="margin-top:16px" aria-labelledby="t-ubicaciones"><h2 id="t-ubicaciones">Dónde marcó cada persona</h2>
      <p class="suave pequeno" style="margin-top:-6px">La ubicación que envió el teléfono al marcar entrada y salida. Ábrela en el mapa para compararla con la obra.</p>
      <div class="campo" style="max-width:220px"><label for="dia-mapa">Día</label><input id="dia-mapa" type="date" value="${esc(diaMapa)}"></div>
      ${ubicaciones.length ? `<div class="tabla-contenedor"><table><thead><tr><th>Persona</th><th>Obra</th><th>Entrada</th><th>Salida</th>
        <th>Control</th></tr></thead><tbody>
        ${ubicaciones.map((m) => `<tr><td>${esc(m.nombre)}</td><td>${esc(m.obra ? m.obra.nombre : '—')}</td>
          <td>${esc(m.jornada.horaEntrada)}<br>${Mapa.boton(m.jornada.ubicacionEntrada, 'Entrada de ' + m.nombre, m.obra)}${distanciaObra(m.jornada.ubicacionEntrada, m.obra)}</td>
          <td>${m.jornada.horaSalida ? esc(m.jornada.horaSalida) + '<br>' + Mapa.boton(m.jornada.ubicacionSalida, 'Salida de ' + m.nombre, m.obra)
            + distanciaObra(m.jornada.ubicacionSalida, m.obra) : chip('En curso', 'azul')}</td>
          <td>${chipControl(m.jornada)}</td></tr>`).join('')}</tbody></table></div>` : '<p class="suave">Nadie marcó ese día.</p>'}</section>
    <section class="tarjeta" style="margin-top:16px" aria-labelledby="t-marcas"><h2 id="t-marcas">Marcas por confirmar</h2>
      <p class="suave pequeno" style="margin-top:-6px">Entradas o salidas hechas sin el código de la obra o lejos de ella.
        Si rechazas una, esa jornada no cuenta como asistencia.</p>
      ${marcas.length ? `<div class="tabla-contenedor"><table><thead><tr><th>Persona</th><th>Obra</th><th>Fecha</th>
        <th>Entrada – salida</th><th>Motivo</th><th><span class="solo-lector">Acciones</span></th></tr></thead><tbody>
        ${marcas.map((m) => `<tr><td>${esc(m.nombre)}</td><td>${esc(m.obra || '—')}</td><td>${fecha(m.fecha)}</td>
          <td>${esc(m.horaEntrada)} – ${esc(m.horaSalida || '…')}</td><td>${esc(m.motivo)}<br>${Mapa.boton(m.ubicacionEntrada, 'Entrada de ' + m.nombre, m.obraMapa)}</td>
          <td style="white-space:nowrap"><button class="btn btn-exito btn-chico" data-marca="CONFIRMADA" data-cedula="${esc(m.cedula)}" data-fecha="${esc(m.fecha)}" data-entrada="${esc(m.horaEntrada)}">Confirmar</button>
            <button class="btn btn-peligro btn-chico" data-marca="RECHAZADA" data-cedula="${esc(m.cedula)}" data-fecha="${esc(m.fecha)}" data-entrada="${esc(m.horaEntrada)}">Rechazar</button></td></tr>`).join('')}
        </tbody></table></div>` : '<p class="suave">No hay marcas por confirmar.</p>'}</section>
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
      <p class="suave pequeno" style="margin-top:-6px">Se abren en Excel para revisarlos o imprimirlos. Los de permisos y de reportes en obra
        incluyen también los aprobados y rechazados que ya salieron del tablero, con quién los autorizó y su nota.</p>
      <div class="acciones" style="justify-content:flex-start;align-items:flex-end">
        <div class="campo" style="margin:0"><label for="tipo-reporte">Tipo</label><select id="tipo-reporte">
          <option value="asistencia">Asistencia</option><option value="puntualidad">Puntualidad</option>
          <option value="permisos">Permisos y reposos</option><option value="reportes-obra">Reportes en obra</option></select></div>
        <div class="campo" style="margin:0"><label for="formato-reporte">Formato</label><select id="formato-reporte">
          <option value="csv">Excel (CSV)</option><option value="txt">Texto</option></select></div>
        <div class="campo" style="margin:0"><label for="periodo-reporte">Período</label><select id="periodo-reporte">
          <option value="${esc(periodo)}">El elegido arriba</option><option value="todo">Todo el historial</option></select></div>
        <button class="btn btn-primario" id="descargar-reporte">Descargar reporte</button>
      </div></section>`;
  $('#periodo').addEventListener('change', (e) => { estado.periodo = e.target.value; vistaSupervision(); });
  $('#dia-mapa').addEventListener('change', (e) => { estado.diaMapa = e.target.value; vistaSupervision(); });
  zona.querySelectorAll('[data-marca]').forEach((b) => b.addEventListener('click', async () => {
    try {
      await Api.revisarAsistencia(b.dataset.cedula, b.dataset.fecha, b.dataset.entrada, b.dataset.marca);
      aviso(b.dataset.marca === 'CONFIRMADA' ? 'Marca confirmada.' : 'Marca rechazada: esa jornada no cuenta.');
      vistaSupervision();
    } catch (e) {
      aviso(e.message, 'error');
    }
  }));
  zona.querySelectorAll('[data-extra]').forEach((b) => b.addEventListener('click', async () => {
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
      const nombre = await Api.descargarReporte($('#tipo-reporte').value, $('#formato-reporte').value, $('#periodo-reporte').value);
      aviso('Reporte descargado: ' + nombre);
    } catch (e) {
      aviso(e.message, 'error');
    }
  });
}

async function vistaPersonal() {
  const zona = contenido();
  const lista = await Api.listarUsuarios();
  const filtro = (estado.filtroPersonal || '').toLowerCase();
  const area = estado.areaPersonal || '';
  const areas = [...new Set(lista.map((u) => u.area).filter(Boolean))].sort();
  const visibles = lista.filter((u) => (!area || u.area === area)
    && (!filtro || [u.nombre, u.cedula, u.rol, u.puesto, u.area, u.obra, u.correo].join(' ').toLowerCase().includes(filtro)));
  zona.innerHTML = encabezado('', 'Personal', `
    <label class="solo-lector" for="area-personal">Área</label>
    <select id="area-personal" style="width:auto"><option value="">Todas las áreas</option>
      ${areas.map((a) => `<option ${a === area ? 'selected' : ''}>${esc(a)}</option>`).join('')}</select>
    <label class="solo-lector" for="buscar-personal">Buscar</label>
    <input id="buscar-personal" placeholder="Buscar nombre, cédula, puesto…" value="${esc(estado.filtroPersonal || '')}" style="width:220px">
    ${esGerente() ? '<button class="btn btn-primario" id="registrar-persona">+ Registrar persona</button>' : ''}`) + `
    ${esGerente() ? '<p class="suave pequeno" style="margin-top:-8px">Los despidos y renuncias se registran en <a href="#talento">Talento humano</a>.</p>' : ''}
    <div class="tabla-contenedor"><table>
      <thead><tr><th>Persona</th><th>Cédula</th><th>Rol</th><th>Puesto y área</th><th>Estado</th><th>Horario</th><th>Supervisor</th><th>Obra</th><th><span class="solo-lector">Acciones</span></th></tr></thead>
      <tbody>${visibles.map((u) => `<tr>
        <td><strong>${esc(u.nombre)}</strong>${u.correo ? `<br><span class="suave pequeno">${esc(u.correo)}</span>` : ''}</td>
        <td>${esc(u.cedula)}</td><td>${chipRol(u.rol)}</td>
        <td>${esc(u.puesto || '—')}<br><span class="suave pequeno">${esc(u.area || 'Sin área')}</span></td>
        <td>${u.presente ? chip('En la obra', 'verde') : chip('Fuera', 'gris')}</td>
        <td class="pequeno">${esc(u.horario.dias)}<br>${esc(u.horario.horaEntrada)}–${esc(u.horario.horaSalida)}</td>
        <td>${u.supervisor ? esc(u.supervisor.nombre) : '—'}</td><td>${esc(u.obra || '—')}</td>
        <td style="white-space:nowrap"><button class="btn btn-enlace btn-chico" data-historial="${esc(u.cedula)}">Historial</button>
          ${esGerente() && u.rol !== 'Gerente' ? `<button class="btn btn-enlace btn-chico" data-editar="${esc(u.cedula)}">Editar</button>` : ''}</td>
      </tr>`).join('') || '<tr><td colspan="9" class="vacio">No hay coincidencias.</td></tr>'}</tbody></table></div>`;
  const buscador = $('#buscar-personal');
  buscador.addEventListener('input', () => {
    estado.filtroPersonal = buscador.value;
    vistaPersonal().then(() => { const b = $('#buscar-personal'); b.focus(); b.setSelectionRange(b.value.length, b.value.length); });
  });
  $('#area-personal').addEventListener('change', (e) => { estado.areaPersonal = e.target.value; vistaPersonal(); });
  if (esGerente()) $('#registrar-persona').addEventListener('click', () => formularioPersona(null));
  zona.querySelectorAll('[data-editar]').forEach((b) => b.addEventListener('click', () => formularioPersona(b.dataset.editar)));
  zona.querySelectorAll('[data-historial]').forEach((b) => b.addEventListener('click', () => historialPersona(b.dataset.historial)));
}

const opcionesLista = (lista, elegido, vacio) => `<option value="">${esc(vacio)}</option>`
  + lista.map((x) => `<option ${x === elegido ? 'selected' : ''}>${esc(x)}</option>`).join('');

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
    <div class="fila">
      <div class="campo"><label for="p-puesto">Puesto</label><select id="p-puesto" name="puesto">
        ${opcionesLista(opciones.catalogo.puestos, u && u.puesto, '— Sin puesto —')}</select></div>
      <div class="campo"><label for="p-area">Área</label><select id="p-area" name="area">
        ${opcionesLista(opciones.catalogo.areas, u && u.area, '— Sin área —')}</select></div>
    </div>
    <p class="ayuda" style="margin-top:-8px">Los puestos y las áreas se administran en <a href="#catalogo">Puestos y áreas</a>.</p>
    <div class="campo"><label for="p-clave">Clave</label><input id="p-clave" name="clave" type="password" autocomplete="new-password">
      <span class="ayuda">${u ? 'Déjala vacía para no cambiarla.' : 'Mínimo 8 caracteres, con letras y números.'}</span></div>
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
      <option value="">— Sin obra —</option>${opciones.obras.filter((o) => o.admitePersonal || (u && u.obra === o.nombre)).map((o) =>
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

function tablaNovedades(lista, conPersona, vacio) {
  return `<div class="tabla-contenedor"><table><thead><tr><th>#</th><th>Tipo</th>${conPersona ? '<th>Persona</th>' : ''}<th>Fechas</th>
    <th>Estado</th><th>Detalle</th></tr></thead><tbody>
    ${lista.map((n) => `<tr><td>${n.id}</td><td>${esc(n.nombreTipo)}${n.obra ? `<br><span class="pequeno suave">${esc(n.obra)}</span>` : ''}</td>
      ${conPersona ? `<td>${esc(n.nombreAfectado)}</td>` : ''}
      <td style="white-space:nowrap">${fecha(n.desde)}${n.hasta && n.hasta !== n.desde ? ' – ' + fecha(n.hasta) : ''}
        <br><span class="pequeno suave">${n.dias} día(s)</span></td>
      <td>${chipEstado(n.estadoNombre)}${n.revisadaPor ? `<br><span class="pequeno suave">${n.estado === 'APROBADA' ? 'Autorizó' : 'Rechazó'}: ${esc(n.revisadaPor)}</span>` : ''}</td>
      <td>${esc(n.descripcion)}${n.detalle ? `<br><span class="pequeno suave">${esc(n.detalle)}</span>` : ''}
        ${n.nota ? `<br><span class="pequeno">Nota: «${esc(n.nota)}»</span>` : ''}
        <br><span class="pequeno suave">Registró: ${esc(n.registradaPor)}</span> ${Adjuntos.boton(n.archivo, 'Ver reposo')}</td></tr>`).join('')
      || `<tr><td colspan="${conPersona ? 6 : 5}" class="vacio">${esc(vacio)}</td></tr>`}</tbody></table></div>`;
}

async function historialPersona(cedula) {
  try {
    const u = await Api.verUsuario(cedula);
    const novedades = u.novedades || [];
    const licencias = novedades.filter((n) => n.categoria === 'LICENCIA');
    const reportes = novedades.filter((n) => n.categoria === 'REPORTE_OBRA');
    const herramientas = u.herramientas || [];
    const ausencias = u.ausencias || [];
    const pestanas = [
      ['h-asistencia', `Asistencia (${u.asistencias.length})`],
      ['h-ausencias', `Días sin marcar (${ausencias.length})`],
      ['h-permisos', `Permisos y reposos (${licencias.length})`],
      ...(esRevisor() ? [['h-reportes', `Reportes en obra (${reportes.length})`]] : []),
      ['h-herramientas', `Herramientas (${herramientas.length})`],
    ];
    const modal = Modal.abrir('Historial de ' + u.nombre, `<p class="suave">${esc(u.puesto || 'Sin puesto')} · ${esc(u.area || 'Sin área')} ·
      ${esc(textoDias(u.horario.dias))}, de ${esc(u.horario.horaEntrada)} a ${esc(u.horario.horaSalida)}
      ${u.fechaIngreso ? ' · En la empresa desde el ' + fecha(u.fechaIngreso) : ''}</p>
      <div class="pestanas" role="tablist">${pestanas.map(([id, texto], i) => `<button type="button" role="tab" class="pestana${i ? '' : ' activa'}"
        aria-selected="${i ? 'false' : 'true'}" data-pestana="${id}">${esc(texto)}</button>`).join('')}</div>
      <div id="h-asistencia" role="tabpanel"><div class="tabla-contenedor"><table><thead><tr><th>Fecha</th><th>Entrada</th><th>Salida</th><th>Llegada</th><th>Horas</th><th>Extra</th><th>Ubicación</th></tr></thead><tbody>
      ${u.asistencias.map((a) => `<tr><td>${fecha(a.fecha)}</td><td>${esc(a.horaEntrada)}</td><td>${esc(a.horaSalida || 'En curso')}</td>
        <td>${a.tarde ? chip('Tarde', 'amarillo') : chip('Puntual', 'verde')}</td><td>${a.horaSalida ? formatoHoras(a.minutos) : '—'}</td>
        <td>${a.minutosExtra ? '+' + formatoHoras(a.minutosExtra) + ' · ' + esc(a.estadoExtra) : '—'}</td>
        <td>${Mapa.boton(a.ubicacionEntrada, 'Entrada de ' + u.nombre + ' · ' + fecha(a.fecha))}</td></tr>`).join('')
        || '<tr><td colspan="7" class="vacio">Sin jornadas registradas.</td></tr>'}</tbody></table></div></div>
      <div id="h-ausencias" role="tabpanel" class="oculto">${ausencias.length ? `<ul class="lista-simple">${ausencias.map((a) => `<li><span>${fecha(a.fecha)}</span>
        ${chip(a.tipo, a.tipo === 'Incapacidad' ? 'morado' : a.justificada ? 'azul' : 'rojo')}</li>`).join('')}</ul>`
        : '<p class="suave">Sin días sin marcar en los últimos 30 días.</p>'}</div>
      <div id="h-permisos" role="tabpanel" class="oculto">${tablaNovedades(licencias, false, 'Sin permisos ni reposos.')}</div>
      <div id="h-reportes" role="tabpanel" class="oculto">${tablaNovedades(reportes, false, 'Sin reportes en obra.')}</div>
      <div id="h-herramientas" role="tabpanel" class="oculto">${herramientas.length ? `<ul class="lista-simple">${herramientas.map((h) =>
        `<li><span><strong>${esc(h.nombreArticulo)}</strong> ${h.cantidad > 1 ? '× ' + h.cantidad : ''}
          <span class="pequeno suave">${esc(h.articulo)} · entregado el ${fecha(h.fecha)}</span></span>
          ${h.vencida ? chip('Devolución vencida', 'rojo') : ''}</li>`).join('')}</ul>` : '<p class="suave">No tiene herramientas a su cargo.</p>'}</div>`);
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

async function vistaObras() {
  const zona = contenido();
  const [obras, visibles] = await Promise.all([Api.obras(), Api.listarUsuarios()]);
  const asignables = esGerente() ? visibles : [estado.usuario, ...visibles];
  const sinRevisar = obras.flatMap((o) => o.retrasos.filter((r) => !r.revisado).map((r) => ({ ...r, obra: o })));
  const plazo = (o) => (o.estado === 'PLANIFICADA' ? `Inicio previsto: ${fecha(o.inicio)}`
    : o.estado === 'CONCLUIDA' ? `Concluida el ${fecha(o.conclusion)}` : `${o.avance}% del plazo`);
  zona.innerHTML = encabezado('', 'Obras', '<button class="btn btn-primario" id="nueva-obra">+ Nueva obra</button>') + `
    <p class="suave" style="margin-top:-8px">Cada obra pasa de <strong>Planificada</strong> a <strong>En curso</strong> con el «Inicio de obra»
      y termina con la «Conclusión de la obra». Si la fecha estimada se mueve, el supervisor reporta el retraso y el gerente recibe la alerta.</p>
    ${esGerente() && sinRevisar.length ? `<div class="mensaje mensaje-error" role="alert"><strong>${sinRevisar.length} retraso(s) sin revisar:</strong>
      ${sinRevisar.map((r) => `${esc(r.obra.nombre)} (${r.dias} días, reportado por ${esc(r.reportadoPor)})`).join(' · ')}</div>` : ''}
    <div class="rejilla-obras">${obras.map((o) => `<section class="tarjeta tarjeta-obra estado-${o.estado.toLowerCase()}" aria-labelledby="obra-${esc(o.codigo)}">
      <div class="titulo-vista" style="margin:0 0 4px"><h2 id="obra-${esc(o.codigo)}" style="margin:0">${esc(o.nombre)}</h2>
        <span>${chipEstado(o.estadoNombre)} ${chip(o.codigo, 'gris')}</span></div>
      <p class="suave pequeno" style="margin:0 0 10px">${esc(o.ubicacion || 'Sin ubicación')} · ${fecha(o.inicio)} → ${fecha(o.fin)}
        ${o.inicioReal ? ` · iniciada el ${fecha(o.inicioReal)}` : ''}</p>
      <p class="control-obra-resumen">${o.exigeCodigo ? chip('Código QR', 'azul') : ''}${o.latitud !== null ? chip(`Ubicación · ${o.radio} m`, 'azul') : ''}
        ${!o.exigeCodigo && o.latitud === null ? '<span class="pequeno suave">Sin control de asistencia</span>' : ''}</p>
      <div class="barra-pista" role="img" aria-label="${o.avance}% del plazo transcurrido"><span style="width:${o.avance}%;background:${o.estado === 'CONCLUIDA' ? '#22a06b' : '#0c66e4'}"></span></div>
      <p class="pequeno suave" style="margin:4px 0 12px">${plazo(o)}</p>
      ${o.retrasos.length ? `<details class="retrasos" ${o.retrasos.some((r) => !r.revisado) ? 'open' : ''}><summary>Retrasos (${o.retrasos.length})</summary>
        <ul>${o.retrasos.map((r) => `<li class="${r.revisado ? '' : 'retraso-nuevo'}"><strong>${fecha(r.finAnterior)} → ${fecha(r.finNuevo)}</strong>
          (+${r.dias} días) · ${esc(r.motivo)}<br><span class="pequeno suave">Reportó ${esc(r.reportadoPor)} el ${fecha(r.fecha)}
          ${r.revisado ? ' · revisado por ' + esc(r.revisadoPor) : ''}</span>
          ${!r.revisado && esGerente() ? `<button class="btn btn-secundario btn-chico" data-revisar-retraso="${r.id}" data-obra="${esc(o.codigo)}">Marcar como revisado</button>` : ''}</li>`).join('')}</ul></details>` : ''}
      <h3 class="pequeno" style="margin:0 0 6px">Personas (${o.personas.length})</h3>
      <ul class="lista-personas">${o.personas.map((p) => `<li>${esc(p.nombre)} ${chipRol(p.rol)}
        ${asignables.some((a) => a.cedula === p.cedula) ? `<button class="btn btn-enlace btn-chico" data-quitar="${esc(p.cedula)}" aria-label="Quitar a ${esc(p.nombre)} de la obra">Quitar</button>` : ''}</li>`).join('')
        || '<li class="suave">Nadie asignado todavía.</li>'}</ul>
      <div class="acciones" style="justify-content:flex-start;flex-wrap:wrap">
        ${o.estado !== 'CONCLUIDA' ? `<label class="solo-lector" for="sumar-${esc(o.codigo)}">Sumar persona a ${esc(o.nombre)}</label>
        <select id="sumar-${esc(o.codigo)}" data-sumar="${esc(o.codigo)}" style="width:auto;max-width:100%">
          <option value="">+ Sumar persona…</option>${asignables.filter((a) => !o.personas.some((p) => p.cedula === a.cedula))
            .map((a) => `<option value="${esc(a.cedula)}">${esc(a.nombre)}${a.obra ? ' (en ' + esc(a.obra) + ')' : ''}</option>`).join('')}</select>` : ''}
        ${o.estado === 'PLANIFICADA' ? `<button class="btn btn-exito btn-chico" data-iniciar="${esc(o.codigo)}">Inicio de obra</button>` : ''}
        ${o.estado === 'EN_CURSO' ? `<button class="btn btn-exito btn-chico" data-concluir="${esc(o.codigo)}">Conclusión de la obra</button>` : ''}
        ${o.estado !== 'CONCLUIDA' && esSupervisor() ? `<button class="btn btn-peligro btn-chico" data-retraso="${esc(o.codigo)}">Reportar retraso</button>` : ''}
        ${o.exigeCodigo && o.estado !== 'CONCLUIDA' ? `<a class="btn btn-primario btn-chico" href="#codigo-obra?obra=${encodeURIComponent(o.codigo)}">Mostrar QR de asistencia</a>` : ''}
        <button class="btn btn-secundario btn-chico" data-editar-obra="${esc(o.codigo)}">Editar</button>
        <button class="btn btn-enlace btn-chico" style="color:var(--rojo)" data-eliminar-obra="${esc(o.codigo)}">Eliminar</button>
      </div></section>`).join('') || '<p class="suave">Aún no hay obras. Crea la primera.</p>'}</div>`;
  const recargar = async (mensaje) => { aviso(mensaje); await vistaObras(); };
  const intentar = (accion) => async (evento) => {
    try {
      await accion(evento);
    } catch (e) {
      aviso(e.message, 'error');
    }
  };
  const obraDe = (codigo) => obras.find((o) => o.codigo === codigo);
  $('#nueva-obra').addEventListener('click', () => formularioObra(null));
  zona.querySelectorAll('[data-editar-obra]').forEach((b) => b.addEventListener('click', () => formularioObra(obraDe(b.dataset.editarObra))));
  zona.querySelectorAll('[data-eliminar-obra]').forEach((b) => b.addEventListener('click', intentar(async () => {
    const obra = obraDe(b.dataset.eliminarObra);
    if (!await confirmar('Eliminar obra', `¿Eliminar la obra «${esc(obra.nombre)}»? Esta acción no se puede deshacer.`, 'Eliminar', true)) return;
    await Api.eliminarObra(obra.codigo);
    await recargar('Obra eliminada.');
  })));
  zona.querySelectorAll('[data-iniciar]').forEach((b) => b.addEventListener('click', intentar(async () => {
    const obra = obraDe(b.dataset.iniciar);
    if (!await confirmar('Inicio de obra', `¿Registrar hoy el inicio de «${esc(obra.nombre)}»? Pasará a «En curso».`, 'Iniciar obra')) return;
    await Api.iniciarObra(obra.codigo);
    await recargar(`«${obra.nombre}» está en curso.`);
  })));
  zona.querySelectorAll('[data-concluir]').forEach((b) => b.addEventListener('click', intentar(async () => {
    const obra = obraDe(b.dataset.concluir);
    if (!await confirmar('Conclusión de la obra', `¿Registrar hoy la conclusión de «${esc(obra.nombre)}»? Ya no admitirá personal nuevo.`,
      'Concluir obra')) return;
    await Api.concluirObra(obra.codigo);
    await recargar(`«${obra.nombre}» quedó concluida.`);
  })));
  zona.querySelectorAll('[data-retraso]').forEach((b) => b.addEventListener('click', () => formularioRetraso(obraDe(b.dataset.retraso))));
  zona.querySelectorAll('[data-revisar-retraso]').forEach((b) => b.addEventListener('click', intentar(async () => {
    await Api.revisarRetraso(b.dataset.obra, b.dataset.revisarRetraso);
    await recargar('Retraso marcado como revisado.');
  })));
  zona.querySelectorAll('[data-sumar]').forEach((s) => s.addEventListener('change', intentar(async () => {
    if (!s.value) return;
    const persona = await Api.asignarObra(s.value, s.dataset.sumar);
    await recargar(persona.nombre + ' ahora trabaja en esta obra.');
  })));
  zona.querySelectorAll('[data-quitar]').forEach((b) => b.addEventListener('click', intentar(async () => {
    const persona = await Api.asignarObra(b.dataset.quitar, '');
    await recargar(persona.nombre + ' quedó sin obra asignada.');
  })));
}

function formularioRetraso(obra) {
  const minimo = new Date(new Date(obra.fin + 'T12:00').getTime() + 86400000).toISOString().slice(0, 10);
  const modal = Modal.abrir('Reportar retraso · ' + obra.nombre, `<form novalidate>
    <p class="suave">Fecha estimada de conclusión actual: <strong>${fecha(obra.fin)}</strong>. El gerente recibirá una alerta.</p>
    <div class="campo"><label for="r-motivo">Motivo del retraso *</label>
      <textarea id="r-motivo" name="motivo" maxlength="300" placeholder="Ej.: lluvias inundaron la excavación; falta de cemento"></textarea></div>
    <div class="campo"><label for="r-fecha">Nueva fecha estimada de conclusión *</label>
      <input id="r-fecha" name="nuevaFecha" type="date" min="${minimo}" value="${minimo}"></div>
    <div class="mensaje mensaje-error oculto" role="alert"></div>
    <div class="acciones"><button type="button" class="btn btn-secundario" data-cerrar>Cancelar</button>
      <button class="btn btn-peligro" type="submit">Reportar retraso</button></div></form>`);
  const formulario = modal.querySelector('form');
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    try {
      const retraso = await Api.reportarRetraso(obra.codigo, $('#r-motivo').value, $('#r-fecha').value);
      Modal.cerrar();
      aviso(`Retraso reportado: la obra se movió ${retraso.dias} días. El gerente ya tiene la alerta.`);
      await vistaObras();
    } catch (e) {
      mostrarErrorFormulario(formulario, e.message);
    }
  });
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
    <fieldset class="control-obra"><legend>Control de asistencia</legend>
      <label class="opcion-check"><input type="checkbox" name="exigeCodigo" value="true" ${obra && obra.exigeCodigo ? 'checked' : ''}>
        Exigir el código QR de la obra al marcar</label>
      <p class="ayuda">El supervisor muestra en su teléfono un QR que cambia cada 30 segundos. Quien marca sin él queda por confirmar.</p>
      <div class="fila">
        <div class="campo"><label for="o-lat">Latitud</label><input id="o-lat" name="latitud" inputmode="decimal" value="${obra && obra.latitud !== null ? obra.latitud : ''}" placeholder="10.4806"></div>
        <div class="campo"><label for="o-lon">Longitud</label><input id="o-lon" name="longitud" inputmode="decimal" value="${obra && obra.longitud !== null ? obra.longitud : ''}" placeholder="-66.9036"></div>
        <div class="campo"><label for="o-radio">Radio (m)</label><input id="o-radio" name="radio" type="number" min="20" max="2000" value="${obra && obra.radio ? obra.radio : 150}"></div>
      </div>
      <button type="button" class="btn btn-secundario btn-chico" id="o-mi-ubicacion">Usar mi ubicación actual</button>
      <p class="ayuda">Con la ubicación, las marcas hechas lejos de la obra quedan por confirmar. Déjala vacía para no usarla.</p>
    </fieldset>
    <div class="mensaje mensaje-error oculto" role="alert"></div>
    <div class="acciones"><button type="button" class="btn btn-secundario" data-cerrar>Cancelar</button>
      <button class="btn btn-primario" type="submit">Guardar</button></div></form>`);
  const formulario = modal.querySelector('form');
  modal.querySelector('#o-mi-ubicacion').addEventListener('click', () => {
    if (!navigator.geolocation) {
      mostrarErrorFormulario(formulario, 'Este navegador no puede leer la ubicación.');
      return;
    }
    navigator.geolocation.getCurrentPosition((p) => {
      modal.querySelector('#o-lat').value = p.coords.latitude.toFixed(6);
      modal.querySelector('#o-lon').value = p.coords.longitude.toFixed(6);
    }, () => mostrarErrorFormulario(formulario, 'No se pudo leer tu ubicación. Revisa el permiso del navegador.'),
    { enableHighAccuracy: true, timeout: 12000 });
  });
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
  const zona = contenido();
  const [opciones, lista, config] = await Promise.all([Api.opcionesPersonal(), Api.invitaciones(), Api.configuracionInvitaciones()]);
  const catalogo = await Api.catalogo();
  zona.innerHTML = encabezado('', 'Invitaciones') + `
    <p class="suave" style="margin-top:-8px">Cada código sirve una sola vez. Compártelo por WhatsApp o correo con la persona que quieres sumar.
      Las invitaciones vencidas o canceladas se borran solas; las usadas se muestran 7 días.</p>
    <div class="rejilla-formulario">
      <form class="tarjeta" id="form-invitacion" novalidate aria-labelledby="t-nueva">
        <h2 id="t-nueva">Nueva invitación</h2>
        <div class="campo"><label for="i-rol">Rol</label><select id="i-rol" name="rol"><option>Empleado</option><option>Supervisor</option></select></div>
        <div class="campo" id="i-campo-supervisor"><label for="i-supervisor">Supervisor</label><select id="i-supervisor" name="supervisor">
          <option value="">— Sin supervisor —</option>${opciones.supervisores.map((s) => `<option value="${esc(s.cedula)}">${esc(s.nombre)}</option>`).join('')}</select></div>
        <div class="fila">
          <div class="campo"><label for="i-puesto">Puesto</label><select id="i-puesto" name="puesto">${opcionesLista(catalogo.puestos, null, '— Sin puesto —')}</select></div>
          <div class="campo"><label for="i-area">Área</label><select id="i-area" name="area">${opcionesLista(catalogo.areas, null, '— Sin área —')}</select></div>
        </div>
        <div class="campo"><label for="i-obra">Obra</label><select id="i-obra" name="obra"><option value="">— Sin obra —</option>
          ${opciones.obras.filter((o) => o.admitePersonal).map((o) => `<option value="${esc(o.codigo)}">${esc(o.nombre)}</option>`).join('')}</select></div>
        <div class="campo"><label for="i-dias">Vigencia (días)</label><input id="i-dias" name="dias" type="number" min="1" max="30" value="7"></div>
        <div class="campo"><label for="i-correo">Correo de la persona (opcional)</label><input id="i-correo" name="correo" type="email" placeholder="nombre@correo.com">
          <span class="ayuda">${config.correoReal ? '✉ Le llegará un correo con el código y un botón para unirse.'
            : 'El envío de correos no está configurado: se registra, pero no llega. Comparte el código por WhatsApp.'}</span></div>
        <button class="btn btn-primario btn-bloque" type="submit">Crear invitación</button>
        <div id="codigo-creado" class="oculto" style="margin-top:14px"></div>
      </form>
      <section class="tarjeta" aria-labelledby="t-creadas"><h2 id="t-creadas">Invitaciones creadas</h2>
        <div class="tabla-contenedor"><table><thead><tr><th>Código</th><th>Rol y puesto</th><th>Supervisor</th><th>Obra</th><th>Estado</th><th>Vence</th><th>Usada por</th><th></th></tr></thead>
        <tbody>${lista.map((v) => `<tr><td><strong class="codigo">${esc(v.codigo)}</strong></td><td>${esc(v.rol)}${v.puesto ? `<br><span class="pequeno suave">${esc(v.puesto)}${v.area ? ' · ' + esc(v.area) : ''}</span>` : ''}</td><td>${esc(v.supervisor || '—')}</td>
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
  zona.querySelectorAll('[data-enviar]').forEach((b) => b.addEventListener('click', () => {
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
  zona.querySelectorAll('[data-cancelar]').forEach((b) => b.addEventListener('click', async () => {
    try {
      await Api.cancelarInvitacion(b.dataset.cancelar);
      aviso('Invitación ' + b.dataset.cancelar + ' cancelada y borrada.');
      await vistaInvitaciones();
    } catch (e) {
      aviso(e.message, 'error');
    }
  }));
}
