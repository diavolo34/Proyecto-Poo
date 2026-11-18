const estado = { usuario: null, cuenta: null, periodo: 'ultimos7' };

(async function iniciar() {
  const banda = $('#conexion');
  await Api.conectar((intento, segundos) => {
    if (intento >= 2) {
      banda.innerHTML = `<div class="aviso-conexion" role="status"><span class="girando"></span>
        Conectando con el servidor… ${segundos} s. Si está publicado en un plan gratuito, puede tardar hasta un minuto en despertar.</div>`;
    }
  });
  banda.innerHTML = '';
  if (Api.token) {
    try {
      const yo = await Api.yo();
      if (yo.pendiente) estado.cuenta = yo.cuenta;
      else estado.usuario = yo;
    } catch (e) {
      Api.guardarToken(null);
    }
  }
  ChatUI.montar({ usuario: () => estado.usuario, alCambiar: () => vistaActual() && vistaActual().dibujar() });
  window.addEventListener('hashchange', enrutar);
  enrutar();
}());

function ruta() {
  const [nombre, consulta] = location.hash.slice(1).split('?');
  return { nombre: nombre || '', parametros: new URLSearchParams(consulta || '') };
}

const vistaActual = () => VISTAS.find((v) => v.id === ruta().nombre && v.disponible());

function enrutar() {
  const { nombre, parametros } = ruta();
  if (nombre === 'restablecer') {
    pantallaRestablecer(parametros);
    return;
  }
  if (!estado.usuario && !estado.cuenta) {
    if (nombre === 'registro') pantallaRegistro(parametros);
    else if (nombre === 'recuperar') pantallaRecuperar();
    else if (nombre === 'demo') pantallaDemo();
    else pantallaLogin();
    return;
  }
  if (!estado.usuario) {
    pantallaUnirse();
    return;
  }
  const vista = vistaActual() || VISTAS[0];
  if (vista.id !== nombre) history.replaceState(null, '', '#' + vista.id);
  dibujarApp(vista);
}

function dibujarApp(vista) {
  const u = estado.usuario;
  const disponibles = VISTAS.filter((v) => v.disponible());
  const grupos = [...new Set(disponibles.map((v) => v.grupo))];
  $('#raiz').innerHTML = `<a class="saltar" href="#principal">Saltar al contenido</a>
    <div class="app">
      <header class="barra-superior">
        <a class="marca" href="index.html">${LOGO} ConstruTrack</a>
        ${Api.esDemo() ? '<span class="chip chip-amarillo" title="Datos de prueba que se reinician con el servidor">Demo privada</span>' : ''}
        ${u.empresa ? `<span class="empresa-actual ocultar-movil" title="Tu empresa">${esc(u.empresa)}</span>` : ''}
        <span class="espacio"></span>
        <span class="ocultar-movil">${esc(u.nombre)}</span><span class="ocultar-movil">${chipRol(u.rol)}</span>
        <span class="avatar" aria-hidden="true">${esc(iniciales(u.nombre))}</span>
        <button class="btn btn-secundario btn-chico" id="cerrar-sesion" aria-label="Cerrar sesión">Salir</button>
      </header>
      <nav class="lateral" aria-label="Menú principal">
        ${grupos.map((g) => `<div class="titulo-lateral">${esc(g)}</div>` + disponibles.filter((v) => v.grupo === g).map((v) =>
          `<a href="#${v.id}" ${v.id === vista.id ? 'aria-current="page"' : ''}><span aria-hidden="true">${v.icono}</span>${esc(v.id === 'personal' && !esJefe() ? 'Mi equipo' : v.titulo)}</a>`).join('')).join('')}
      </nav>
      <main class="contenido" id="principal" tabindex="-1"><p class="vacio"><span class="girando"></span> Cargando…</p></main>
    </div>`;
  $('#cerrar-sesion').addEventListener('click', cerrarSesion);
  document.title = vista.titulo + ' · ConstruTrack';
  vista.dibujar().catch((e) => {
    if (e.codigo === 401) {
      estado.usuario = null;
      aviso('Tu sesión expiró. Vuelve a entrar.', 'error');
      enrutar();
      return;
    }
    contenido().innerHTML = `<div class="mensaje mensaje-error" role="alert">${esc(e.message)}</div>
      <button class="btn btn-secundario" id="reintentar">Reintentar</button>`;
    $('#reintentar').addEventListener('click', enrutar);
  });
}

async function cerrarSesion() {
  await Api.cerrarSesion();
  estado.usuario = null;
  estado.cuenta = null;
  location.hash = 'login';
  enrutar();
}
