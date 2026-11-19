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
const chipRol = (rol) => chip(rol, { Gerente: 'morado', Supervisor: 'azul', Empleado: 'gris' }[rol] || 'gris');
const chipEstado = (estado) => chip(estado, {
  Pendiente: 'amarillo', Aprobada: 'verde', Rechazada: 'rojo',
  Vigente: 'azul', Usado: 'verde', Cancelado: 'gris', Vencido: 'amarillo',
  Planificado: 'gris', 'En curso': 'azul', Concluido: 'verde',
  Operativo: 'verde', 'En mantenimiento': 'amarillo', 'De baja': 'gris',
}[estado] || 'gris');

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

const OJO = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5C6.5 5 2.7 9.1 1.5 12c1.2 2.9 5 7 10.5 7s9.3-4.1 10.5-7C21.3 9.1 17.5 5 12 5zm0 11.5a4.5 4.5 0 1 1 0-9 4.5 4.5 0 0 1 0 9zm0-2.3a2.2 2.2 0 1 0 0-4.4 2.2 2.2 0 0 0 0 4.4z"/></svg>';
const OJO_TACHADO = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3.3 2 2 3.3l3.2 3.2C3.3 7.9 2.1 9.9 1.5 12c1.2 2.9 5 7 10.5 7 1.9 0 3.6-.5 5.1-1.3l3.6 3.6 1.3-1.3L3.3 2zM12 16.5A4.5 4.5 0 0 1 7.6 11l1.8 1.8a2.2 2.2 0 0 0 2.8 2.8l1.8 1.8c-.6.1-1.3.1-2 .1zM12 5c5.5 0 9.3 4.1 10.5 7-.5 1.3-1.5 2.8-2.9 4.1l-3.3-3.3A4.5 4.5 0 0 0 11.2 7.7L8.7 5.3C9.7 5.1 10.8 5 12 5z"/></svg>';

function ojoDeClave(campo) {
  campo.dataset.ojo = '1';
  const envoltura = document.createElement('div');
  envoltura.className = 'campo-clave';
  campo.parentNode.insertBefore(envoltura, campo);
  envoltura.appendChild(campo);
  const boton = document.createElement('button');
  boton.type = 'button';
  boton.className = 'ver-clave';
  boton.setAttribute('aria-label', 'Mostrar la clave');
  boton.setAttribute('aria-pressed', 'false');
  boton.title = 'Mostrar la clave';
  boton.innerHTML = OJO;
  boton.addEventListener('click', () => {
    const mostrar = campo.type === 'password';
    campo.type = mostrar ? 'text' : 'password';
    boton.innerHTML = mostrar ? OJO_TACHADO : OJO;
    boton.setAttribute('aria-pressed', String(mostrar));
    boton.setAttribute('aria-label', mostrar ? 'Ocultar la clave' : 'Mostrar la clave');
    boton.title = boton.getAttribute('aria-label');
    campo.focus();
  });
  envoltura.appendChild(boton);
}

new MutationObserver(() => {
  document.querySelectorAll('input[type=password]:not([data-ojo])').forEach(ojoDeClave);
}).observe(document.documentElement, { childList: true, subtree: true });

const Adjuntos = {
  ACEPTA: 'image/jpeg,image/png,image/webp,application/pdf',
  MAXIMO: 5 * 1024 * 1024,
  LADO_MAXIMO: 1800,

  aBase64(blob) {
    return new Promise((resolver, rechazar) => {
      const lector = new FileReader();
      lector.onload = () => resolver(String(lector.result));
      lector.onerror = () => rechazar(new Error('No se pudo leer el archivo.'));
      lector.readAsDataURL(blob);
    });
  },

  async comprimir(archivo) {
    const imagen = await createImageBitmap(archivo);
    const escala = Math.min(1, this.LADO_MAXIMO / Math.max(imagen.width, imagen.height));
    const lienzo = document.createElement('canvas');
    lienzo.width = Math.round(imagen.width * escala);
    lienzo.height = Math.round(imagen.height * escala);
    const contexto = lienzo.getContext('2d');
    contexto.fillStyle = '#fff';
    contexto.fillRect(0, 0, lienzo.width, lienzo.height);
    contexto.drawImage(imagen, 0, 0, lienzo.width, lienzo.height);
    const blob = await new Promise((resolver) => lienzo.toBlob(resolver, 'image/jpeg', 0.82));
    return blob && blob.size < archivo.size ? blob : archivo;
  },

  async subir(archivo, persona, confidencial) {
    if (!archivo) throw new Error('Elige el archivo.');
    const esImagen = /^image\/(jpeg|png|webp)$/.test(archivo.type);
    if (!esImagen && archivo.type !== 'application/pdf') throw new Error('Solo se aceptan fotos (JPG, PNG o WEBP) o documentos PDF.');
    let contenido = archivo;
    if (esImagen) {
      try { contenido = await this.comprimir(archivo); } catch (e) { contenido = archivo; }
    }
    if (contenido.size > this.MAXIMO) throw new Error('El archivo pesa más de 5 MB. Toma la foto con menos resolución o reduce el PDF.');
    const nombre = esImagen && contenido !== archivo ? archivo.name.replace(/\.[^.]+$/, '') + '.jpg' : archivo.name;
    return Api.subirArchivo({ nombre, datos: await this.aBase64(contenido), persona: persona || '', confidencial: Boolean(confidencial) });
  },

  campo(id, etiqueta, requerido, ayuda) {
    return `<div class="campo"><label for="${id}">${esc(etiqueta)}${requerido ? ' *' : ''}</label>
      <input id="${id}" type="file" accept="${this.ACEPTA}" ${requerido ? 'required' : ''}>
      <span class="ayuda">${esc(ayuda || 'Foto o PDF, hasta 5 MB. Las fotos se reducen antes de enviarse.')}</span></div>`;
  },

  async abrir(id) {
    try {
      const archivo = await Api.archivo(id);
      const url = URL.createObjectURL(archivo.datos);
      if (archivo.tipo.startsWith('image/')) {
        Modal.abrir(archivo.nombre, `<div class="vista-documento"><img src="${url}" alt="${esc(archivo.nombre)}"></div>
          <div class="acciones"><a class="btn btn-secundario" href="${url}" download="${esc(archivo.nombre)}">Descargar</a>
          <button type="button" class="btn btn-primario" data-cerrar>Cerrar</button></div>`, () => URL.revokeObjectURL(url));
        return;
      }
      const enlace = document.createElement('a');
      enlace.href = url;
      enlace.download = archivo.nombre;
      document.body.appendChild(enlace);
      enlace.click();
      enlace.remove();
      setTimeout(() => URL.revokeObjectURL(url), 60000);
      aviso('Documento descargado: ' + archivo.nombre);
    } catch (e) {
      aviso(e.message, 'error');
    }
  },

  boton(id, texto) {
    return id ? `<button type="button" class="btn btn-enlace btn-chico" data-adjunto="${esc(id)}">${esc(texto || 'Ver documento')}</button>` : '';
  },
};

document.addEventListener('click', (evento) => {
  const boton = evento.target.closest && evento.target.closest('[data-adjunto]');
  if (boton) Adjuntos.abrir(boton.dataset.adjunto);
});

const Mapa = {
  distancia(a, b) {
    const r = 6371000;
    const rad = (g) => (g * Math.PI) / 180;
    const dLat = rad(b.latitud - a.latitud);
    const dLon = rad(b.longitud - a.longitud);
    const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.latitud)) * Math.cos(rad(b.latitud)) * Math.sin(dLon / 2) ** 2;
    return 2 * r * Math.asin(Math.min(1, Math.sqrt(h)));
  },
  texto(metros) {
    return metros < 1000 ? Math.round(metros) + ' m' : (metros / 1000).toFixed(1).replace('.', ',') + ' km';
  },
  enlace(p) {
    return `https://www.openstreetmap.org/?mlat=${p.latitud}&mlon=${p.longitud}#map=17/${p.latitud}/${p.longitud}`;
  },
  boton(punto, titulo, obra) {
    if (!punto) return '<span class="pequeno suave">Sin ubicación</span>';
    return `<button type="button" class="btn btn-enlace btn-chico" data-mapa='${esc(JSON.stringify({ punto, titulo, obra: obra || null }))}'>📍 Ver en el mapa</button>`;
  },
  abrir(punto, titulo, obra) {
    const margen = 0.004;
    const lats = [punto.latitud];
    const lons = [punto.longitud];
    if (obra && obra.latitud !== null && obra.latitud !== undefined) {
      lats.push(obra.latitud);
      lons.push(obra.longitud);
    }
    const caja = [Math.min(...lons) - margen, Math.min(...lats) - margen, Math.max(...lons) + margen, Math.max(...lats) + margen]
      .map((n) => n.toFixed(6)).join(',');
    const distancia = obra && obra.latitud !== null && obra.latitud !== undefined ? Mapa.distancia(punto, obra) : null;
    Modal.abrir(titulo, `<div class="marco-mapa"><iframe title="Mapa de la marca" loading="lazy" referrerpolicy="no-referrer"
        src="https://www.openstreetmap.org/export/embed.html?bbox=${caja}&layer=mapnik&marker=${punto.latitud},${punto.longitud}"></iframe></div>
      <p class="pequeno">${punto.latitud.toFixed(5)}, ${punto.longitud.toFixed(5)}${punto.precision ? ' · precisión del GPS: ' + Math.round(punto.precision) + ' m' : ''}
        ${distancia !== null ? `<br><strong>A ${Mapa.texto(distancia)} de ${esc(obra.nombre)}</strong>${obra.radio ? ` (radio permitido: ${obra.radio} m)` : ''}` : ''}</p>
      <div class="acciones"><a class="btn btn-secundario" href="${Mapa.enlace(punto)}" target="_blank" rel="noopener noreferrer">Abrir en OpenStreetMap</a>
        <button type="button" class="btn btn-primario" data-cerrar>Cerrar</button></div>`);
  },
};

document.addEventListener('click', (evento) => {
  const boton = evento.target.closest && evento.target.closest('[data-mapa]');
  if (!boton) return;
  const datos = JSON.parse(boton.dataset.mapa);
  Mapa.abrir(datos.punto, datos.titulo, datos.obra);
});

function confirmar(titulo, mensaje, textoBoton, peligro) {
  return new Promise((resolver) => {
    let respuesta = false;
    const modal = Modal.abrir(titulo, `<p>${mensaje}</p><div class="acciones">
      <button type="button" class="btn btn-secundario" data-cerrar>Cancelar</button>
      <button type="button" class="btn ${peligro ? 'btn-peligro' : 'btn-primario'}" id="confirmar-accion">${esc(textoBoton)}</button></div>`,
    () => resolver(respuesta));
    modal.querySelector('#confirmar-accion').addEventListener('click', () => {
      respuesta = true;
      Modal.cerrar();
    });
  });
}
