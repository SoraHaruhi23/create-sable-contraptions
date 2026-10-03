package dev.createsablecontraptions.linear;

/** One accepted step per actuator tick, shared by sequence accounting and movement. */
public final class LinearStep {
    private boolean checked, moved;
    private float accepted;
    public void reset() { checked = moved = false; accepted = 0; }
    public boolean checked() { return checked; }
    public float accept(float speed, float offset, float range, boolean stalled) {
        if (!checked) {
            accepted = stalled ? 0 : Math.max(-offset, Math.min(range - offset, speed));
            checked = true;
        }
        return accepted;
    }
    public float speed() { return accepted; }
    public float take() {
        if (moved) return 0;
        moved = true;
        return accepted;
    }
}
