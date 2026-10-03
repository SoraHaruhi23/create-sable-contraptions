package dev.createsablecontraptions;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client presentation is local; gameplay settings are owned and synced by the server. */
public final class CscConfig {
    public static final ModConfigSpec CLIENT, SERVER;
    public static final ModConfigSpec.BooleanValue HUD, REASONS, DIAGNOSTICS, HIGHLIGHT;
    public static final ModConfigSpec.DoubleValue ROTATION_CLEARANCE;
    static {
        var client = new ModConfigSpec.Builder();
        HUD = client.translation("csc.config.hud").comment("Show CSC information in Create's goggles overlay.")
                .define("gogglesHud", true);
        REASONS = client.translation("csc.config.reasons").comment("Show the reason a contraption has stopped.")
                .define("gogglesReasons", true);
        HIGHLIGHT = client.translation("csc.config.highlight").comment("Outline the blocking block while inspecting a stopped structure with goggles.")
                .define("collisionHighlight", true);
        CLIENT = client.build();
        var server = new ModConfigSpec.Builder();
        ROTATION_CLEARANCE = server.translation("csc.config.clearance")
                .comment("Bearing vs static-world clearance, in blocks. Does not resize Sable bodies or change cart collisions.")
                .defineInRange("rotationClearance", 1.0 / 1024, 1e-5, 1.0 / 256);
        DIAGNOSTICS = server.translation("csc.config.diagnostics")
                .comment("Record cart/bearing sweep details for /csc diagnostics. Does not disable collision checks.")
                .define("collisionDiagnostics", true);
        SERVER = server.build();
    }
    private CscConfig() {}
    public static double rotationClearance() { return SERVER.isLoaded() ? ROTATION_CLEARANCE.get() : 1.0 / 1024; }
    public static boolean diagnostics() { return !SERVER.isLoaded() || DIAGNOSTICS.get(); }
}
