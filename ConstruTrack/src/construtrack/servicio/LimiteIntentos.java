package construtrack.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class LimiteIntentos {
    private static final int MAXIMO_CLAVES = 10_000;

    private final int maximo;
    private final Duration ventana;
    private final Clock reloj;
    private final Map<String, Deque<Instant>> registros = new HashMap<>();

    public LimiteIntentos(int maximo, Duration ventana, Clock reloj) {
        this.maximo = maximo;
        this.ventana = ventana;
        this.reloj = reloj;
    }

    public synchronized boolean bloqueado(String clave) {
        return vigentes(clave).size() >= maximo;
    }

    public synchronized void registrar(String clave) {
        if (registros.size() >= MAXIMO_CLAVES) {
            depurar();
        }
        registros.computeIfAbsent(clave, k -> new ArrayDeque<>()).addLast(reloj.instant());
    }

    public synchronized void limpiar(String clave) {
        registros.remove(clave);
    }

    public synchronized long minutosDeEspera(String clave) {
        Deque<Instant> lista = vigentes(clave);
        if (lista.size() < maximo) {
            return 0;
        }
        long segundos = Duration.between(reloj.instant(), lista.peekFirst().plus(ventana)).getSeconds();
        return Math.max(1, (segundos + 59) / 60);
    }

    private Deque<Instant> vigentes(String clave) {
        Deque<Instant> lista = registros.get(clave);
        if (lista == null) {
            return new ArrayDeque<>();
        }
        Instant limite = reloj.instant().minus(ventana);
        while (!lista.isEmpty() && !lista.peekFirst().isAfter(limite)) {
            lista.pollFirst();
        }
        if (lista.isEmpty()) {
            registros.remove(clave);
        }
        return lista;
    }

    private void depurar() {
        for (String clave : registros.keySet().toArray(new String[0])) {
            vigentes(clave);
        }
        if (registros.size() >= MAXIMO_CLAVES) {
            registros.clear();
        }
    }
}
