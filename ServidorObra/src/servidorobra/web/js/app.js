// Interfaz de la página: vista pública con inicio de sesión y los paneles de Empleado, Supervisor y Jefe.
const DIAS = [['LU', 'Lun'], ['MA', 'Mar'], ['MI', 'Mié'], ['JU', 'Jue'], ['VI', 'Vie'], ['SA', 'Sáb'], ['DO', 'Dom']];
const USUARIOS_PRUEBA = [
  ['Jefe', '10000001', 'jefe123', 'Carlos Méndez'],
  ['Supervisor', '20000001', 'super123', 'Ana Rodríguez'],
  ['Supervisor', '20000002', 'super123', 'Luis Pérez'],
  ['Empleado', '30000001', 'emp123', 'José García'],
];

const estado = { usuario: null, personal: [], filtro: 'todos', busqueda: '', filtroRol: 'todos' };
const contenedor = document.getElementById('app');

// ---------- Utilidades de formato ----------
const esc = (texto) => String(texto ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const nombreDia = (codigo) => DIAS.find((d) => d[0] === codigo)[1];

function textoDias(dias) {
  const codigos = dias.split(',');
  const posiciones = codigos.map((c) => DIAS.findIndex((d) => d[0] === c));
  const seguidos = codigos.length > 2 && posiciones.every((p, i) => i === 0 || p === posiciones[i - 1] + 1);
  return seguidos ? `${nombreDia(codigos[0])} a ${nombreDia(codigos[codigos.length - 1])}` : codigos.map(nombreDia).join(', ');
}
const textoHorario = (h) => `${textoDias(h.dias)} · ${h.horaEntrada} – ${h.horaSalida}`;
const fechaLegible = (f) => f.split('-').reverse().join('/');

const etiqueta = (texto, color) => `<span class="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-semibold ${color}">${texto}</span>`;
const etiquetaPresencia = (u) => (u.presente
  ? etiqueta('<span class="h-2 w-2 rounded-full bg-green-500"></span>Presente', 'bg-green-100 text-green-800')
  : etiqueta('<span class="h-2 w-2 rounded-full bg-red-500"></span>Ausente', 'bg-red-100 text-red-800'));
const etiquetaPuntualidad = (a) => (a.tarde ? etiqueta('Tarde', 'bg-amber-100 text-amber-800') : etiqueta('Puntual', 'bg-sky-100 text-sky-800'));
const etiquetaRol = (rol) => etiqueta(esc(rol), { Jefe: 'bg-purple-100 text-purple-800', Supervisor: 'bg-blue-100 text-blue-800', Empleado: 'bg-slate-200 text-slate-700' }[rol]);

function entradaDeHoy(u) {
  const a = u.asistenciaHoy;
  if (!a) return `<span class="text-slate-400">${u.trabajaHoy ? 'Sin registro' : 'Libre hoy'}</span>`;
  return `${a.horaEntrada}${a.horaSalida ? ' – ' + a.horaSalida : ''} ${etiquetaPuntualidad(a)}`;
}

// ---------- Avisos y ventana modal ----------
function aviso(mensaje, tipo = 'ok') {
  const caja = document.createElement('div');
  caja.className = `rounded-lg px-4 py-3 text-sm font-medium text-white shadow-lg ${tipo === 'error' ? 'bg-red-600' : 'bg-green-600'}`;
  caja.textContent = mensaje;
  document.getElementById('avisos').appendChild(caja);
  setTimeout(() => caja.remove(), 4000);
}

function abrirModal(titulo, html) {
  document.getElementById('modal-titulo').textContent = titulo;
  document.getElementById('modal-cuerpo').innerHTML = html;
  document.getElementById('modal').classList.remove('hidden');
}
const cerrarModal = () => document.getElementById('modal').classList.add('hidden');
const modalAbierto = () => !document.getElementById('modal').classList.contains('hidden');

// ---------- Barra superior ----------
function actualizarBarra() {
  const modo = document.getElementById('modo');
  modo.innerHTML = Api.modo === 'java'
    ? etiqueta('● Servidor Java conectado', 'bg-green-500/20 text-green-100')
    : etiqueta('● Modo demostración', 'bg-amber-400/20 text-amber-100');
  const zona = document.getElementById('zona-usuario');
  const u = estado.usuario;
  zona.innerHTML = u
    ? `<div class="text-right leading-tight"><div class="text-sm font-semibold">${esc(u.nombre)}</div><div class="text-xs text-blue-200">${esc(u.rol)} · C.I. ${esc(u.cedula)}</div></div>
       <button data-accion="salir" class="rounded bg-white/10 px-3 py-1.5 text-sm hover:bg-white/20">Cerrar sesión</button>`
    : '';
  document.getElementById('aviso-demo').classList.toggle('hidden', Api.modo !== 'demo');
}

// ---------- Vista pública (sin sesión) ----------
function vistaPublica() {
  return `
  <div class="mx-auto grid w-full max-w-6xl gap-8 lg:grid-cols-5">
    <section class="lg:col-span-3">
      <p class="text-sm font-semibold uppercase tracking-wide text-blue-700">Gestión de obra</p>
      <h2 class="mt-2 text-3xl font-bold text-slate-900 sm:text-4xl">Controla tu personal en obra, en tiempo real.</h2>
      <p class="mt-4 text-slate-600">ConstruTrack es una empresa de construcción que gestiona a sus trabajadores en cada obra:
        registro de empleados, horarios, control de asistencia y supervisión por roles. Nuestro asistente virtual responde
        dudas de Recursos Humanos las 24 horas.</p>
      <div class="mt-6 grid gap-4 sm:grid-cols-3">
        ${[['🏗️', 'Obras civiles', 'Edificaciones, vialidad y remodelaciones.'],
           ['🕒', 'Control de asistencia', 'Entrada y salida de cada trabajador con su horario.'],
           ['👷', 'Supervisión', 'Supervisores y jefes ven quién está en la obra.']]
          .map(([icono, titulo, texto]) => `<div class="rounded-xl bg-white p-4 shadow"><div class="text-2xl">${icono}</div>
            <h3 class="mt-2 font-semibold">${titulo}</h3><p class="text-sm text-slate-600">${texto}</p></div>`).join('')}
      </div>
      <div class="mt-6 rounded-xl bg-white p-4 shadow">
        <h3 class="font-semibold">Usuarios de prueba</h3>
        <p class="text-sm text-slate-500">Haz clic en uno para llenar el formulario.</p>
        <div class="mt-2 overflow-x-auto"><table class="w-full text-left text-sm">
          <thead class="text-slate-500"><tr><th class="py-1 pr-3">Rol</th><th class="pr-3">Nombre</th><th class="pr-3">Cédula</th><th>Clave</th></tr></thead>
          <tbody>${USUARIOS_PRUEBA.map(([rol, cedula, clave, nombre]) => `
            <tr data-accion="usar-prueba" data-cedula="${cedula}" data-clave="${clave}" class="cursor-pointer border-t hover:bg-blue-50">
              <td class="py-1.5 pr-3">${etiquetaRol(rol)}</td><td class="pr-3">${nombre}</td><td class="pr-3 font-mono">${cedula}</td><td class="font-mono">${clave}</td></tr>`).join('')}
          </tbody></table></div>
      </div>
    </section>
    <section class="lg:col-span-2">
      <form id="form-login" class="rounded-xl bg-white p-6 shadow-lg">
        <h2 class="text-xl font-bold">Iniciar sesión</h2>
        <p class="mb-4 text-sm text-slate-500">Ingresa con tu cédula y la clave que te asignó el Jefe.</p>
        <label class="block text-sm font-medium">Cédula
          <input name="cedula" inputmode="numeric" autocomplete="username" required class="mt-1 w-full rounded border px-3 py-2" placeholder="Ej: 30000001"></label>
        <label class="mt-3 block text-sm font-medium">Clave
          <input name="clave" type="password" autocomplete="current-password" required class="mt-1 w-full rounded border px-3 py-2"></label>
        <p id="error-login" class="mt-3 hidden text-sm text-red-600"></p>
        <button class="mt-4 w-full rounded bg-blue-700 py-2.5 font-bold text-white hover:bg-blue-800">Entrar</button>
      </form>
    </section>
  </div>`;
}

// ---------- Piezas compartidas de los paneles ----------
function tarjetaJornada(u) {
  const hoy = u.asistenciaHoy;
  return `
  <section class="rounded-xl bg-white p-5 shadow">
    <div class="flex items-center justify-between gap-2"><h3 class="text-lg font-bold">Mi jornada de hoy</h3>${etiquetaPresencia(u)}</div>
    <p class="mt-2 text-sm text-slate-600">Horario: ${esc(textoHorario(u.horario))}${u.trabajaHoy ? '' : ' · <b>hoy no es día laborable</b>'}</p>
    <p class="mt-1 text-sm text-slate-600">${hoy
      ? `Entrada: <b>${hoy.horaEntrada}</b>${hoy.horaSalida ? ` · Salida: <b>${hoy.horaSalida}</b>` : ''} ${etiquetaPuntualidad(hoy)}`
      : 'Aún no registras tu entrada hoy.'}</p>
    <div class="mt-4 grid grid-cols-2 gap-3">
      <button data-accion="entrada" ${u.presente ? 'disabled' : ''} class="rounded bg-green-600 py-2.5 font-bold text-white hover:bg-green-700 disabled:cursor-not-allowed disabled:bg-slate-300">Marcar entrada</button>
      <button data-accion="salida" ${u.presente ? '' : 'disabled'} class="rounded bg-orange-600 py-2.5 font-bold text-white hover:bg-orange-700 disabled:cursor-not-allowed disabled:bg-slate-300">Marcar salida</button>
    </div>
  </section>`;
}

function tablaHistorial(asistencias) {
  if (!asistencias || !asistencias.length) return '<p class="text-sm text-slate-500">Sin registros de asistencia.</p>';
  return `<div class="overflow-x-auto"><table class="w-full text-left text-sm">
    <thead class="bg-slate-50 text-slate-500"><tr><th class="px-3 py-2">Fecha</th><th class="px-3">Entrada</th><th class="px-3">Salida</th><th class="px-3">Puntualidad</th></tr></thead>
    <tbody>${asistencias.map((a) => `<tr class="border-t"><td class="px-3 py-2">${fechaLegible(a.fecha)}</td><td class="px-3">${a.horaEntrada}</td>
      <td class="px-3">${a.horaSalida || '<span class="text-green-700">En la obra</span>'}</td><td class="px-3">${etiquetaPuntualidad(a)}</td></tr>`).join('')}</tbody>
  </table></div>`;
}

function tarjetaDato(titulo, valor, color) {
  return `<div class="rounded-xl bg-white p-4 shadow"><div class="text-sm text-slate-500">${titulo}</div><div class="text-3xl font-bold ${color}">${valor}</div></div>`;
}

function encabezado(titulo, subtitulo) {
  return `<div class="mb-6 flex flex-wrap items-end justify-between gap-3"><div><h2 class="text-2xl font-bold">${titulo}</h2>
    <p class="text-slate-500">${subtitulo}</p></div>
    <button data-accion="refrescar" class="rounded border bg-white px-3 py-1.5 text-sm hover:bg-slate-50">↻ Actualizar</button></div>`;
}

// ---------- Panel del Empleado ----------
function panelEmpleado(u) {
  return `<div class="mx-auto w-full max-w-4xl">
    ${encabezado('Panel del Trabajador', `Hola, ${esc(u.nombre.split(' ')[0])}. Registra aquí tu entrada y salida de la obra.`)}
    <div class="grid gap-6 md:grid-cols-2">
      ${tarjetaJornada(u)}
      <section class="rounded-xl bg-white p-5 shadow">
        <h3 class="text-lg font-bold">Mis datos</h3>
        <dl class="mt-3 grid grid-cols-3 gap-y-2 text-sm">
          <dt class="text-slate-500">Nombre</dt><dd class="col-span-2">${esc(u.nombre)}</dd>
          <dt class="text-slate-500">Cédula</dt><dd class="col-span-2">${esc(u.cedula)}</dd>
          <dt class="text-slate-500">Rol</dt><dd class="col-span-2">${etiquetaRol(u.rol)}</dd>
          <dt class="text-slate-500">Horario</dt><dd class="col-span-2">${esc(textoHorario(u.horario))}</dd>
          <dt class="text-slate-500">Supervisor</dt><dd class="col-span-2">${u.supervisor ? esc(u.supervisor.nombre) : 'Sin asignar'}</dd>
        </dl>
      </section>
    </div>
    <section class="mt-6 rounded-xl bg-white p-5 shadow"><h3 class="mb-3 text-lg font-bold">Mi historial de asistencia</h3>${tablaHistorial(u.asistencias)}</section>
  </div>`;
}

// ---------- Panel del Supervisor ----------
function panelSupervisor(u) {
  const empleados = estado.personal;
  const presentes = empleados.filter((e) => e.presente).length;
  const tarde = empleados.filter((e) => e.asistenciaHoy && e.asistenciaHoy.tarde).length;
  const visibles = empleados.filter((e) => estado.filtro === 'todos' || (estado.filtro === 'presentes') === e.presente);
  const boton = (valor, texto) => `<button data-accion="filtro" data-valor="${valor}" class="rounded-full px-3 py-1 text-sm ${estado.filtro === valor ? 'bg-blue-700 text-white' : 'bg-white border hover:bg-slate-50'}">${texto}</button>`;

  return `<div class="mx-auto w-full max-w-6xl">
    ${encabezado('Panel de Supervisión', `Hola, ${esc(u.nombre.split(' ')[0])}. Estos son los empleados que supervisas.`)}
    <div class="grid gap-6 lg:grid-cols-3">
      <div>${tarjetaJornada(u)}</div>
      <div class="grid grid-cols-3 gap-4 self-start lg:col-span-2">
        ${tarjetaDato('A cargo', empleados.length, 'text-slate-900')}
        ${tarjetaDato('Presentes', presentes, 'text-green-600')}
        ${tarjetaDato('Llegaron tarde', tarde, 'text-amber-600')}
      </div>
    </div>
    <section class="mt-6 rounded-xl bg-white p-5 shadow">
      <div class="mb-3 flex flex-wrap items-center justify-between gap-3"><h3 class="text-lg font-bold">Empleados a cargo</h3>
        <div class="flex gap-2">${boton('todos', 'Todos')}${boton('presentes', 'Presentes')}${boton('ausentes', 'Ausentes')}</div></div>
      ${visibles.length ? `<div class="overflow-x-auto"><table class="w-full text-left text-sm">
        <thead class="bg-slate-50 text-slate-500"><tr><th class="px-3 py-2">Empleado</th><th class="px-3">Horario</th><th class="px-3">Estado</th><th class="px-3">Entrada de hoy</th><th class="px-3"></th></tr></thead>
        <tbody>${visibles.map((e) => `<tr class="border-t">
          <td class="px-3 py-2"><div class="font-medium">${esc(e.nombre)}</div><div class="text-xs text-slate-500">C.I. ${esc(e.cedula)}</div></td>
          <td class="px-3">${esc(textoHorario(e.horario))}</td><td class="px-3">${etiquetaPresencia(e)}</td><td class="px-3">${entradaDeHoy(e)}</td>
          <td class="px-3 text-right"><button data-accion="historial" data-cedula="${esc(e.cedula)}" class="text-blue-700 hover:underline">Ver historial</button></td></tr>`).join('')}
        </tbody></table></div>` : '<p class="text-sm text-slate-500">No hay empleados en este filtro.</p>'}
    </section>
  </div>`;
}

// ---------- Panel del Jefe ----------
function panelJefe(u) {
  const personal = estado.personal;
  const texto = estado.busqueda.toLowerCase();
  const visibles = personal.filter((p) => (estado.filtroRol === 'todos' || p.rol === estado.filtroRol)
    && (!texto || p.nombre.toLowerCase().includes(texto) || p.cedula.includes(texto)));

  return `<div class="mx-auto w-full max-w-6xl">
    ${encabezado('Dashboard Gerencial', `Hola, ${esc(u.nombre.split(' ')[0])}. Gestiona a los empleados y supervisores de la obra.`)}
    <div class="grid gap-6 lg:grid-cols-3">
      <div>${tarjetaJornada(u)}</div>
      <div class="grid grid-cols-2 gap-4 self-start lg:col-span-2 sm:grid-cols-4">
        ${tarjetaDato('Personal', personal.length, 'text-slate-900')}
        ${tarjetaDato('Supervisores', personal.filter((p) => p.rol === 'Supervisor').length, 'text-blue-700')}
        ${tarjetaDato('Empleados', personal.filter((p) => p.rol === 'Empleado').length, 'text-slate-700')}
        ${tarjetaDato('Presentes', personal.filter((p) => p.presente).length, 'text-green-600')}
      </div>
    </div>
    <section class="mt-6 rounded-xl bg-white p-5 shadow">
      <div class="mb-3 flex flex-wrap items-center gap-3">
        <h3 class="mr-auto text-lg font-bold">Personal registrado</h3>
        <input id="busqueda" value="${esc(estado.busqueda)}" placeholder="Buscar por nombre o cédula" class="w-full rounded border px-3 py-1.5 text-sm sm:w-56">
        <select id="filtro-rol" class="rounded border px-2 py-1.5 text-sm">
          ${['todos', 'Empleado', 'Supervisor', 'Jefe'].map((r) => `<option value="${r}" ${estado.filtroRol === r ? 'selected' : ''}>${r === 'todos' ? 'Todos los roles' : r}</option>`).join('')}
        </select>
        <button data-accion="nuevo" class="rounded bg-blue-700 px-4 py-1.5 text-sm font-bold text-white hover:bg-blue-800">+ Registrar usuario</button>
      </div>
      <div class="overflow-x-auto"><table class="w-full text-left text-sm">
        <thead class="bg-slate-50 text-slate-500"><tr><th class="px-3 py-2">Nombre</th><th class="px-3">Rol</th><th class="px-3">Horario</th><th class="px-3">Supervisor</th><th class="px-3">Estado</th><th class="px-3">Entrada de hoy</th><th class="px-3 text-right">Acciones</th></tr></thead>
        <tbody>${visibles.map((p) => `<tr class="border-t">
          <td class="px-3 py-2"><div class="font-medium">${esc(p.nombre)}</div><div class="text-xs text-slate-500">C.I. ${esc(p.cedula)}</div></td>
          <td class="px-3">${etiquetaRol(p.rol)}${p.rol === 'Supervisor' ? `<div class="text-xs text-slate-500">${p.empleadosACargo} a cargo</div>` : ''}</td>
          <td class="px-3">${esc(textoHorario(p.horario))}</td>
          <td class="px-3">${p.rol === 'Empleado' ? (p.supervisor ? esc(p.supervisor.nombre) : '<span class="text-slate-400">Sin asignar</span>') : '—'}</td>
          <td class="px-3">${etiquetaPresencia(p)}</td><td class="px-3">${entradaDeHoy(p)}</td>
          <td class="whitespace-nowrap px-3 text-right">
            <button data-accion="historial" data-cedula="${esc(p.cedula)}" class="text-blue-700 hover:underline">Historial</button>
            ${p.rol === 'Jefe' ? '' : `<button data-accion="editar" data-cedula="${esc(p.cedula)}" class="ml-2 text-blue-700 hover:underline">Editar</button>
            <button data-accion="eliminar" data-cedula="${esc(p.cedula)}" class="ml-2 text-red-600 hover:underline">Eliminar</button>`}
          </td></tr>`).join('') || '<tr><td colspan="7" class="px-3 py-4 text-slate-500">No hay resultados.</td></tr>'}
        </tbody></table></div>
    </section>
  </div>`;
}

// ---------- Formulario de registro / edición (Jefe) ----------
function abrirFormulario(usuario) {
  const editando = Boolean(usuario);
  const h = usuario ? usuario.horario : { dias: 'LU,MA,MI,JU,VI', horaEntrada: '07:00', horaSalida: '16:00' };
  const supervisores = estado.personal.filter((p) => p.rol === 'Supervisor' && (!usuario || p.cedula !== usuario.cedula));
  const rol = usuario ? usuario.rol : 'Empleado';
  const supervisorActual = usuario && usuario.supervisor ? usuario.supervisor.cedula : '';

  abrirModal(editando ? `Editar a ${usuario.nombre}` : 'Registrar usuario', `
    <form id="form-usuario" data-cedula="${editando ? esc(usuario.cedula) : ''}" class="grid gap-3 sm:grid-cols-2">
      <label class="text-sm font-medium sm:col-span-2">Nombre completo
        <input name="nombre" required value="${editando ? esc(usuario.nombre) : ''}" class="mt-1 w-full rounded border px-3 py-2"></label>
      <label class="text-sm font-medium">Cédula
        <input name="cedula" required inputmode="numeric" pattern="\\d{6,9}" title="Entre 6 y 9 dígitos" ${editando ? `value="${esc(usuario.cedula)}" disabled` : ''} class="mt-1 w-full rounded border px-3 py-2 disabled:bg-slate-100"></label>
      <label class="text-sm font-medium">Clave ${editando ? '<span class="font-normal text-slate-500">(vacía = no cambiar)</span>' : ''}
        <input name="clave" type="text" ${editando ? '' : 'required'} autocomplete="off" class="mt-1 w-full rounded border px-3 py-2"></label>
      <label class="text-sm font-medium">Rol en la empresa
        <select name="rol" class="mt-1 w-full rounded border px-3 py-2">
          ${['Empleado', 'Supervisor'].map((r) => `<option ${r === rol ? 'selected' : ''}>${r}</option>`).join('')}
        </select></label>
      <label id="campo-supervisor" class="text-sm font-medium ${rol === 'Empleado' ? '' : 'hidden'}">Supervisor asignado
        <select name="supervisor" class="mt-1 w-full rounded border px-3 py-2">
          <option value="">Sin asignar</option>
          ${supervisores.map((s) => `<option value="${esc(s.cedula)}" ${s.cedula === supervisorActual ? 'selected' : ''}>${esc(s.nombre)}</option>`).join('')}
        </select></label>
      <fieldset class="text-sm font-medium sm:col-span-2"><legend>Días de trabajo</legend>
        <div class="mt-1 flex flex-wrap gap-2">${DIAS.map(([codigo, nombre]) => `
          <label class="flex items-center gap-1 rounded border px-2 py-1 font-normal"><input type="checkbox" name="dias" value="${codigo}" ${h.dias.split(',').includes(codigo) ? 'checked' : ''}>${nombre}</label>`).join('')}
        </div></fieldset>
      <label class="text-sm font-medium">Hora de entrada<input name="horaEntrada" type="time" required value="${h.horaEntrada}" class="mt-1 w-full rounded border px-3 py-2"></label>
      <label class="text-sm font-medium">Hora de salida<input name="horaSalida" type="time" required value="${h.horaSalida}" class="mt-1 w-full rounded border px-3 py-2"></label>
      <label class="flex items-center gap-2 text-sm font-medium sm:col-span-2"><input type="checkbox" name="presente" ${usuario && usuario.presente ? 'checked' : ''}>
        Presente en la empresa ahora <span class="font-normal text-slate-500">(registra la entrada o salida con la hora actual)</span></label>
      <p id="error-formulario" class="hidden text-sm text-red-600 sm:col-span-2"></p>
      <div class="flex justify-end gap-2 sm:col-span-2">
        <button type="button" data-accion="cerrar-modal" class="rounded border px-4 py-2">Cancelar</button>
        <button class="rounded bg-blue-700 px-4 py-2 font-bold text-white hover:bg-blue-800">${editando ? 'Guardar cambios' : 'Registrar'}</button>
      </div>
    </form>`);
}

async function guardarFormulario(form) {
  const f = new FormData(form);
  const cedula = form.dataset.cedula;
  const datos = {
    nombre: f.get('nombre'),
    rol: f.get('rol'),
    dias: f.getAll('dias').join(','),
    horaEntrada: f.get('horaEntrada'),
    horaSalida: f.get('horaSalida'),
    presente: f.get('presente') === 'on',
  };
  if (datos.rol === 'Empleado') datos.supervisor = f.get('supervisor');
  if (f.get('clave')) datos.clave = f.get('clave');
  if (!cedula) datos.cedula = f.get('cedula').trim();
  try {
    if (cedula) await Api.modificarUsuario(cedula, datos);
    else await Api.registrarUsuario(datos);
    cerrarModal();
    aviso(cedula ? 'Cambios guardados.' : `${datos.nombre} fue registrado.`);
    await recargar();
  } catch (e) {
    const error = document.getElementById('error-formulario');
    error.textContent = e.message;
    error.classList.remove('hidden');
  }
}

async function mostrarHistorial(cedula) {
  try {
    const u = await Api.verUsuario(cedula);
    abrirModal(`Asistencia de ${u.nombre}`, `
      <p class="mb-1 text-sm text-slate-600">${etiquetaRol(u.rol)} · C.I. ${esc(u.cedula)} · ${etiquetaPresencia(u)}</p>
      <p class="mb-4 text-sm text-slate-600">Horario: ${esc(textoHorario(u.horario))}</p>${tablaHistorial(u.asistencias)}`);
  } catch (e) {
    aviso(e.message, 'error');
  }
}

function confirmarEliminacion(cedula) {
  const u = estado.personal.find((p) => p.cedula === cedula);
  abrirModal('Eliminar usuario', `
    <p>¿Seguro que deseas eliminar a <b>${esc(u.nombre)}</b> (${esc(u.rol)}, C.I. ${esc(u.cedula)})? Esta acción no se puede deshacer.</p>
    ${u.rol === 'Supervisor' && u.empleadosACargo ? `<p class="mt-2 text-sm text-amber-700">Sus ${u.empleadosACargo} empleados quedarán sin supervisor.</p>` : ''}
    <div class="mt-5 flex justify-end gap-2"><button data-accion="cerrar-modal" class="rounded border px-4 py-2">Cancelar</button>
      <button data-accion="confirmar-eliminar" data-cedula="${esc(cedula)}" class="rounded bg-red-600 px-4 py-2 font-bold text-white hover:bg-red-700">Eliminar</button></div>`);
}

// ---------- Dibujo y recarga ----------
function dibujar() {
  actualizarBarra();
  const u = estado.usuario;
  if (!u) contenedor.innerHTML = vistaPublica();
  else if (u.rol === 'Jefe') contenedor.innerHTML = panelJefe(u);
  else if (u.rol === 'Supervisor') contenedor.innerHTML = panelSupervisor(u);
  else contenedor.innerHTML = panelEmpleado(u);
  ChatUI.actualizarSugerencias();
}

/** Vuelve a pedir los datos al servidor (o al modo demostración) y redibuja. */
async function recargar() {
  if (estado.usuario) {
    try {
      estado.usuario = await Api.yo();
      estado.personal = estado.usuario.rol === 'Empleado' ? [] : await Api.listarUsuarios();
    } catch (e) {
      if (e.codigo === 401) estado.usuario = null;
      aviso(e.message, 'error');
    }
  }
  dibujar();
}

async function marcar(tipo) {
  try {
    const respuesta = tipo === 'entrada' ? await Api.marcarEntrada() : await Api.marcarSalida();
    aviso(respuesta.mensaje);
    await recargar();
  } catch (e) {
    aviso(e.message, 'error');
  }
}

// ---------- Eventos ----------
document.addEventListener('click', async (evento) => {
  const elemento = evento.target.closest('[data-accion]');
  if (!elemento) return;
  const { accion, cedula } = elemento.dataset;
  switch (accion) {
    case 'entrada': case 'salida': return marcar(accion);
    case 'refrescar': return recargar();
    case 'filtro': estado.filtro = elemento.dataset.valor; return dibujar();
    case 'nuevo': return abrirFormulario(null);
    case 'editar': return abrirFormulario(estado.personal.find((p) => p.cedula === cedula));
    case 'historial': return mostrarHistorial(cedula);
    case 'eliminar': return confirmarEliminacion(cedula);
    case 'confirmar-eliminar':
      try {
        await Api.eliminarUsuario(cedula);
        cerrarModal();
        aviso('Usuario eliminado.');
        await recargar();
      } catch (e) { aviso(e.message, 'error'); }
      return undefined;
    case 'cerrar-modal': return cerrarModal();
    case 'usar-prueba': {
      const form = document.getElementById('form-login');
      form.cedula.value = elemento.dataset.cedula;
      form.clave.value = elemento.dataset.clave;
      return form.clave.focus();
    }
    case 'salir':
      await Api.cerrarSesion();
      Object.assign(estado, { usuario: null, personal: [], filtro: 'todos', busqueda: '', filtroRol: 'todos' });
      return dibujar();
    case 'reiniciar-demo':
      ServidorLocal.reiniciar();
      aviso('Datos de prueba restablecidos.');
      return recargar();
    default: return undefined;
  }
});

document.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const form = evento.target;
  if (form.id === 'form-usuario') return guardarFormulario(form);
  if (form.id === 'form-login') {
    try {
      estado.usuario = await Api.iniciarSesion(form.cedula.value.trim(), form.clave.value);
      await recargar();
    } catch (e) {
      const error = document.getElementById('error-login');
      error.textContent = e.message;
      error.classList.remove('hidden');
    }
  }
  return undefined;
});

document.addEventListener('input', (evento) => {
  if (evento.target.id === 'busqueda') {
    estado.busqueda = evento.target.value;
    dibujar();
    const campo = document.getElementById('busqueda');
    campo.focus();
    campo.setSelectionRange(campo.value.length, campo.value.length);
  }
});

document.addEventListener('change', (evento) => {
  if (evento.target.id === 'filtro-rol') {
    estado.filtroRol = evento.target.value;
    dibujar();
  }
  if (evento.target.name === 'rol' && evento.target.form && evento.target.form.id === 'form-usuario') {
    document.getElementById('campo-supervisor').classList.toggle('hidden', evento.target.value !== 'Empleado');
  }
});

document.addEventListener('keydown', (evento) => {
  if (evento.key === 'Escape' && modalAbierto()) cerrarModal();
});

// ---------- Ventana del chatbot ----------
const ChatUI = {
  bot: new Chatbot({
    usuario: () => estado.usuario,
    alCambiarUsuario: async () => recargar(),
  }),

  alternar() {
    document.getElementById('chatbot-window').classList.toggle('hidden');
    document.getElementById('chat-input').focus();
  },

  agregar(texto, deUsuario) {
    const burbuja = document.createElement('div');
    burbuja.className = `max-w-[85%] whitespace-pre-line rounded-lg p-2 text-sm ${deUsuario ? 'self-end bg-blue-600 text-white' : 'self-start bg-gray-200'}`;
    burbuja.textContent = texto;
    const caja = document.getElementById('chat-messages');
    caja.appendChild(burbuja);
    caja.scrollTop = caja.scrollHeight;
  },

  async enviar(texto) {
    const input = document.getElementById('chat-input');
    const mensaje = (texto ?? input.value).trim();
    if (!mensaje) return;
    input.value = '';
    this.agregar(mensaje, true);
    const respuesta = await this.bot.responder(mensaje);
    setTimeout(() => this.agregar(respuesta, false), 300);
  },

  actualizarSugerencias() {
    document.getElementById('chat-sugerencias').innerHTML = this.bot.sugerencias()
      .map((s) => `<button type="button" class="rounded-full border border-blue-300 bg-white px-2 py-0.5 text-xs text-blue-700 hover:bg-blue-50">${esc(s)}</button>`).join('');
  },
};

document.getElementById('chat-abrir').addEventListener('click', () => ChatUI.alternar());
document.getElementById('chat-cerrar').addEventListener('click', () => ChatUI.alternar());
document.getElementById('chat-form').addEventListener('submit', (evento) => { evento.preventDefault(); evento.stopPropagation(); ChatUI.enviar(); });
document.getElementById('chat-sugerencias').addEventListener('click', (evento) => {
  if (evento.target.tagName === 'BUTTON') ChatUI.enviar(evento.target.textContent);
});

// ---------- Inicio ----------
(async function iniciar() {
  await Api.iniciar();
  if (Api.token) {
    try { estado.usuario = await Api.yo(); } catch (e) { Api.guardarToken(null); }
  }
  await recargar();
  // Supervisores y jefes ven la asistencia actualizada cada 30 segundos
  setInterval(() => {
    const escribiendo = ['INPUT', 'SELECT', 'TEXTAREA'].includes(document.activeElement.tagName);
    if (estado.usuario && estado.usuario.rol !== 'Empleado' && !modalAbierto() && !escribiendo) recargar();
  }, 30000);
}());
