$('#logo-cabecera').outerHTML = LOGO;

const cabecera = $('#lp-cabecera');
const botonMenu = $('#lp-menu-boton');
function abrirMenu(abrir) {
  cabecera.classList.toggle('lp-menu-abierto', abrir);
  botonMenu.setAttribute('aria-expanded', String(abrir));
  botonMenu.setAttribute('aria-label', abrir ? 'Cerrar el menú' : 'Abrir el menú');
}
botonMenu.addEventListener('click', () => abrirMenu(!cabecera.classList.contains('lp-menu-abierto')));
$('#lp-menu').addEventListener('click', (evento) => { if (evento.target.closest('a')) abrirMenu(false); });
document.addEventListener('keydown', (evento) => {
  if (evento.key === 'Escape' && cabecera.classList.contains('lp-menu-abierto')) {
    abrirMenu(false);
    botonMenu.focus();
  }
});

$('#form-empezar').addEventListener('submit', (evento) => {
  evento.preventDefault();
  const correo = $('#correo-empezar').value.trim();
  location.href = 'app.html#registro' + (correo ? '?correo=' + encodeURIComponent(correo) : '');
});

const cifra = (valor, texto) =>
  `<div class="lp-cifra"><span class="lp-cifra-valor">${esc(valor)}</span><span class="lp-cifra-texto">${esc(texto)}</span></div>`;

(async function cargarDatosEnVivo() {
  const estado = $('#estado-servidor');
  await Api.conectar((intento, segundos) => {
    if (intento === 2) {
      estado.innerHTML = '<span class="girando"></span> Despertando el servidor (puede tardar hasta un minuto)…';
    } else if (intento > 2) {
      estado.innerHTML = `<span class="girando"></span> Despertando el servidor… ${segundos} s`;
    }
  });
  estado.innerHTML = '<span class="chip chip-verde">● Sistema en línea</span>';
  try {
    const datos = await Api.publico();
    const promedio = typeof datos.avancePromedio === 'number' ? datos.avancePromedio
      : Math.round(datos.obras.reduce((s, o) => s + o.avance, 0) / Math.max(1, datos.obras.length));
    const obrasActivas = typeof datos.obrasActivas === 'number' ? datos.obrasActivas : datos.obras.length;
    const cifras = [
      [datos.personal, 'Personas registradas'],
      [datos.presentes, 'En la obra ahora'],
      [obrasActivas, 'Obras activas'],
      [promedio + '%', 'Avance promedio del plazo'],
    ];

    if (typeof datos.empresas === 'number') cifras.unshift([datos.empresas, datos.empresas === 1 ? 'Empresa usa ConstruTrack' : 'Empresas usan ConstruTrack']);
    $('#cifras').innerHTML = datos.empresas === 0
      ? `<div class="lp-primera"><p class="lp-primera-titulo">Sé la primera empresa en usar ConstruTrack</p>
          <p>Crea tu empresa en dos minutos, registra tus obras e invita a tu equipo con un código.</p>
          <a class="btn btn-primario" href="app.html#registro?tipo=gerente">Crear mi empresa</a></div>`
      : cifras.map(([valor, texto]) => cifra(valor, texto)).join('');
    $('#lista-obras').innerHTML = datos.obras.map((o) => `
      <article class="lp-obra">
        <div class="lp-obra-cabeza"><img src="img/icono-obra.svg" width="48" height="48" alt="">
          <div><h4>${esc(o.nombre)}</h4><p>${esc(o.ubicacion)}</p></div></div>
        <div class="barra-avance" role="progressbar" aria-valuenow="${o.avance}" aria-valuemin="0" aria-valuemax="100"
          aria-label="Avance del plazo de ${esc(o.nombre)}"><span style="width:${o.avance}%"></span></div>
        <p class="lp-obra-pie"><strong>${o.avance}%</strong> del plazo · fin estimado ${fecha(o.finEstimado)}</p>
      </article>`).join('')
      || `<p class="lp-sin-obras">Las obras de cada empresa son privadas: solo las ve su equipo.
        <a href="app.html#registro?tipo=gerente">Crea tu empresa</a> y registra la primera.</p>`;
    $('#titulo-obras').hidden = datos.obras.length === 0;
    if (datos.empresas === 0) $('#lista-obras').innerHTML = '';
  } catch (e) {
    $('#lista-obras').innerHTML = `<p>${esc(e.message)}</p>`;
  }
}());

if ('IntersectionObserver' in window && matchMedia('(prefers-reduced-motion: no-preference)').matches) {
  document.documentElement.classList.add('lp-animar');
  const observador = new IntersectionObserver((entradas) => entradas.forEach((entrada) => {
    if (entrada.isIntersecting) {
      entrada.target.classList.add('lp-visible');
      observador.unobserve(entrada.target);
    }
  }), { rootMargin: '0px 0px -8% 0px' });
  document.querySelectorAll('.lp-aparece').forEach((elemento) => observador.observe(elemento));
}

ChatUI.montar({ usuario: () => null, alCambiar: () => {} });
$('#lp-abrir-arqui').addEventListener('click', () => {
  if ($('#chat-panel').classList.contains('oculto')) ChatUI.alternar();
  else $('#chat-texto').focus();
});
