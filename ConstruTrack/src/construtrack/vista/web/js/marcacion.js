const Marcacion = (() => {
  const CLAVE_PENDIENTE = 'construtrack-marca-pendiente';
  const VIGENCIA_PENDIENTE = 3 * 60 * 1000;

  function ubicacionActual() {
    if (!navigator.geolocation) return Promise.resolve(null);
    return new Promise((resolver) => {
      navigator.geolocation.getCurrentPosition(
        (p) => resolver({ latitud: p.coords.latitude, longitud: p.coords.longitude, precision: p.coords.accuracy }),
        () => resolver(null),
        { enableHighAccuracy: true, timeout: 12000, maximumAge: 30000 },
      );
    });
  }

  function codigoDesdeTexto(texto) {
    const valor = String(texto || '').trim();
    const enEnlace = valor.match(/[?&]codigo=(\d{6})/);
    if (enEnlace) return enEnlace[1];
    const limpio = valor.replace(/\s/g, '');
    return /^\d{6}$/.test(limpio) ? limpio : null;
  }

  const puedeEscanear = () => 'BarcodeDetector' in window && Boolean(navigator.mediaDevices && navigator.mediaDevices.getUserMedia);

  async function escanear(video, alLeer) {
    const flujo = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
    video.srcObject = flujo;
    await video.play();
    const detector = new BarcodeDetector({ formats: ['qr_code'] });
    let activo = true;
    const detener = () => {
      activo = false;
      flujo.getTracks().forEach((pista) => pista.stop());
    };
    const buscar = async () => {
      if (!activo) return;
      if (!video.isConnected) {
        detener();
        return;
      }
      try {
        const leidos = await detector.detect(video);
        const codigo = leidos.map((l) => codigoDesdeTexto(l.rawValue)).find(Boolean);
        if (codigo) {
          detener();
          alLeer(codigo);
          return;
        }
      } catch (e) { }
      setTimeout(buscar, 250);
    };
    buscar();
    return detener;
  }

  function pedirCodigo(tipo) {
    return new Promise((resolver) => {
      let detener = null;
      let resuelto = false;
      const terminar = (valor) => {
        if (resuelto) return;
        resuelto = true;
        if (detener) detener();
        resolver(valor);
        Modal.cerrar();
      };
      const modal = Modal.abrir(`Marcar ${tipo}`, `<form novalidate>
        <p>Tu proyecto pide el <strong>código de asistencia</strong>. Escanea el QR que muestra tu supervisor o escribe sus 6 dígitos.</p>
        ${puedeEscanear() ? `<button type="button" class="btn btn-secundario btn-bloque" id="m-escanear">Escanear el QR con la cámara</button>
          <video id="m-video" class="video-qr oculto" playsinline muted></video>` : ''}
        <div class="campo"><label for="m-codigo">Código del proyecto</label>
          <input id="m-codigo" class="codigo-grande" inputmode="numeric" autocomplete="off" maxlength="6" placeholder="000000"></div>
        <div class="mensaje mensaje-error oculto" role="alert"></div>
        <div class="acciones">
          <button type="button" class="btn btn-secundario" id="m-sin-codigo">Marcar sin código</button>
          <button class="btn btn-primario" type="submit">Marcar ${tipo}</button></div>
        <p class="pequeno suave">Sin el código, tu marca queda por confirmar hasta que tu supervisor la revise.</p></form>`,
      () => terminar(undefined));
      const formulario = modal.querySelector('form');
      formulario.addEventListener('submit', (evento) => {
        evento.preventDefault();
        const codigo = codigoDesdeTexto(modal.querySelector('#m-codigo').value);
        if (!codigo) {
          mostrarErrorFormulario(formulario, 'Escribe los 6 dígitos del código del proyecto.');
          return;
        }
        terminar(codigo);
      });
      modal.querySelector('#m-sin-codigo').addEventListener('click', () => terminar(''));
      const boton = modal.querySelector('#m-escanear');
      if (boton) {
        boton.addEventListener('click', async () => {
          const video = modal.querySelector('#m-video');
          video.classList.remove('oculto');
          boton.disabled = true;
          try {
            detener = await escanear(video, terminar);
          } catch (e) {
            video.classList.add('oculto');
            mostrarErrorFormulario(formulario, 'No se pudo abrir la cámara. Escribe el código que se ve en la pantalla.');
          }
        });
      }
    });
  }

  async function marcar(tipo, control, codigoLeido) {
    let codigo = codigoLeido || null;
    if (control && control.exigeCodigo && !codigo) {
      codigo = await pedirCodigo(tipo);
      if (codigo === undefined) return null;
    }
    if (tipo === 'entrada') aviso('Tomando tu ubicación…', 'aviso');
    const ubicacion = await ubicacionActual();
    const datos = { codigo: codigo || null, ...(ubicacion || {}) };
    return tipo === 'entrada' ? Api.marcarEntrada(datos) : Api.marcarSalida(datos);
  }

  function guardarPendiente(codigo) {
    try { sessionStorage.setItem(CLAVE_PENDIENTE, JSON.stringify({ codigo, hora: Date.now() })); } catch (e) { }
  }

  function tomarPendiente() {
    try {
      const pendiente = JSON.parse(sessionStorage.getItem(CLAVE_PENDIENTE) || 'null');
      sessionStorage.removeItem(CLAVE_PENDIENTE);
      return pendiente && Date.now() - pendiente.hora < VIGENCIA_PENDIENTE ? pendiente.codigo : null;
    } catch (e) {
      return null;
    }
  }

  return { marcar, guardarPendiente, tomarPendiente, codigoDesdeTexto };
})();
