/**
 * ConstruTrack · envío de invitaciones desde tu Gmail.
 *
 * Pégalo en https://script.google.com (Nuevo proyecto), cambia CLAVE por una frase larga solo tuya y
 * publícalo en Implementar → Nueva implementación → Aplicación web:
 *   - Ejecutar como: Yo
 *   - Quién tiene acceso: Cualquier usuario
 * Copia la URL que termina en /exec y ponla en Render junto con la misma CLAVE:
 *   GMAIL_SCRIPT_URL   = la URL /exec
 *   GMAIL_SCRIPT_CLAVE = la CLAVE de abajo
 * Google permite unos 100 correos al día con una cuenta gratuita.
 */
const CLAVE = 'CAMBIA-ESTA-FRASE-POR-UNA-LARGA-Y-SECRETA';

function doPost(e) {
  try {
    const datos = JSON.parse(e.postData.contents);
    if (!datos.clave || datos.clave !== CLAVE) {
      return responder({ ok: false, error: 'Clave incorrecta' });
    }
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
  } catch (error) {
    return responder({ ok: false, error: String(error && error.message || error) });
  }
}

/** Prueba rápida desde el editor: Ejecutar → probar (te llega un correo a ti mismo). */
function probar() {
  MailApp.sendEmail(Session.getActiveUser().getEmail(), 'Prueba de ConstruTrack', 'Si lees esto, el envío funciona.');
}

function responder(objeto) {
  return ContentService.createTextOutput(JSON.stringify(objeto)).setMimeType(ContentService.MimeType.JSON);
}
