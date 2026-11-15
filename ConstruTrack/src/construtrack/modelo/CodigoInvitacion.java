package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Objeto de valor con el código de una invitación, con formato CT-XXXX-XXXX.
 * Usa un alfabeto sin caracteres que se confunden (0/O, 1/I/L) para que sea fácil de dictar y copiar.
 * Es inmutable: dos códigos con el mismo texto son iguales.
 */
public final class CodigoInvitacion {

    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final String FORMATO = "CT-[" + ALFABETO + "]{4}-[" + ALFABETO + "]{4}";

    private final String valor;

    private CodigoInvitacion(String valor) {
        this.valor = valor;
    }

    public static CodigoInvitacion generar(RandomGenerator azar) {
        StringBuilder sb = new StringBuilder("CT-");
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                sb.append('-');
            }
            sb.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
        }
        return new CodigoInvitacion(sb.toString());
    }

    /** Interpreta un código escrito por el usuario (ignora espacios y minúsculas). */
    public static CodigoInvitacion de(String texto) {
        String limpio = texto == null ? "" : texto.replaceAll("\\s", "").toUpperCase();
        if (!limpio.matches(FORMATO)) {
            throw new DatoInvalidoException("El código de invitación tiene el formato CT-XXXX-XXXX. Revísalo e intenta de nuevo.");
        }
        return new CodigoInvitacion(limpio);
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof CodigoInvitacion c && c.valor.equals(valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
