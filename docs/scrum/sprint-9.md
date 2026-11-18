# Sprint 9 · Cuentas verificadas y asistencia comprobable

**Objetivo:** que solo entren personas con un correo real, proteger la cuenta del jefe y poder comprobar que cada marca
se hizo en la obra.
**Historias:** HU-33, HU-34 y HU-35.

## Sprint Backlog
- **Correo verificado** (HU-34): código de 6 dígitos al registrarse. La empresa del jefe se crea al confirmarlo. Pantalla
  «Confirma tu correo» en la web y diálogo en Swing, con «Reenviar código».
- **Verificación en dos pasos** (HU-35): TOTP del RFC 6238 propio (`util.Totp`, `util.Base32`). Activación con QR en
  **Seguridad**, 8 códigos de respaldo de un solo uso y desafío al entrar, en la web y en Swing.
- **Asistencia comprobable** (HU-33):
  - geocerca y código QR rotativo por obra;
  - marcas por confirmar, con su motivo;
  - confirmación o rechazo por el supervisor o el jefe (una jornada rechazada no cuenta);
  - pantalla del QR para la obra;
  - escaneo con la cámara o por el enlace del QR;
  - datos de demostración con una obra controlada.
- **Generador de QR propio** (`js/qr.js`), sin librerías y validado con un decodificador externo.
- **Arreglo:** una vista que terminaba de cargar después de cambiar de pantalla pisaba a la nueva.

## Incremento
- 91 pruebas JUnit, 23 de ellas nuevas:
  - vectores del RFC 6238;
  - correo verificado (intentos, vencimiento, límite de envíos y persistencia);
  - dos pasos (activación, respaldos, repetición de códigos, límites y persistencia);
  - asistencia comprobable (código, geocerca, precisión, confirmación, rechazo y persistencia).

## Review
Recorrido en el navegador:
1. Un jefe se registra, confirma el correo con el código y queda con su empresa.
2. Activa los dos pasos con el QR y vuelve a entrar con un código de respaldo.
3. El supervisor muestra el QR de la obra.
4. El trabajador marca con ese código (verificada) y luego sin código (por confirmar).
5. El supervisor confirma y rechaza.
6. Una marca hecha desde el enlace del QR entra y se registra sola.

## Retrospectiva
- **Bien:** reutilizar un mismo TOTP para dos funciones (dos pasos y código de obra) redujo código y pruebas.
- **Mejorar:** la política de seguridad de contenido también afecta a las herramientas de prueba; hay que esperar sin
  evaluar código.
- **Para el futuro:**
  - aviso por correo cuando la clave cambia;
  - registro de auditoría;
  - kiosco en la obra.
