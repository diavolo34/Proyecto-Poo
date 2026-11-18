package construtrack;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public final class RelojAjustable extends Clock {
    private Instant ahora;
    private final ZoneId zona;

    public RelojAjustable(Clock base) {
        this(base.instant(), base.getZone());
    }

    private RelojAjustable(Instant ahora, ZoneId zona) {
        this.ahora = ahora;
        this.zona = zona;
    }

    public void avanzar(Duration tiempo) {
        ahora = ahora.plus(tiempo);
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId otra) {
        return new RelojAjustable(ahora, otra);
    }

    @Override
    public Instant instant() {
        return ahora;
    }
}
