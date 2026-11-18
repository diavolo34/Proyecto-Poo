/**
 * ConstruTrack · correos desde tu Gmail y archivos en tu Google Drive.
 *
 * Primera vez: pégalo en https://script.google.com (Nuevo proyecto), cambia CLAVE por una frase larga solo tuya y
 * publícalo en Implementar → Nueva implementación → Aplicación web:
 *   - Ejecutar como: Yo
 *   - Quién tiene acceso: Cualquier usuario
 * Copia la URL que termina en /exec y ponla en Render junto con la misma CLAVE:
 *   GMAIL_SCRIPT_URL   = la URL /exec
 *   GMAIL_SCRIPT_CLAVE = la CLAVE de abajo
 *
 * Para actualizar un script que ya usabas (conserva la misma URL):
 *   1. Reemplaza todo el código por este y guarda, sin cambiar tu CLAVE.
 *   2. Elige la función «autorizar» y pulsa Ejecutar: Google pedirá permiso para Gmail y Drive. Acéptalo.
 *   3. Implementar → Gestionar implementaciones → lápiz (Editar) → Versión: «Nueva versión» → Implementar.
 *
 * Los reposos, vouchers de pago y cartas de renuncia se guardan en la carpeta «ConstruTrack · Adjuntos» de tu Drive,
 * sin compartir con nadie: solo el servidor de ConstruTrack los lee con la CLAVE y se los muestra a quien tiene permiso.
 * Google permite unos 100 correos al día con una cuenta gratuita.
 */
const CLAVE = 'CAMBIA-ESTA-FRASE-POR-UNA-LARGA-Y-SECRETA';
const CARPETA = 'ConstruTrack · Adjuntos';
const TIPOS = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'];
const MAXIMO_BYTES = 5 * 1024 * 1024;

function doPost(e) {
  try {
    const datos = JSON.parse(e.postData.contents);
    if (!datos.clave || datos.clave !== CLAVE) {
      return responder({ ok: false, error: 'Clave incorrecta' });
    }
    const accion = datos.accion || 'correo';
    if (accion === 'correo') return enviarCorreo(datos);
    if (accion === 'subir') return subirArchivo(datos);
    if (accion === 'leer') return leerArchivo(datos);
    return responder({ ok: false, error: 'Acción desconocida: ' + accion });
  } catch (error) {
    return responder({ ok: false, error: String(error && error.message || error) });
  }
}

function enviarCorreo(datos) {
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(datos.para || '')) {
    return responder({ ok: false, error: 'Correo de destino no válido' });
  }
  MailApp.sendEmail({
    to: datos.para,
    subject: String(datos.asunto || 'Invitación a ConstruTrack').slice(0, 200),
    htmlBody: String(datos.html || ''),
    body: String(datos.texto || ''),
    name: 'ConstruTrack',
  });
  return responder({ ok: true, restantes: MailApp.getRemainingDailyQuota() });
}

function subirArchivo(datos) {
  if (TIPOS.indexOf(datos.tipo) < 0) {
    return responder({ ok: false, error: 'Tipo de archivo no permitido' });
  }
  const bytes = Utilities.base64Decode(String(datos.datos || ''));
  if (!bytes.length || bytes.length > MAXIMO_BYTES) {
    return responder({ ok: false, error: 'El archivo está vacío o supera 5 MB' });
  }
  const nombre = String(datos.nombre || 'adjunto').replace(/[\\/:*?"<>|]/g, '_').slice(0, 120);
  const archivo = carpeta().createFile(Utilities.newBlob(bytes, datos.tipo, nombre));
  return responder({ ok: true, id: archivo.getId() });
}

function leerArchivo(datos) {
  let archivo;
  try {
    archivo = DriveApp.getFileById(String(datos.id || ''));
  } catch (error) {
    return responder({ ok: false, error: 'No encontrado' });
  }
  const padre = archivo.getParents();
  if (!padre.hasNext() || padre.next().getName() !== CARPETA) {
    return responder({ ok: false, error: 'No encontrado' });
  }
  return responder({ ok: true, datos: Utilities.base64Encode(archivo.getBlob().getBytes()) });
}

function carpeta() {
  const existentes = DriveApp.getFoldersByName(CARPETA);
  return existentes.hasNext() ? existentes.next() : DriveApp.createFolder(CARPETA);
}

/** Ejecútala una vez desde el editor para que Google pida los permisos de Gmail y Drive. */
function autorizar() {
  carpeta();
  MailApp.getRemainingDailyQuota();
}

/** Prueba rápida desde el editor: Ejecutar → probar (te llega un correo a ti mismo). */
function probar() {
  MailApp.sendEmail(Session.getActiveUser().getEmail(), 'Prueba de ConstruTrack', 'Si lees esto, el envío funciona.');
}

function responder(objeto) {
  return ContentService.createTextOutput(JSON.stringify(objeto)).setMimeType(ContentService.MimeType.JSON);
}
