package dev.createsablecontraptions.physics;

/** A bounded approach/departure ramp; reaching an interface still uses Create's handshake. */
public final class DockingSpeed {
    private double factor = 1;
    private boolean held;
    public double update(double distance, double nominalSpeed, boolean transferring) {
        if (transferring) { held = true; return factor = 0; }
        // Brake only within about three ticks of travel, then leave within three ticks.
        // A small crawl reaches Create's actual handshake plane; preview never starts transfer.
        double desired = Double.isFinite(distance)
                ? Math.max(.15, Math.min(1, (distance - .15) / Math.max(.15, nominalSpeed * 1.5))) : 1;
        if (held) { held = false; factor = 0; }
        factor += Math.max(-.4, Math.min(.4, desired - factor));
        if (Double.isFinite(distance) && nominalSpeed > 1e-9)
            factor = Math.min(factor, Math.max(.15, distance / (nominalSpeed * 1.5 + .001)));
        return factor;
    }
}
