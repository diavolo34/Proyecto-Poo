// Portada pública: formulario de inicio, cifras y obras en vivo, y el asistente.
$('#logo-cabecera').outerHTML = LOGO;

$('#form-empezar').addEventListener('submit', (evento) => {
  evento.preventDefault();
  const correo = $('#correo-empezar').value.trim();
  location.href = 'app.html#registro' + (correo ? '?correo=' + encodeURIComponent(correo) : '');
});

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
    $('#cifras').innerHTML = [
      [datos.personal, 'Personas registradas'],
      [datos.presentes, 'En la obra ahora'],
      [datos.obras.length, 'Obras activas'],
      [Math.round(datos.obras.reduce((s, o) => s + o.avance, 0) / Math.max(1, datos.obras.length)) + '%', 'Avance promedio'],
    ].map(([valor, texto]) => `<div class="cifra"><div class="valor">${esc(valor)}</div><div class="descripcion">${esc(texto)}</div></div>`).join('');
    $('#lista-obras').innerHTML = datos.obras.map((o) => `
      <article class="obra"><h3>${esc(o.nombre)}</h3><p class="suave pequeno" style="margin:0">${esc(o.ubicacion)}</p>
        <div class="barra-avance" role="progressbar" aria-valuenow="${o.avance}" aria-valuemin="0" aria-valuemax="100"
          aria-label="Avance de ${esc(o.nombre)}"><span style="width:${o.avance}%"></span></div>
        <p class="pequeno suave" style="margin:6px 0 0">${o.avance}% del plazo · fin estimado ${fecha(o.finEstimado)}</p></article>`).join('')
      || '<p class="suave">Aún no hay obras registradas.</p>';
  } catch (e) {
    $('#lista-obras').innerHTML = `<p class="suave">${esc(e.message)}</p>`;
  }
}());

ChatUI.montar({ usuario: () => null, alCambiar: () => {} });
