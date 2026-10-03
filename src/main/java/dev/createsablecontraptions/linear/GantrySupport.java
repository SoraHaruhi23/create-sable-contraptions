package dev.createsablecontraptions.linear;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.createsablecontraptions.elevator.ElevatorBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

public final class GantrySupport {
    // Sable emits neighbour updates before assembleBlocks returns and before our UUID/tag exists.
    private static final ThreadLocal<Boolean> TRANSFERRING = ThreadLocal.withInitial(() -> false);
    private GantrySupport() {}
    public static boolean beginTransfer() { boolean before = TRANSFERRING.get(); TRANSFERRING.set(true); return before; }
    public static void endTransfer(boolean before) { if (before) TRANSFERRING.set(true); else TRANSFERRING.remove(); }
    public static boolean retained(LevelReader world, BlockPos pos) {
        if (!(world instanceof Level level)) return false;
        var sub = Sable.HELPER.getContaining(level, pos);
        if (sub == null) return false;
        // Clients can receive the plot before the proxy UUID. Let the server decide removal.
        if (level.isClientSide) return true;
        return sub instanceof ServerSubLevel server && (TRANSFERRING.get() || ElevatorBridge.managed(server));
    }
}
