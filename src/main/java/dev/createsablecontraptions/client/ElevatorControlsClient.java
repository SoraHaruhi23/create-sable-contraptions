package dev.createsablecontraptions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsBlockEntity;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement.ElevatorFloorSelection;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsRenderer;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.contraptions.sync.ContraptionInteractionPacket;
import dev.ryanhcode.sable.Sable;
import dev.createsablecontraptions.CreateSableContraptions;
import dev.createsablecontraptions.elevator.ElevatorLink;
import dev.createsablecontraptions.elevator.ElevatorControlsBridge;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

/** Bind physical block input/rendering directly to the original elevator actor. */
@EventBusSubscriber(modid = CreateSableContraptions.ID, value = Dist.CLIENT)
public final class ElevatorControlsClient {
    private static final java.util.Map<AbstractContraptionEntity, Boolean> LOGGED = new java.util.WeakHashMap<>();
    private static long nextMissingLinkWarning;
    private static final ContraptionControlsBlockEntity.ControlsSlot SLOT = new ContraptionControlsBlockEntity.ControlsSlot() {
        @Override public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
            var offset = getLocalOffset(level, pos, state);
            return offset != null && localHit.distanceTo(offset) < scale * .85;
        }
    };
    private ElevatorControlsClient() {}

    public record Control(AbstractContraptionEntity entity, ElevatorContraption elevator,
                          MovementContext context, ContraptionControlsBlockEntity display) {}

    public static Control resolve(BlockPos physical) {
        var level = Minecraft.getInstance().level;
        if (level == null || !AllBlocks.CONTRAPTION_CONTROLS.has(level.getBlockState(physical))) return null;
        var sub = Sable.HELPER.getContaining(level, physical);
        if (sub == null) return null;
        var local = physical.subtract(sub.getPlot().getCenterBlock());
        // Entity storage is authoritative. Do not depend on Create's collision-list bookkeeping
        // or the proxy's bounding box, which need not match Sable's interpolated platform.
        for (var candidate : level.entitiesForRendering()) {
            if (!(candidate instanceof AbstractContraptionEntity entity) || !entity.isAlive()
                    || !(entity.getContraption() instanceof ElevatorContraption elevator)
                    || !ElevatorLink.managed(elevator)
                    || !sub.getUniqueId().equals(((ElevatorLink) elevator).csc$getSubLevel())) continue;
            var actor = elevator.getActorAt(local);
            ElevatorControlsBridge.rememberClientOwner(entity);
            if (actor == null || actor.right == null || !AllBlocks.CONTRAPTION_CONTROLS.has(actor.left.state())) return null;
            // Build virtual animation state even if the invisible proxy has never been rendered.
            var display = elevator.getOrCreateClientContraptionLazy().getBlockEntity(local);
            if (!(display instanceof ContraptionControlsBlockEntity controls)) return null;
            if (!(actor.right.temporaryData instanceof ElevatorFloorSelection)) {
                var selection = new ElevatorFloorSelection();
                for (int i = 0; i < elevator.namesList.size(); i++)
                    if (elevator.namesList.get(i).getFirst() == elevator.clientYTarget) selection.currentIndex = i;
                actor.right.temporaryData = selection;
            }
            ContraptionControlsMovement.tickFloorSelection((ElevatorFloorSelection) actor.right.temporaryData, elevator);
            if (LOGGED.putIfAbsent(entity, true) == null)
                org.slf4j.LoggerFactory.getLogger(CreateSableContraptions.ID).info(
                        "Linked physical elevator controls: entity={}, subLevel={}, actors={}, floors={}",
                        entity.getId(), sub.getUniqueId(), elevator.getActors().size(), elevator.namesList.size());
            return new Control(entity, elevator, actor.right, controls);
        }
        return null;
    }

    private static void warnMissingLink(BlockPos pos) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !AllBlocks.CONTRAPTION_CONTROLS.has(mc.level.getBlockState(pos))) return;
        var sub = Sable.HELPER.getContaining(mc.level, pos);
        if (sub == null || System.nanoTime() < nextMissingLinkWarning) return;
        nextMissingLinkWarning = System.nanoTime() + 5_000_000_000L;
        var details = new java.util.ArrayList<String>();
        for (var candidate : mc.level.entitiesForRendering())
            if (candidate instanceof AbstractContraptionEntity entity && entity.getContraption() instanceof ElevatorContraption ec)
                details.add("entity=" + entity.getId() + ",link=" + ((ElevatorLink) ec).csc$getSubLevel()
                        + ",actors=" + ec.getActors().size() + ",floors=" + ec.namesList.size());
        org.slf4j.LoggerFactory.getLogger(CreateSableContraptions.ID).warn(
                "No usable elevator controls actor for physical block {} in subLevel {}; client elevators={}. "
                        + "This may also be an ordinary, non-elevator Sable structure.", pos, sub.getUniqueId(), details);
    }

    private static BlockHitResult pickedControl() {
        var mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || mc.player.isSpectator() || mc.level == null
                || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        // Use the actual Sable pick, including its occlusion and render-pose conversion.
        if (Sable.HELPER.distanceSquaredWithSubLevels(mc.level, mc.player.getEyePosition(), hit.getLocation())
                > Math.pow(mc.player.blockInteractionRange() + .01, 2)) return null;
        return hit;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void scroll(InputEvent.MouseScrollingEvent event) {
        var hit = pickedControl();
        if (hit == null) return;
        var control = resolve(hit.getBlockPos());
        if (control == null) { warnMissingLink(hit.getBlockPos()); return; }
        var mc = Minecraft.getInstance();
        if (!SLOT.testHit(mc.level, hit.getBlockPos(), mc.level.getBlockState(hit.getBlockPos()),
                hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos())))) return;
        double delta = event.getScrollDeltaY();
        var selection = (ElevatorFloorSelection) control.context.temporaryData;
        int before = selection.currentIndex;
        selection.currentIndex += (int) (delta > 0 ? Math.ceil(delta) : Math.floor(delta));
        ContraptionControlsMovement.tickFloorSelection(selection, control.elevator);
        if (before != selection.currentIndex && !control.elevator.namesList.isEmpty())
            AllSoundEvents.SCROLL_VALUE.play(mc.level, mc.player, mc.player.blockPosition(), 1,
                    1 + .5f * selection.currentIndex / control.elevator.namesList.size());
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void use(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        var player = Minecraft.getInstance().player;
        if (player != null && player.isShiftKeyDown()
                && player.getItemInHand(event.getHand()).getItem() instanceof net.minecraft.world.item.BlockItem) return;
        var hit = pickedControl();
        if (hit == null) return;
        var control = resolve(hit.getBlockPos());
        if (control == null) {
            if (!BearingControlsClient.use(hit, event)) warnMissingLink(hit.getBlockPos());
            return;
        }
        if (control.entity.handlePlayerInteraction(Minecraft.getInstance().player, control.context.localPos,
                hit.getDirection(), event.getHand()))
            CatnipServices.NETWORK.sendToServer(new ContraptionInteractionPacket(control.entity,
                    event.getHand(), control.context.localPos, hit.getDirection()));
        // The original moving interaction sends ElevatorTargetFloorPacket; no static block toggle.
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    public static void prepareDisplay(ContraptionControlsBlockEntity physical, float partialTick) {
        var control = resolve(physical.getBlockPos());
        if (control == null) { BearingControlsClient.prepareDisplay(physical, partialTick); return; }
        physical.button.setValue(control.display.button.getValue(partialTick));
        physical.indicator.setValue(control.display.indicator.getValue(partialTick));
    }

    public static void renderFloor(ContraptionControlsBlockEntity physical, PoseStack pose, MultiBufferSource buffers) {
        var control = resolve(physical.getBlockPos());
        if (control == null) return;
        var ctx = control.context;
        var matrices = new ContraptionMatrices();
        // The supplied pose already follows the real Sable block. Remove the actor-local offset
        // because Create's renderer adds that offset itself (also under Sable's renderer mixin).
        ContraptionMatrices.transform(matrices.getViewProjection(), pose);
        ContraptionMatrices.transform(matrices.getModelViewProjection(), pose);
        var local = ctx.localPos;
        matrices.getViewProjection().translate(-local.getX(), -local.getY(), -local.getZ());
        matrices.getModelViewProjection().translate(-local.getX(), -local.getY(), -local.getZ());
        var world = Sable.HELPER.projectOutOfSubLevel(physical.getLevel(), Vec3.atLowerCornerOf(physical.getBlockPos()));
        matrices.getWorld().translation((float) (world.x - local.getX()), (float) (world.y - local.getY()), (float) (world.z - local.getZ()));
        var oldPosition = ctx.position;
        try {
            ctx.position = world.add(.5, .5, .5);
            ContraptionControlsRenderer.renderInContraption(ctx,
                    control.elevator.getOrCreateClientContraptionLazy().getRenderLevel(), matrices, buffers);
        } finally { ctx.position = oldPosition; }
    }
}
