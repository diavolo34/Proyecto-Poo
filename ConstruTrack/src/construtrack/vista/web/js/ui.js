const esc = (texto) => String(texto ?? '').replace(/[&<>"']/g,
  (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

const $ = (selector, raiz = document) => raiz.querySelector(selector);

const DIAS = { LU: 'lunes', MA: 'martes', MI: 'miércoles', JU: 'jueves', VI: 'viernes', SA: 'sábado', DO: 'domingo' };

function textoDias(dias) {
  const lista = dias.split(',').map((d) => DIAS[d]);
  return lista.length > 1 ? lista.slice(0, -1).join(', ') + ' y ' + lista[lista.length - 1] : lista[0];
}

const fecha = (iso) => (iso ? String(iso).slice(0, 10).split('-').reverse().join('/') : '—');

const fechaCorta = (iso) => (iso ? String(iso).slice(5, 10).split('-').reverse().join('/') : '—');

const formatoHoras = (minutos) => {
  const h = Math.round((minutos || 0) / 6) / 10;
  return (Number.isInteger(h) ? String(h) : h.toFixed(1).replace('.', ',')) + ' h';
};

const iniciales = (nombre) => String(nombre || '?').split(' ').slice(0, 2).map((p) => p[0]).join('').toUpperCase();

const chip = (texto, color) => `<span class="chip chip-${color}">${esc(texto)}</span>`;
const chipRol = (rol) => chip(rol, { Jefe: 'morado', Supervisor: 'azul', Empleado: 'gris' }[rol] || 'gris');
const chipEstado = (estado) => chip(estado, {
  Pendiente: 'gris', 'En revisión': 'azul', Aprobada: 'verde', Rechazada: 'rojo',
  Vigente: 'azul', Usada: 'verde', Cancelada: 'gris', Vencida: 'amarillo',
}[estado] || 'gris');
const chipGravedad = (g) => chip('Gravedad ' + String(g).toLowerCase(), { Alta: 'rojo', Media: 'amarillo', Baja: 'azul' }[g]);

function aviso(texto, tipo = 'ok') {
  let zona = $('#avisos');
  if (!zona) {
    zona = document.createElement('div');
    zona.id = 'avisos';
    zona.setAttribute('aria-live', 'polite');
    document.body.appendChild(zona);
  }
  const elemento = document.createElement('div');
  elemento.className = 'aviso' + (tipo === 'error' ? ' aviso-error' : tipo === 'aviso' ? ' aviso-alerta' : '');
  elemento.setAttribute('role', tipo === 'error' ? 'alert' : 'status');
  elemento.textContent = (tipo === 'ok' ? '✓ ' : '⚠ ') + texto;
  zona.appendChild(elemento);
  setTimeout(() => elemento.remove(), tipo === 'error' ? 6000 : 3500);
}

const Modal = {
  anterior: null,
  alCerrar: null,
  abrir(titulo, html, alCerrar) {
    this.cerrar();
    this.anterior = document.activeElement;
    this.alCerrar = alCerrar || null;
    const fondo = document.createElement('div');
    fondo.className = 'modal-fondo';
    fondo.id = 'modal';
    fondo.innerHTML = `<div class="modal" role="dialog" aria-modal="true" aria-labelledby="modal-titulo">
      <div class="modal-cabecera"><h2 id="modal-titulo">${esc(titulo)}</h2>
      <button type="button" class="btn btn-enlace" data-cerrar aria-label="Cerrar">✕</button></div>${html}</div>`;
    fondo.addEventListener('click', (e) => { if (e.target === fondo || e.target.closest('[data-cerrar]')) this.cerrar(); });
    document.body.appendChild(fondo);
    const primero = fondo.querySelector('input, select, textarea, button:not([data-cerrar])');
    if (primero) primero.focus();
    return fondo;
  },
  cerrar() {
    const fondo = $('#modal');
    const alCerrar = this.alCerrar;
    this.alCerrar = null;
    if (fondo) {
      fondo.remove();
      if (this.anterior) this.anterior.focus();
    }
    if (alCerrar) alCerrar();
  },
  abierto: () => Boolean($('#modal')),
};
document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && Modal.abierto()) Modal.cerrar(); });

const LOGO = `<svg class="logo" viewBox="0 0 32 32" aria-hidden="true"><rect width="32" height="32" rx="7" fill="#0c66e4"/>
  <path d="M8 23V13l8-5 8 5v10h-5v-6h-6v6z" fill="#fff"/><rect x="21" y="6" width="3" height="7" fill="#fca700"/></svg>`;

document.addEventListener('input', (evento) => {
  const campo = evento.target;
  if (campo.matches && campo.matches('[data-solo-numeros]')) {
    const limpio = campo.value.replace(/[^0-9]/g, '').slice(0, 10);
    if (limpio !== campo.value) campo.value = limpio;
  }
});

function datosFormulario(formulario) {
  const datos = {};
  new FormData(formulario).forEach((valor, clave) => {
    datos[clave] = datos[clave] !== undefined ? datos[clave] + ',' + valor : valor;
  });
  return datos;
}
