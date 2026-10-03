package dev.createsablecontraptions.physics;

/** Impulse is kg*m/s; Minecraft cart velocity is blocks/tick. */
public final class CartPush {
    private CartPush() {}
    public static double velocity(double impulse,double mass) {
        if (!Double.isFinite(impulse) || !Double.isFinite(mass) || mass <= 0) return 0;
        return impulse / (mass * 20);
    }
}
