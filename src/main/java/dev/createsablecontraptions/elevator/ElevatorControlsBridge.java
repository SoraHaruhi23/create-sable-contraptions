package dev.createsablecontraptions.elevator;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsBlockEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Physical controls hold their saved data; the Create actor owns their moving behaviour. */
public final class ElevatorControlsBridge {
    private static final java.util.Map<Level, java.util.Map<java.util.UUID,
            java.lang.ref.WeakReference<com.simibubi.create.content.contraptions.AbstractContraptionEntity>>> CLIENT_OWNERS
            = new java.util.WeakHashMap<>();
    private ElevatorControlsBridge() {}

    public static void rememberClientOwner(com.simibubi.create.content.contraptions.AbstractContraptionEntity entity) {
        if (!entity.level().isClientSide || !ElevatorLink.managed(entity.getContraption())) return;
        var owners = CLIENT_OWNERS.computeIfAbsent(entity.level(), ignored -> new java.util.HashMap<>());
        owners.entrySet().removeIf(entry -> {
            var owner = entry.getValue().get();
            return owner == null || !owner.isAlive() || !ElevatorLink.managed(owner.getContraption())
                    || !entry.getKey().equals(((ElevatorLink) owner.getContraption()).csc$getSubLevel());
        });
        owners.put(
                ((ElevatorLink) entity.getContraption()).csc$getSubLevel(), new java.lang.ref.WeakReference<>(entity));
    }

    public static boolean isPhysicalControl(Level level, BlockPos pos) {
        if (level == null || !AllBlocks.CONTRAPTION_CONTROLS.has(level.getBlockState(pos))) return false;
        var sub = Sable.HELPER.getContaining(level, pos);
        if (sub == null) return false;
        if (sub instanceof ServerSubLevel server) return ElevatorBridge.managed(server);
        var owners = CLIENT_OWNERS.get(level);
        var owner = owners == null ? null : owners.get(sub.getUniqueId());
        var cached = owner == null ? null : owner.get();
        if (cached != null && cached.isAlive() && ElevatorLink.managed(cached.getContraption())
                && sub.getUniqueId().equals(((ElevatorLink) cached.getContraption()).csc$getSubLevel())) return true;
        if (owners != null) owners.remove(sub.getUniqueId());
        // Sable's server user tag is not synchronized; use Create's saved/spawned UUID on clients.
        for (var ref : ContraptionHandler.loadedContraptions.get(level).values()) {
            var entity = ref.get();
            if (entity != null && ElevatorLink.managed(entity.getContraption())
                    && sub.getUniqueId().equals(((ElevatorLink) entity.getContraption()).csc$getSubLevel()))
                return true;
        }
        return false;
    }

    public static void restoreActorData(Level level, ElevatorContraption elevator, BlockPos plotAnchor) {
        for (var actor : elevator.getActors()) {
            if (!AllBlocks.CONTRAPTION_CONTROLS.has(actor.left.state())) continue;
            var context = actor.right;
            if (!AllBlocks.CONTRAPTION_CONTROLS.has(level.getBlockState(plotAnchor.offset(actor.left.pos())))) continue;
            var be = level.getBlockEntity(plotAnchor.offset(actor.left.pos()));
            if (be == null || context == null || context.blockEntityData == null)
                throw new IllegalStateException("Elevator controls data missing; platform retained");
            // startMoving removes the filter on elevators; stopMoving sets Disabled.
            // Preserve all other physical BE data, components and current plot coordinates.
            var tag = be.saveWithFullMetadata(level.registryAccess());
            tag.remove("Filter");
            if (context.blockEntityData.contains("Filter"))
                tag.put("Filter", context.blockEntityData.get("Filter").copy());
            tag.putBoolean("Disabled", context.blockEntityData.getBoolean("Disabled"));
            be.loadWithComponents(tag, level.registryAccess());
            be.setChanged();
        }
    }

    public static boolean hasActorData(Level level, ElevatorContraption elevator, BlockPos local, BlockPos physical) {
        var original = elevator.getBlocks().get(local);
        if (original == null || !AllBlocks.CONTRAPTION_CONTROLS.has(original.state())) return true;
        if (!AllBlocks.CONTRAPTION_CONTROLS.has(level.getBlockState(physical))) return true;
        var actor = elevator.getActorAt(local);
        return level.getBlockEntity(physical) instanceof ContraptionControlsBlockEntity
                && actor != null && actor.right != null && actor.right.blockEntityData != null;
    }
}
